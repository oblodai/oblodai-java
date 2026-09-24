package com.oblodai.core;

import com.oblodai.RequestOptions;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything one transport call needs beyond the route: what to send, where to substitute it, and
 * the caller's per-call overrides. Resources build this from their body, query and path parameters
 * plus a {@link RequestOptions}.
 */
public final class CallOptions {

    private Object body;
    private Map<String, Object> query;
    private Map<String, String> pathParams;
    private String idempotencyKey;
    private Map<String, String> headers = Map.of();
    private Long timeoutMs;
    private Integer maxRetries;
    private String requestId;

    /** Empty options. */
    public CallOptions() {}

    /**
     * Options seeded from the caller's per-call overrides.
     *
     * @param options the caller's options, or null
     * @return call options carrying them
     */
    public static CallOptions from(RequestOptions options) {
        CallOptions out = new CallOptions();
        if (options != null) {
            out.idempotencyKey = options.idempotencyKey();
            out.headers = options.extraHeaders();
            out.timeoutMs = options.timeout() == null ? null : options.timeout().toMillis();
            out.maxRetries = options.maxRetries();
            out.requestId = options.requestId();
        }
        return out;
    }

    /**
     * @param value the request body (a JSON tree); null means an empty JSON object on a POST
     * @return this
     */
    public CallOptions body(Object value) {
        this.body = value;
        return this;
    }

    /**
     * @param name query parameter name
     * @param value value; null values are dropped
     * @return this
     */
    public CallOptions query(String name, Object value) {
        if (value == null) {
            return this;
        }
        if (query == null) {
            query = new LinkedHashMap<>();
        }
        query.put(name, value);
        return this;
    }

    /**
     * @param values query parameters to add, in order
     * @return this
     */
    public CallOptions query(Map<String, ?> values) {
        if (values != null) {
            values.forEach(this::query);
        }
        return this;
    }

    /**
     * @param name path placeholder name
     * @param value value to substitute
     * @return this
     */
    public CallOptions pathParam(String name, String value) {
        if (pathParams == null) {
            pathParams = new LinkedHashMap<>();
        }
        pathParams.put(name, value);
        return this;
    }

    /**
     * @param values path parameters, or null
     * @return this
     */
    public CallOptions pathParams(Map<String, ?> values) {
        if (values != null) {
            values.forEach((k, v) -> pathParam(k, v == null ? null : String.valueOf(v)));
        }
        return this;
    }

    /** @return this, without the caller's idempotency key (list pages must never reuse one) */
    public CallOptions withoutIdempotencyKey() {
        this.idempotencyKey = null;
        return this;
    }

    /** @return the request body */
    public Object body() {
        return body;
    }

    /** @return query parameters, or null */
    public Map<String, Object> query() {
        return query;
    }

    /** @return path parameters, or null */
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /** @return extra headers for this call, never null */
    public Map<String, String> headers() {
        return headers;
    }

    /** @return the caller's idempotency key, or null */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /** @return per-attempt timeout override in milliseconds, or null */
    public Long timeoutMs() {
        return timeoutMs;
    }

    /** @return retry count override, or null */
    public Integer maxRetries() {
        return maxRetries;
    }

    /** @return the call's {@code X-Request-ID}, or null for a fresh one */
    public String requestId() {
        return requestId;
    }
}
