package com.oblodai;

import com.oblodai.core.Idempotency;
import com.oblodai.core.RequestBuilder;
import com.oblodai.errors.ConfigException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-call options, the last argument of every resource method; {@code null} means the defaults.
 *
 * <p>Immutable: every setter returns a copy, so one instance can be shared and specialised.
 *
 * <pre>{@code
 * oblodai.payouts().create(request, RequestOptions.of()
 *         .idempotencyKey("payout-" + orderId)
 *         .timeout(Duration.ofSeconds(10))
 *         .maxRetries(1)
 *         .extraHeader("X-Trace", traceId)
 *         .requestId(traceId));
 * }</pre>
 *
 * <p>The five options are {@code idempotencyKey}, {@code timeout}, {@code maxRetries},
 * {@code extraHeaders} and {@code requestId}.
 */
public final class RequestOptions {

    private static final RequestOptions NONE = new RequestOptions(null, null, null, Map.of(), null);

    private final String idempotencyKey;
    private final Duration timeout;
    private final Integer maxRetries;
    private final Map<String, String> extraHeaders;
    private final String requestId;

    private RequestOptions(
            String idempotencyKey,
            Duration timeout,
            Integer maxRetries,
            Map<String, String> extraHeaders,
            String requestId) {
        this.idempotencyKey = idempotencyKey;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.extraHeaders = extraHeaders;
        this.requestId = requestId;
    }

    /** @return the defaults: an automatic idempotency key where the route deduplicates, the client's timeout and retries */
    public static RequestOptions of() {
        return NONE;
    }

    /** @return the defaults, as {@link #of()} */
    public static RequestOptions none() {
        return NONE;
    }

    /**
     * Your own idempotency key, making a retry safe across process restarts. Generated
     * automatically on routes the gateway deduplicates when omitted, and refused
     * ({@code sdk.idempotency_unsupported}) on routes it does not: the gateway would ignore it, and
     * the SDK would wrongly believe a re-send is safe.
     *
     * @param key printable ASCII, at most {@value
     *     com.oblodai.generated.SigningProtocol#MAX_IDEMPOTENCY_KEY_LENGTH} characters; {@code null}
     *     clears it
     * @return a copy carrying the key
     * @throws ConfigException {@code sdk.bad_idempotency_key} when it is not a header-safe value
     */
    public RequestOptions idempotencyKey(String key) {
        if (key != null) {
            Idempotency.assertValid(key);
        }
        return new RequestOptions(key, timeout, maxRetries, extraHeaders, requestId);
    }

    /**
     * How long one HTTP attempt may take; the client's {@code timeout} when not set.
     *
     * @param timeout a positive duration; {@code null} clears it
     * @return a copy carrying the timeout
     * @throws ConfigException {@code sdk.bad_config} for a zero or negative duration
     */
    public RequestOptions timeout(Duration timeout) {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG, "timeout must be positive, got " + timeout, "timeout");
        }
        return new RequestOptions(idempotencyKey, timeout, maxRetries, extraHeaders, requestId);
    }

    /**
     * Retries after the first attempt for this call; the client's {@code RetryOptions.maxRetries}
     * when not set. {@code 0} disables retrying.
     *
     * @param maxRetries zero or more; {@code null} clears it
     * @return a copy carrying the count
     * @throws ConfigException {@code sdk.bad_config} for a negative count
     */
    public RequestOptions maxRetries(Integer maxRetries) {
        if (maxRetries != null && maxRetries < 0) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "maxRetries must be zero or more, got " + maxRetries,
                    "maxRetries");
        }
        return new RequestOptions(idempotencyKey, timeout, maxRetries, extraHeaders, requestId);
    }

    /**
     * One extra header on this call only, merged over the client-wide ones. A name the SDK owns
     * (Accept, Content-Type, User-Agent, the signing headers of {@link
     * com.oblodai.generated.SigningProtocol#REQUEST_HEADERS}, {@value
     * com.oblodai.core.Signing#HEADER_ADMIN_TOKEN}) is refused, as is a value HTTP could not carry.
     *
     * @param name header name
     * @param value header value
     * @return a copy carrying the header
     * @throws ConfigException {@code sdk.bad_header} when it cannot be sent
     */
    public RequestOptions extraHeader(String name, String value) {
        RequestBuilder.assertCallerHeader(name, value);
        Map<String, String> merged = new LinkedHashMap<>(extraHeaders);
        merged.put(name, value);
        return new RequestOptions(
                idempotencyKey, timeout, maxRetries, Map.copyOf(merged), requestId);
    }

    /**
     * Extra headers on this call only, merged over the client-wide ones; see {@link
     * #extraHeader(String, String)}.
     *
     * @param headers header names and values
     * @return a copy carrying them
     * @throws ConfigException {@code sdk.bad_header} when one cannot be sent
     */
    public RequestOptions extraHeaders(Map<String, String> headers) {
        RequestOptions out = this;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            out = out.extraHeader(e.getKey(), e.getValue());
        }
        return out;
    }

    /**
     * The {@code X-Request-ID} of this call - one value for every attempt, so your logs and the
     * gateway's can be joined. A fresh UUID when not set.
     *
     * @param requestId a header-safe value; {@code null} clears it
     * @return a copy carrying the id
     * @throws ConfigException {@code sdk.bad_header} when it cannot be sent as a header
     */
    public RequestOptions requestId(String requestId) {
        if (requestId != null) {
            RequestBuilder.assertHeader("X-Request-ID", requestId);
        }
        return new RequestOptions(idempotencyKey, timeout, maxRetries, extraHeaders, requestId);
    }

    /** @return the caller's idempotency key, or {@code null} */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /** @return the per-attempt timeout, or {@code null} for the client's */
    public Duration timeout() {
        return timeout;
    }

    /** @return the retry count, or {@code null} for the client's */
    public Integer maxRetries() {
        return maxRetries;
    }

    /** @return extra headers for this call, never {@code null} */
    public Map<String, String> extraHeaders() {
        return extraHeaders;
    }

    /** @return the call's {@code X-Request-ID}, or {@code null} for a fresh one */
    public String requestId() {
        return requestId;
    }

    /** @return a copy without the idempotency key (what list pages and job polls use) */
    public RequestOptions withoutIdempotencyKey() {
        return idempotencyKey == null
                ? this
                : new RequestOptions(null, timeout, maxRetries, extraHeaders, requestId);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RequestOptions that
                && Objects.equals(idempotencyKey, that.idempotencyKey)
                && Objects.equals(timeout, that.timeout)
                && Objects.equals(maxRetries, that.maxRetries)
                && extraHeaders.equals(that.extraHeaders)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey, timeout, maxRetries, extraHeaders, requestId);
    }

    @Override
    public String toString() {
        return "RequestOptions(idempotencyKey="
                + idempotencyKey
                + ", timeout="
                + timeout
                + ", maxRetries="
                + maxRetries
                + ", extraHeaders="
                + extraHeaders.keySet()
                + ", requestId="
                + requestId
                + ")";
    }
}
