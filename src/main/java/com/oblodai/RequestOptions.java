package com.oblodai;

import com.oblodai.core.RequestBuilder;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-call options, accepted as the last argument of every resource method.
 *
 * <p>Immutable: every setter returns a copy, so one instance can be shared and specialised.
 *
 * <pre>{@code
 * oblodai.payouts().create(request, RequestOptions.of().idempotencyKey(orderId).timeout(Duration.ofSeconds(10)));
 * }</pre>
 *
 * <p>The options are {@code idempotencyKey}, {@code timeout}, {@code deadline}, {@code header} and
 * {@code preferPayoutKey}.
 */
public final class RequestOptions {

    private static final RequestOptions NONE =
            new RequestOptions(null, null, null, false, Map.of());

    private final String idempotencyKey;
    private final Long timeoutMs;
    private final Long deadlineMs;
    private final boolean preferPayoutKey;
    private final Map<String, String> headers;

    private RequestOptions(
            String idempotencyKey,
            Long timeoutMs,
            Long deadlineMs,
            boolean preferPayoutKey,
            Map<String, String> headers) {
        this.idempotencyKey = idempotencyKey;
        this.timeoutMs = timeoutMs;
        this.deadlineMs = deadlineMs;
        this.preferPayoutKey = preferPayoutKey;
        this.headers = headers;
    }

    /** Defaults: an automatic idempotency key where the route deduplicates, client-level timeouts. */
    public static RequestOptions of() {
        return NONE;
    }

    /** The defaults, as a singleton — what a method called without options uses. */
    public static RequestOptions none() {
        return NONE;
    }

    /**
     * Your own idempotency key, making a retry safe across process restarts. Generated
     * automatically on create routes when omitted, and refused on routes the gateway does not
     * deduplicate (they would ignore it, and the SDK would wrongly believe a re-send is safe).
     *
     * @param key printable ASCII, at most 255 characters
     * @return a copy carrying the key
     */
    public RequestOptions idempotencyKey(String key) {
        return new RequestOptions(key, timeoutMs, deadlineMs, preferPayoutKey, headers);
    }

    /**
     * Per-attempt timeout.
     *
     * @param timeout how long one HTTP attempt may take
     * @return a copy carrying the timeout
     */
    public RequestOptions timeout(Duration timeout) {
        return new RequestOptions(
                idempotencyKey, timeout.toMillis(), deadlineMs, preferPayoutKey, headers);
    }

    /**
     * Overall budget for the call, retries and pauses included.
     *
     * @param deadline the budget
     * @return a copy carrying the deadline
     */
    public RequestOptions deadline(Duration deadline) {
        return new RequestOptions(
                idempotencyKey, timeoutMs, deadline.toMillis(), preferPayoutKey, headers);
    }

    /**
     * Signs with the payout key on a route that accepts either kind (for instance {@code
     * batches.info} for a payout batch).
     *
     * @param prefer true to prefer the payout key pair
     * @return a copy carrying the preference
     */
    public RequestOptions preferPayoutKey(boolean prefer) {
        return new RequestOptions(idempotencyKey, timeoutMs, deadlineMs, prefer, headers);
    }

    /**
     * One extra header on this call only, on top of the client-wide ones. A name the SDK owns
     * (Accept, Content-Type, User-Agent, the signing headers, Idempotency-Key, X-Admin-Token) is
     * refused, as is a value HTTP could not carry: the signature covers what is sent, so a header
     * that changes on the way out would break it.
     *
     * @param name header name
     * @param value header value
     * @return a copy carrying the header
     * @throws com.oblodai.errors.ConfigException ({@code sdk.bad_header}) when it cannot be sent
     */
    public RequestOptions header(String name, String value) {
        RequestBuilder.assertCallerHeader(name, value);
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.put(name, value);
        return new RequestOptions(
                idempotencyKey, timeoutMs, deadlineMs, preferPayoutKey, Map.copyOf(merged));
    }

    /** The caller's idempotency key, or null. */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /** Per-attempt timeout in milliseconds, or null for the client default. */
    public Long timeoutMs() {
        return timeoutMs;
    }

    /** Overall budget in milliseconds, or null for the client default. */
    public Long deadlineMs() {
        return deadlineMs;
    }

    /** Extra headers for this call, never null. */
    public Map<String, String> headers() {
        return headers;
    }

    /** Whether to prefer the payout key pair on an {@code any}-gated route. */
    public boolean isPreferPayoutKey() {
        return preferPayoutKey;
    }
}
