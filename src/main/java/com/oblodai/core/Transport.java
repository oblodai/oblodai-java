package com.oblodai.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * The HTTP engine every resource goes through. One lifecycle, written once and shared by the
 * blocking and the {@link CompletableFuture} clients: serialize, sign, send with a per-attempt
 * timeout, read the envelope under a size ceiling, classify the failure, retry per policy, correct a
 * skewed clock.
 *
 * <p>A successful call answers with the envelope's {@code result} as a plain JSON tree - maps,
 * lists, strings, {@link Long}/{@link BigInteger} for integers, {@link BigDecimal} for fractions,
 * booleans - which the generated models parse. {@link #callRaw} serves the {@code bare} routes that
 * answer with bytes.
 *
 * <p>Cancelling the future a call returns cancels the HTTP exchange in flight and stops the retry
 * loop; the blocking API surfaces that as {@code transport.aborted}.
 */
public final class Transport {

    private final Config config;
    private final Dispatcher dispatcher;
    private final Consumer<RawResponse> observer;

    /**
     * @param config everything the transport needs; built by the client from its options
     */
    public Transport(Config config) {
        this(config, null);
    }

    private Transport(Config config, Consumer<RawResponse> observer) {
        this.config = config;
        this.dispatcher = new Dispatcher(config);
        this.observer = observer;
    }

    /**
     * Transport configuration.
     *
     * @param baseUrl API origin, optionally with a path prefix
     * @param credentials the merchant's API key pair; every signed route uses it
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
     * @param hooks request and response hooks
     * @param sleeper how to wait between attempts
     */
    public record Config(
            String baseUrl,
            Credentials credentials,
            HttpClient httpClient,
            RetryOptions retry,
            SkewCorrectingClock clock,
            Logger logger,
            long timeoutMs,
            long deadlineMs,
            Map<String, String> headers,
            String adminToken,
            String userAgent,
            ObjectMapper mapper,
            Hooks hooks,
            Sleeper sleeper) {

        @Override
        public String toString() {
            return "Transport.Config[baseUrl="
                    + baseUrl
                    + ", credentials="
                    + credentials
                    + ", retry="
                    + retry
                    + ", timeoutMs="
                    + timeoutMs
                    + ", deadlineMs="
                    + deadlineMs
                    + ", headers="
                    + headers.keySet()
                    + ", adminToken="
                    + (adminToken == null ? null : "***")
                    + "]";
        }
    }

    /** @return the configuration */
    public Config config() {
        return config;
    }

    /**
     * A transport over the same connections and clock with a different configuration (what {@code
     * withOptions} builds).
     *
     * @param changed the new configuration
     * @return the transport
     */
    public Transport derive(Config changed) {
        return new Transport(changed, observer);
    }

    /**
     * A transport that hands every successful raw response to {@code observer} before it is
     * decoded (what {@code withRawResponse} builds).
     *
     * @param observer called with each 2xx answer
     * @return the transport
     */
    public Transport observing(Consumer<RawResponse> observer) {
        return new Transport(config, observer);
    }

    /** @return the JSON mapper the client decodes with */
    public ObjectMapper mapper() {
        return config.mapper();
    }

    /** @return the HTTP client this transport sends with */
    public HttpClient httpClient() {
        return config.httpClient();
    }

    /** @return the signing clock, exposed for tests and for reading the learned skew */
    public SkewCorrectingClock clock() {
        return config.clock();
    }

    // --- blocking API ---------------------------------------------------------------------------

    /**
     * Calls an envelope route and returns its {@code result} as a JSON tree.
     *
     * @param route the route
     * @param options body, query, path parameters and per-call overrides
     * @return the result, or {@code null} when it is JSON null
     */
    public Object call(RouteSpec route, CallOptions options) {
        return await(callAsync(route, options));
    }

    /**
     * Calls a route and returns the raw 2xx response.
     *
     * @param route the route
     * @param options query, path parameters and per-call overrides
     * @return the raw response
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
     * @return a future of the result tree; it fails with an {@link OblodaiException}
     */
    public CompletableFuture<Object> callAsync(RouteSpec route, CallOptions options) {
        CompletableFuture<RawResponse> raw = callRawAsync(route, options);
        CompletableFuture<Object> decoded = raw.thenApply(answer -> result(route, answer));
        // thenApply gives a fresh future: without this the caller's cancel() would stop at it and
        // never reach the socket.
        return linkCancellation(decoded, raw);
    }

    /**
     * Calls a route without blocking and returns the raw 2xx response.
     *
     * @param route the route
     * @param options query, path parameters and per-call overrides
     * @return a future of the raw response
     */
    public CompletableFuture<RawResponse> callRawAsync(RouteSpec route, CallOptions options) {
        Exchange exchange;
        try {
            exchange = dispatcher.newExchange(route, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }

        CompletableFuture<RawResponse> result =
                new CompletableFuture<>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        // Cancel this future first: aborting the exchange completes the chain
                        // synchronously, and a future already completed can no longer be cancelled.
                        boolean cancelled = super.cancel(mayInterruptIfRunning);
                        exchange.cancel(mayInterruptIfRunning);
                        return cancelled;
                    }
                };
        dispatcher
                .attempt(exchange)
                .whenComplete(
                        (raw, failure) -> {
                            if (failure != null) {
                                result.completeExceptionally(unwrap(failure));
                                return;
                            }
                            try {
                                if (observer != null) {
                                    observer.accept(raw);
                                }
                                result.complete(raw);
                            } catch (RuntimeException e) {
                                result.completeExceptionally(e);
                            }
                        });
        return result;
    }

    /** Makes cancelling {@code derived} cancel {@code source} as well. */
    static <T> CompletableFuture<T> linkCancellation(
            CompletableFuture<T> derived, CompletableFuture<?> source) {
        CompletableFuture<T> out =
                new CompletableFuture<>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        boolean cancelled = super.cancel(mayInterruptIfRunning);
                        source.cancel(mayInterruptIfRunning);
                        return cancelled;
                    }
                };
        derived.whenComplete(
                (value, failure) -> {
                    if (failure != null) {
                        out.completeExceptionally(unwrap(failure));
                    } else {
                        out.complete(value);
                    }
                });
        return out;
    }

    // --- decoding -------------------------------------------------------------------------------

    /**
     * The {@code result} of a success envelope, as a JSON tree.
     *
     * @param route the route that answered
     * @param raw its 2xx answer
     * @return the result, or {@code null} for JSON null
     * @throws ContractException when the body is not a success envelope, or is the gateway's
     *     "already processed, too large to replay" marker
     */
    public Object result(RouteSpec route, RawResponse raw) {
        JsonNode result =
                Envelope.decode(
                        config.mapper(),
                        raw.status(),
                        raw.text(),
                        raw.header("retry-after").orElse(null),
                        raw.header("location").orElse(null));
        // The gateway replays a cached response by Idempotency-Key; when the original was too large
        // to cache it answers {ok, idempotent_replay: true, detail} instead of the object.
        if (result != null
                && result.isObject()
                && result.path("idempotent_replay").asBoolean(false)) {
            throw new ContractException(
                    route.label()
                            + ": the request was already processed but its response was too large to"
                            + " replay - fetch the result by order_id/reference ("
                            + result.path("detail").asText("")
                            + ")",
                    raw.status(),
                    null);
        }
        return tree(result);
    }

    /**
     * A Jackson tree as plain Java values: objects become ordered maps, arrays lists, integers
     * {@link Long} (or {@link BigInteger} past its range), fractions {@link BigDecimal}.
     *
     * @param node the node, or null
     * @return the value
     */
    public static Object tree(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> out = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                out.put(e.getKey(), tree(e.getValue()));
            }
            return out;
        }
        if (node.isArray()) {
            List<Object> out = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                out.add(tree(item));
            }
            return out;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.canConvertToLong() ? (Object) node.longValue() : node.bigIntegerValue();
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        return node.asText();
    }

    // --- helpers --------------------------------------------------------------------------------

    /**
     * Waits for a future, unwrapping its cause so callers catch an {@link OblodaiException}.
     *
     * @param future the future
     * @param <T> its value
     * @return the value
     */
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
        if (cause instanceof RuntimeException e) {
            return e;
        }
        if (cause instanceof Error e) {
            throw e;
        }
        return new TransportException(TransportException.NETWORK, String.valueOf(cause), cause);
    }

    static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException
                        || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
