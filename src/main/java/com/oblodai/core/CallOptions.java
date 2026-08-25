package com.oblodai.core;

import com.oblodai.RequestOptions;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything one transport call needs beyond the route: what to send, where to substitute it, and
 * the caller's per-call overrides. Resources build this from a {@link RequestOptions} plus their own
 * body, query and path parameters.
 */
public final class CallOptions {

    private Object body;
    private Map<String, Object> query;
    private Map<String, String> pathParams;
    private String idempotencyKey;
    private boolean preferPayoutKey;
    private Long timeoutMs;
    private Long deadlineMs;

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
            out.preferPayoutKey = options.isPreferPayoutKey();
            out.timeoutMs = options.timeoutMs();
            out.deadlineMs = options.deadlineMs();
        }
        return out;
    }

    /**
     * @param value the request body object; null means an empty JSON object on a POST
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
        if (value == null) return this;
        if (query == null) query = new LinkedHashMap<>();
        query.put(name, value);
        return this;
    }

    /**
     * @param values query parameters to add, in order
     * @return this
     */
    public CallOptions query(Map<String, Object> values) {
        if (values != null) values.forEach(this::query);
        return this;
    }

    /**
     * @param name path placeholder name
     * @param value value to substitute
     * @return this
     */
    public CallOptions pathParam(String name, String value) {
        if (pathParams == null) pathParams = new LinkedHashMap<>();
        pathParams.put(name, value);
        return this;
    }

    /** Drops the caller's idempotency key: list pages must never reuse one. */
    public CallOptions withoutIdempotencyKey() {
        this.idempotencyKey = null;
        return this;
    }

    /** The request body. */
    public Object body() {
        return body;
    }

    /** Query parameters, or null. */
    public Map<String, Object> query() {
        return query;
    }

    /** Path parameters, or null. */
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /** The caller's idempotency key, or null. */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /** Whether the payout key pair is preferred on an {@code any}-gated route. */
    public boolean preferPayoutKey() {
        return preferPayoutKey;
    }

    /** Per-attempt timeout override, or null. */
    public Long timeoutMs() {
        return timeoutMs;
    }

    /** Overall deadline override, or null. */
    public Long deadlineMs() {
        return deadlineMs;
    }
}
