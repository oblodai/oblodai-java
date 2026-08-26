package com.oblodai.core;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.contract.RouteSpec;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The HTTP engine every resource goes through. One lifecycle, written once and shared by the
 * blocking and the {@link CompletableFuture} clients: serialize, sign, send with a per-attempt
 * timeout, read the envelope under a size ceiling, classify the failure, retry per policy, correct a
 * skewed clock.
 *
 * <p>{@link #callRaw} serves the few {@code bare} routes that answer with bytes instead of JSON.
 *
 * <p>Cancelling the future a call returns cancels the HTTP exchange in flight and stops the retry
 * loop; the blocking API surfaces that as {@code transport.aborted}.
 */
public final class Transport {

    private final Config config;
    private final Dispatcher dispatcher;

    /**
     * @param config everything the transport needs; built by the client from its options
     */
    public Transport(Config config) {
        this.config = config;
        this.dispatcher = new Dispatcher(config);
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

    /** The HTTP client this transport sends with. */
    public HttpClient httpClient() {
        return config.httpClient();
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
        CompletableFuture<RawResponse> raw = callRawAsync(route, options);
        CompletableFuture<T> decoded = raw.thenApply(answer -> decode(route, answer, type));
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
                            } else {
                                result.complete(raw);
                            }
                        });
        return result;
    }

    /** Makes cancelling {@code derived} cancel {@code source} as well. */
    private static <T> CompletableFuture<T> linkCancellation(
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
        try {
            return config.mapper().convertValue(result, type);
        } catch (IllegalArgumentException badShape) {
            // A field of the wrong JSON type is a contract failure, not a programming error: it must
            // reach the caller as an SDK exception, and without quoting the body into the message.
            throw new ContractException(
                    route.method()
                            + " "
                            + route.path()
                            + ": the result does not match "
                            + type
                            + " ("
                            + rootMessage(badShape)
                            + ")",
                    raw.status(),
                    result);
        }
    }

    /** The innermost message of a binder failure, without the value it choked on. */
    private static String rootMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null) return cause.getClass().getSimpleName();
        int at = message.indexOf(" (through reference chain");
        String head = at < 0 ? message : message.substring(0, at);
        return head.length() > 200 ? head.substring(0, 200) + "…" : head;
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
