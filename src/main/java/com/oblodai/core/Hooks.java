package com.oblodai.core;

import java.util.function.Consumer;

/**
 * Request and response hooks: called once per attempt, for metrics, tracing and structured logs.
 * They run synchronously on the thread driving the call, so keep them cheap; an exception a hook
 * throws fails the call.
 *
 * @param onRequest called before each attempt is sent, or null
 * @param onResponse called when each attempt ends, or null
 */
public record Hooks(Consumer<RequestInfo> onRequest, Consumer<ResponseInfo> onResponse) {

    /** No hooks. */
    public static final Hooks NONE = new Hooks(null, null);

    /**
     * @param hook the request hook
     * @return a copy with it
     */
    public Hooks withOnRequest(Consumer<RequestInfo> hook) {
        return new Hooks(hook, onResponse);
    }

    /**
     * @param hook the response hook
     * @return a copy with it
     */
    public Hooks withOnResponse(Consumer<ResponseInfo> hook) {
        return new Hooks(onRequest, hook);
    }
}
