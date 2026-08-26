package com.oblodai.core;

import com.oblodai.contract.RouteAuth;
import com.oblodai.contract.RouteSpec;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * One logical call, attempt by attempt: build and sign the request, send it under a per-attempt
 * timeout and a body ceiling, classify what came back, correct a skewed clock once, retry per
 * policy, and stop the moment the caller cancels or the deadline is spent.
 *
 * <p>Split out of {@link Transport} so that the engine's public surface and the loop that drives it
 * can be read one at a time; the two are built together and share nothing but the configuration.
 */
final class Dispatcher {

    /** Error codes that mean the gateway rejected the signature because of the MAC or the clock. */
    private static final Set<String> SIGNATURE_FAILURE_CODES =
            Set.of("merchant.bad_signature", "auth.bad_timestamp");

    /**
     * How far a measured server offset must be from the offset the request was signed with before
     * re-signing is worth it. Half the window the gateway tolerates: anything smaller was not the
     * reason the signature was rejected.
     */
    static final long SKEW_CORRECTION_THRESHOLD_SECONDS = Signing.SIGNATURE_SKEW_SECONDS / 2;

    private final Transport.Config config;

    Dispatcher(Transport.Config config) {
        this.config = config;
    }

    /** Everything one call needs to know before its first attempt. */
    Exchange newExchange(RouteSpec route, CallOptions options) {
        byte[] body = RequestBuilder.serializeBody(config.mapper(), options.body(), route.method());
        String idempotencyKey = resolveIdempotencyKey(route, options);
        boolean safeToRepeat = route.safe() || (route.idempotent() && idempotencyKey != null);
        long deadlineAt =
                System.currentTimeMillis()
                        + (options.deadlineMs() != null ? options.deadlineMs() : config.deadlineMs());
        return new Exchange(route, options, body, idempotencyKey, safeToRepeat, deadlineAt);
    }

    // --- the attempt loop -----------------------------------------------------------------------

    CompletableFuture<RawResponse> attempt(Exchange exchange) {
        if (exchange.cancelled()) return CompletableFuture.failedFuture(cancelled(exchange));
        HttpRequest request;
        try {
            request = buildRequest(exchange);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("route", exchange.label());
        fields.put("attempt", exchange.attempt);
        fields.put("idempotencyKey", exchange.idempotencyKey);
        log("request", fields);

        return send(request, exchange)
                .handle(
                        (raw, failure) ->
                                failure != null ? onFailure(exchange, failure) : onResponse(exchange, raw))
                .thenCompose(stage -> stage);
    }

    private CompletionStage<RawResponse> onFailure(Exchange exchange, Throwable failure) {
        Throwable cause = Transport.unwrap(failure);
        if (exchange.cancelled()) return CompletableFuture.failedFuture(cancelled(exchange));
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
        log("response", fields);

        if (raw.status() == 401 && SIGNATURE_FAILURE_CODES.contains(failure.code())) {
            CompletionStage<RawResponse> resigned = correctClockAndRetry(exchange, raw);
            if (resigned != null) return resigned;
        }

        if (Retry.shouldRetry(failure, exchange.attempt, exchange.safeToRepeat, config.retry())) {
            return retryAfterPause(exchange, failure);
        }
        return CompletableFuture.failedFuture(failure);
    }

    /**
     * Clock skew: the gateway rejected the timestamp or the MAC. Learn its time from the {@code Date}
     * header, re-sign once, and keep the offset only if that attempt got past authentication. The
     * comparison is against the offset THIS request was signed with, not the client-wide offset,
     * which another call may have moved in the meantime.
     *
     * @return the retried attempt, or {@code null} when this was not skew
     */
    private CompletionStage<RawResponse> correctClockAndRetry(Exchange exchange, RawResponse raw) {
        if (!exchange.skewTried) {
            Long offset = config.clock().observeServerDate(raw.header("date").orElse(null));
            if (offset == null
                    || Math.abs(offset - exchange.signedOffset) <= SKEW_CORRECTION_THRESHOLD_SECONDS) {
                return null;
            }
            Map<String, Object> skew = new LinkedHashMap<>();
            skew.put("route", exchange.label());
            skew.put("offsetSec", offset);
            warn("clock skew detected; re-signing with server time", skew);
            exchange.skewTried = true;
            exchange.skewBefore = exchange.signedOffset;
            exchange.skewInstalled = offset;
            config.clock().correct(offset);
            return attempt(exchange);
        }
        // The corrected timestamp did not help: it was not skew. Undo our own correction — but only
        // if it is still ours; a concurrent call that has since measured the offset itself wins.
        config.clock().revert(exchange.skewInstalled, exchange.skewBefore);
        return null;
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
                .thenCompose(
                        ignored ->
                                exchange.cancelled()
                                        ? CompletableFuture.<RawResponse>failedFuture(cancelled(exchange))
                                        : attempt(exchange));
    }

    private static TransportException cancelled(Exchange exchange) {
        return new TransportException(
                TransportException.ABORTED, "call cancelled by the caller (" + exchange.label() + ")", null);
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
        // Per-call headers win over the client-wide ones; neither can touch an SDK-owned name.
        extra.putAll(exchange.options.headers());
        String adminToken =
                exchange.route.auth() == RouteAuth.ONBOARD ? config.adminToken() : null;

        exchange.signedOffset = config.clock().offset();
        RequestBuilder.BuiltRequest built =
                RequestBuilder.build(
                        config.baseUrl(),
                        exchange.route,
                        exchange.options.pathParams(),
                        exchange.options.query(),
                        exchange.body,
                        credentialsFor(exchange.route, exchange.options.preferPayoutKey()),
                        exchange.idempotencyKey,
                        config.clock().now(exchange.signedOffset),
                        config.userAgent(),
                        extra,
                        adminToken);

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
        long budget = Math.max(1, exchange.deadlineAt - System.currentTimeMillis());
        CompletableFuture<HttpResponse<byte[]>> sent =
                config
                        .httpClient()
                        .sendAsync(
                                request,
                                Bodies.limited(
                                        exchange.route.bare() ? Bodies.BARE_LIMIT : Bodies.JSON_LIMIT,
                                        exchange.label()));
        exchange.inFlight(sent);
        // The JDK's request timeout covers the exchange; this second bound covers the whole body
        // read as well, so a stalled stream cannot outlive the call's deadline.
        CompletableFuture<HttpResponse<byte[]>> bounded = sent.orTimeout(budget, TimeUnit.MILLISECONDS);
        bounded.whenComplete(
                (response, failure) -> {
                    if (Transport.unwrap(failure) instanceof TimeoutException) sent.cancel(true);
                });
        return bounded
                .handle(
                        (response, failure) -> {
                            if (failure != null) {
                                return CompletableFuture.<RawResponse>failedFuture(
                                        asTransportError(Transport.unwrap(failure), exchange));
                            }
                            if (!response.uri().equals(request.uri())) {
                                // An injected HTTP client followed a redirect. The signature covers
                                // the path that was requested, so whatever answered is not the
                                // gateway acting on this request.
                                return CompletableFuture.<RawResponse>failedFuture(
                                        Envelope.redirect(
                                                response.statusCode(), response.uri().toString(), null, null));
                            }
                            return CompletableFuture.completedFuture(
                                    new RawResponse(
                                            response.statusCode(), response.headers(), response.body()));
                        })
                .thenCompose(stage -> stage);
    }

    private Throwable asTransportError(Throwable cause, Exchange exchange) {
        if (cause instanceof OblodaiException) return cause;
        if (cause instanceof HttpTimeoutException || cause instanceof TimeoutException) {
            return new TransportException(
                    TransportException.TIMEOUT, "request timed out (" + exchange.label() + ")", cause);
        }
        if (cause instanceof CancellationException) {
            return exchange.cancelled()
                    ? cancelled(exchange)
                    : new TransportException(TransportException.ABORTED, "request cancelled", cause);
        }
        if (cause instanceof IOException) {
            return new TransportException(
                    TransportException.NETWORK, "network error: " + cause.getMessage(), cause);
        }
        return new TransportException(TransportException.NETWORK, "network error: " + cause, cause);
    }


    private void log(String message, Map<String, Object> fields) {
        config.logger().debug(message, Logger.redactFields(fields));
    }

    private void warn(String message, Map<String, Object> fields) {
        config.logger().warn(message, Logger.redactFields(fields));
    }
}
