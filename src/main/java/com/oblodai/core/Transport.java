package com.oblodai.core;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.contract.RouteAuth;
import com.oblodai.contract.RouteSpec;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * The HTTP engine every resource goes through. One lifecycle, written once and shared by the
 * blocking and the {@link CompletableFuture} clients: serialize, sign, send with a per-attempt
 * timeout, read the envelope, classify the failure, retry per policy, correct a skewed clock.
 *
 * <p>{@link #callRaw} serves the few {@code bare} routes that answer with bytes instead of JSON.
 */
public final class Transport {

    /** Error codes that mean the gateway rejected the signature because of the MAC or the clock. */
    private static final Set<String> SIGNATURE_FAILURE_CODES =
            Set.of("merchant.bad_signature", "auth.bad_timestamp");

    private final Config config;

    /**
     * @param config everything the transport needs; built by the client from its options
     */
    public Transport(Config config) {
        this.config = config;
    }

    /**
     * Transport configuration.
     *
     * @param baseUrl API origin, optionally with a path prefix
     * @param credentials payment key pair, used for {@code payment}/{@code any} routes
     * @param payoutCredentials payout key pair; {@code payout} routes fall back to the payment pair
     * @param httpClient the JDK HTTP client to send with
     * @param retry retry policy
     * @param clock the signing clock, which learns the gateway's time on a skew failure
     * @param logger structured logger
     * @param timeoutMs default per-attempt timeout
     * @param deadlineMs default overall budget per call
     * @param headers extra headers on every request
     * @param adminToken admin token of a self-hosted gateway; only onboarding routes send it
     * @param userAgent the SDK's user agent
     * @param mapper JSON mapper
     */
    public record Config(
            String baseUrl,
            Credentials credentials,
            Credentials payoutCredentials,
            HttpClient httpClient,
            RetryOptions retry,
            SkewCorrectingClock clock,
            Logger logger,
            long timeoutMs,
            long deadlineMs,
            Map<String, String> headers,
            String adminToken,
            String userAgent,
            ObjectMapper mapper) {}

    /** The JSON mapper the client decodes with. */
    public ObjectMapper mapper() {
        return config.mapper();
    }

    /** The signing clock, exposed for tests and for reading the learned skew. */
    public SkewCorrectingClock clock() {
        return config.clock();
    }

    // --- blocking API ---------------------------------------------------------------------------

    /**
     * Calls an envelope route and decodes its {@code result}.
     *
     * @param route the route
     * @param options body, query, path parameters and per-call overrides
     * @param type the type to decode the result into
     * @param <T> result type
     * @return the decoded result
     */
    public <T> T call(RouteSpec route, CallOptions options, JavaType type) {
        return await(callAsync(route, options, type));
    }

    /**
     * Calls a {@code bare} route and returns the response bytes.
     *
     * @param route the route
     * @param options query, path parameters and per-call overrides
     * @return the raw 2xx response
     */
    public RawResponse callRaw(RouteSpec route, CallOptions options) {
        return await(callRawAsync(route, options));
    }

    // --- asynchronous API -----------------------------------------------------------------------

    /**
     * Calls an envelope route without blocking.
     *
     * @param route the route
     * @param options body, query, path parameters and per-call overrides
     * @param type the type to decode the result into
     * @param <T> result type
     * @return a future of the decoded result; it fails with an {@link OblodaiException} cause
     */
    public <T> CompletableFuture<T> callAsync(RouteSpec route, CallOptions options, JavaType type) {
        return callRawAsync(route, options).thenApply(raw -> decode(route, raw, type));
    }

    /**
     * Calls a route without blocking and returns the raw 2xx response.
     *
     * @param route the route
     * @param options query, path parameters and per-call overrides
     * @return a future of the raw response
     */
    public CompletableFuture<RawResponse> callRawAsync(RouteSpec route, CallOptions options) {
        try {
            byte[] body = RequestBuilder.serializeBody(config.mapper(), options.body(), route.method());
            String idempotencyKey = resolveIdempotencyKey(route, options);
            boolean safeToRepeat = route.safe() || (route.idempotent() && idempotencyKey != null);
            long deadlineAt =
                    System.currentTimeMillis()
                            + (options.deadlineMs() != null ? options.deadlineMs() : config.deadlineMs());
            Exchange exchange =
                    new Exchange(route, options, body, idempotencyKey, safeToRepeat, deadlineAt);
            return attempt(exchange);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // --- decoding -------------------------------------------------------------------------------

    private <T> T decode(RouteSpec route, RawResponse raw, JavaType type) {
        JsonNode result =
                Envelope.decode(
                        config.mapper(),
                        raw.status(),
                        raw.text(),
                        raw.header("retry-after").orElse(null),
                        raw.header("location").orElse(null));
        // The gateway replays a cached response by Idempotency-Key; when the original was too large
        // to cache it answers {ok, idempotent_replay: true, detail} instead of the object.
        if (result != null && result.isObject() && result.path("idempotent_replay").asBoolean(false)) {
            throw new ContractException(
                    route.method()
                            + " "
                            + route.path()
                            + ": the request was already processed but its response was too large to"
                            + " replay — fetch the result by order_id/reference ("
                            + result.path("detail").asText("")
                            + ")",
                    raw.status(),
                    null);
        }
        if (result == null || result.isNull()) return null;
        return config.mapper().convertValue(result, type);
    }

    // --- the attempt loop -----------------------------------------------------------------------

    /** State of one logical call across its attempts. */
    private static final class Exchange {
        final RouteSpec route;
        final CallOptions options;
        final byte[] body;
        final String idempotencyKey;
        final boolean safeToRepeat;
        final long deadlineAt;
        int attempt;
        boolean skewTried;
        long skewBefore;

        Exchange(
                RouteSpec route,
                CallOptions options,
                byte[] body,
                String idempotencyKey,
                boolean safeToRepeat,
                long deadlineAt) {
            this.route = route;
            this.options = options;
            this.body = body;
            this.idempotencyKey = idempotencyKey;
            this.safeToRepeat = safeToRepeat;
            this.deadlineAt = deadlineAt;
        }

        String label() {
            return route.method() + " " + route.path();
        }
    }

    private CompletableFuture<RawResponse> attempt(Exchange exchange) {
        HttpRequest request = buildRequest(exchange);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("route", exchange.label());
        fields.put("attempt", exchange.attempt);
        fields.put("idempotencyKey", exchange.idempotencyKey);
        config.logger().debug("request", fields);

        return send(request, exchange)
                .handle((raw, failure) -> failure != null ? onFailure(exchange, failure) : onResponse(exchange, raw))
                .thenCompose(stage -> stage);
    }

    private CompletionStage<RawResponse> onFailure(Exchange exchange, Throwable failure) {
        Throwable cause = unwrap(failure);
        if (Retry.shouldRetry(cause, exchange.attempt, exchange.safeToRepeat, config.retry())) {
            return retryAfterPause(exchange, cause);
        }
        return CompletableFuture.failedFuture(cause);
    }

    private CompletionStage<RawResponse> onResponse(Exchange exchange, RawResponse raw) {
        if (raw.status() >= 200 && raw.status() < 300) return CompletableFuture.completedFuture(raw);

        OblodaiException failure = classify(exchange.route, raw);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("route", exchange.label());
        fields.put("status", raw.status());
        fields.put("code", failure.code());
        fields.put("requestId", failure.requestId());
        config.logger().debug("response", fields);

        // Clock skew: the gateway rejected the timestamp or the MAC. Learn its time from the `Date`
        // header, re-sign once, and keep the offset only if that attempt got past authentication.
        if (raw.status() == 401 && SIGNATURE_FAILURE_CODES.contains(failure.code())) {
            if (!exchange.skewTried) {
                Long offset = config.clock().observeServerDate(raw.header("date").orElse(null));
                if (offset != null
                        && Math.abs(offset - config.clock().offset())
                                > Signing.SIGNATURE_SKEW_SECONDS / 2) {
                    Map<String, Object> skew = new LinkedHashMap<>();
                    skew.put("route", exchange.label());
                    skew.put("offsetSec", offset);
                    config.logger().warn("clock skew detected; re-signing with server time", skew);
                    exchange.skewTried = true;
                    exchange.skewBefore = config.clock().offset();
                    config.clock().correct(offset);
                    return attempt(exchange);
                }
            } else {
                // The corrected timestamp did not help: it was not skew. Do not keep the offset.
                config.clock().correct(exchange.skewBefore);
            }
        }

        if (Retry.shouldRetry(failure, exchange.attempt, exchange.safeToRepeat, config.retry())) {
            return retryAfterPause(exchange, failure);
        }
        return CompletableFuture.failedFuture(failure);
    }

    private CompletionStage<RawResponse> retryAfterPause(Exchange exchange, Throwable cause) {
        long delay = Retry.delayMs(cause, exchange.attempt, config.retry());
        if (System.currentTimeMillis() + delay > exchange.deadlineAt) {
            return CompletableFuture.failedFuture(
                    new TransportException(
                            TransportException.DEADLINE,
                            "retry would exceed the call deadline; last error: " + cause.getMessage(),
                            cause));
        }
        exchange.attempt++;
        if (delay <= 0) return attempt(exchange);
        return CompletableFuture.supplyAsync(
                        () -> null, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS))
                .thenCompose(ignored -> attempt(exchange));
    }

    private OblodaiException classify(RouteSpec route, RawResponse raw) {
        try {
            Envelope.decode(
                    config.mapper(),
                    raw.status(),
                    raw.text(),
                    raw.header("retry-after").orElse(null),
                    raw.header("location").orElse(null));
        } catch (OblodaiException e) {
            return e;
        }
        return new ContractException(
                route.method() + " " + route.path() + ": HTTP " + raw.status() + " with a success envelope",
                raw.status(),
                raw.text());
    }

    private HttpRequest buildRequest(Exchange exchange) {
        Map<String, String> extra = new LinkedHashMap<>();
        if (config.headers() != null) extra.putAll(config.headers());
        if (exchange.route.auth() == RouteAuth.ONBOARD && config.adminToken() != null) {
            extra.put(Signing.HEADER_ADMIN_TOKEN, config.adminToken());
        }

        RequestBuilder.BuiltRequest built =
                RequestBuilder.build(
                        config.baseUrl(),
                        exchange.route,
                        exchange.options.pathParams(),
                        exchange.options.query(),
                        exchange.body,
                        credentialsFor(exchange.route, exchange.options.preferPayoutKey()),
                        exchange.idempotencyKey,
                        config.clock().now(),
                        config.userAgent(),
                        extra);

        long timeout =
                Math.min(
                        exchange.options.timeoutMs() != null
                                ? exchange.options.timeoutMs()
                                : config.timeoutMs(),
                        Math.max(1, exchange.deadlineAt - System.currentTimeMillis()));

        HttpRequest.Builder builder =
                HttpRequest.newBuilder(built.uri()).timeout(Duration.ofMillis(timeout));
        built.headers().forEach(builder::header);
        if (built.method().equals("GET")) {
            builder.GET();
        } else {
            builder.method(built.method(), HttpRequest.BodyPublishers.ofByteArray(built.body()));
        }
        return builder.build();
    }

    /** Which key pair signs a route. {@code any} routes take the payment key unless told otherwise. */
    private Credentials credentialsFor(RouteSpec route, boolean preferPayout) {
        if (route.auth() == RouteAuth.PAYOUT || (route.auth() == RouteAuth.ANY && preferPayout)) {
            return config.payoutCredentials() != null ? config.payoutCredentials() : config.credentials();
        }
        return config.credentials();
    }

    private String resolveIdempotencyKey(RouteSpec route, CallOptions options) {
        String key = options.idempotencyKey();
        if (key != null) {
            Idempotency.assertValid(key);
            if (!route.idempotent()) {
                // The gateway ignores the header here, so a key would only make the SDK believe a
                // re-send is deduplicated when it is not — the one belief that turns a lost response
                // into a double spend.
                throw new ConfigException(
                        ConfigException.IDEMPOTENCY_UNSUPPORTED,
                        route.method()
                                + " "
                                + route.path()
                                + " does not deduplicate by Idempotency-Key; remove idempotencyKey from"
                                + " this call",
                        "idempotencyKey");
            }
            return key;
        }
        return route.idempotent() ? Idempotency.newKey() : null;
    }

    private CompletableFuture<RawResponse> send(HttpRequest request, Exchange exchange) {
        return config
                .httpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .handle(
                        (response, failure) -> {
                            if (failure != null) {
                                return CompletableFuture.<RawResponse>failedFuture(
                                        asTransportError(unwrap(failure), exchange));
                            }
                            return CompletableFuture.completedFuture(
                                    new RawResponse(
                                            response.statusCode(), response.headers(), response.body()));
                        })
                .thenCompose(stage -> stage);
    }

    private Throwable asTransportError(Throwable cause, Exchange exchange) {
        if (cause instanceof OblodaiException) return cause;
        if (cause instanceof HttpTimeoutException) {
            return new TransportException(
                    TransportException.TIMEOUT,
                    "request timed out (" + exchange.label() + ")",
                    cause);
        }
        if (cause instanceof CancellationException) {
            return new TransportException(TransportException.ABORTED, "request cancelled by caller", cause);
        }
        if (cause instanceof IOException) {
            return new TransportException(
                    TransportException.NETWORK, "network error: " + cause.getMessage(), cause);
        }
        return new TransportException(
                TransportException.NETWORK, "network error: " + cause, cause);
    }

    // --- helpers --------------------------------------------------------------------------------

    /** Waits for a future, unwrapping its cause so callers catch an {@link OblodaiException}. */
    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw rethrow(unwrap(e));
        } catch (CancellationException e) {
            throw new TransportException(TransportException.ABORTED, "call cancelled", e);
        }
    }

    private static RuntimeException rethrow(Throwable cause) {
        if (cause instanceof OblodaiException e) return e;
        if (cause instanceof RuntimeException e) return e;
        return new TransportException(TransportException.NETWORK, String.valueOf(cause), cause);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
