package com.oblodai.core;

import com.oblodai.RequestOptions;
import com.oblodai.errors.ConfigException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Base of every generated resource ({@code com.oblodai.generated.resources}): the few entry points
 * generated methods call, one per kind of route.
 *
 * <ul>
 *   <li>{@link #call} - an envelope route; its {@code result} goes through the model's parser.
 *   <li>{@link #paged} - an {@code {items, paginate}} list; a lazy {@link Pager} whose pages send
 *       {@code limit}/{@code offset} in the body (in the query for GET).
 *   <li>{@link #file} - a {@code bare} route; a {@link FileResult}.
 * </ul>
 *
 * <p>Each has an {@code *Async} form over the same transport. A result the model cannot read is a
 * {@link com.oblodai.errors.ContractException} ({@code sdk.bad_envelope}), never an unchecked exception
 * from the parser.
 */
public abstract class Resource {

    /** The engine every call goes through. */
    protected final Transport transport;

    /**
     * @param transport the engine every call goes through
     */
    protected Resource(Transport transport) {
        this.transport = transport;
    }

    // --- envelope routes ------------------------------------------------------------------------

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @param parse the model's parser
     * @param <T> the model
     * @return the parsed result
     */
    protected <T> T call(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options,
            Function<Object, T> parse) {
        Object result = transport.call(route, Calls.callOptions(pathParams, query, body, options));
        return Calls.parse(route, result, parse);
    }

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @param parse the model's parser
     * @param <T> the model
     * @return a future of the parsed result
     */
    protected <T> CompletableFuture<T> callAsync(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options,
            Function<Object, T> parse) {
        CallOptions opts;
        try {
            opts = Calls.callOptions(pathParams, query, body, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<Object> raw = transport.callAsync(route, opts);
        return Transport.linkCancellation(raw.thenApply(result -> Calls.parse(route, result, parse)), raw);
    }

    // --- lists ----------------------------------------------------------------------------------

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @param item the item model's parser
     * @param <T> the item model
     * @return a lazy pager; nothing is requested until it is consumed
     */
    protected <T> Pager<T> paged(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options,
            Function<Object, T> item) {
        PagedPlan plan = PagedPlan.of(route, pathParams, query, body, options);
        return new Pager<>(
                (limit, offset) ->
                        Calls.page(route, transport.call(route, plan.callOptions(limit, offset)), item),
                plan.limit,
                plan.offset);
    }

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @param item the item model's parser
     * @param <T> the item model
     * @return a lazy pager; nothing is requested until it is consumed
     */
    protected <T> AsyncPager<T> pagedAsync(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options,
            Function<Object, T> item) {
        PagedPlan plan = PagedPlan.of(route, pathParams, query, body, options);
        return new AsyncPager<>(
                (limit, offset) -> {
                    CompletableFuture<Object> raw;
                    try {
                        raw = transport.callAsync(route, plan.callOptions(limit, offset));
                    } catch (RuntimeException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                    return Transport.linkCancellation(
                            raw.thenApply(result -> Calls.page(route, result, item)), raw);
                },
                plan.limit,
                plan.offset);
    }

    // --- files ----------------------------------------------------------------------------------

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @return the file
     */
    protected FileResult file(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options) {
        return Calls.fileResult(transport.callRaw(route, Calls.callOptions(pathParams, query, body, options)));
    }

    /**
     * @param route the route
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body (a JSON tree), or null
     * @param options per-call options, or null
     * @return a future of the file
     */
    protected CompletableFuture<FileResult> fileAsync(
            RouteSpec route,
            Map<String, Object> pathParams,
            Map<String, Object> query,
            Object body,
            RequestOptions options) {
        CallOptions opts;
        try {
            opts = Calls.callOptions(pathParams, query, body, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<RawResponse> raw = transport.callRawAsync(route, opts);
        return Transport.linkCancellation(raw.thenApply(Calls::fileResult), raw);
    }

    // --- Ruling 10 ------------------------------------------------------------------------------

    /**
     * A route the gateway does not deduplicate by header but whose body has its own {@code
     * idempotency_key} field: the call's {@code idempotencyKey} option goes into that field, and no
     * header is sent.
     *
     * @param body the request body, mutable
     * @param field the body field
     * @param options per-call options, or null
     * @return the options without the key
     */
    protected RequestOptions idempotencyKeyToBody(
            Map<String, Object> body, String field, RequestOptions options) {
        if (options == null || options.idempotencyKey() == null) {
            return options;
        }
        body.put(field, options.idempotencyKey());
        return options.withoutIdempotencyKey();
    }

    // --- helpers --------------------------------------------------------------------------------

    /** A generated list call, split into what every page repeats and where paging starts. */
    private static final class PagedPlan {
        final RouteSpec route;
        final Map<String, Object> pathParams;
        final Map<String, Object> query;
        final Map<String, Object> body;
        final RequestOptions options;
        final Long limit;
        final Long offset;

        private PagedPlan(
                RouteSpec route,
                Map<String, Object> pathParams,
                Map<String, Object> query,
                Map<String, Object> body,
                RequestOptions options,
                Long limit,
                Long offset) {
            this.route = route;
            this.pathParams = pathParams;
            this.query = query;
            this.body = body;
            this.options = options;
            this.limit = limit;
            this.offset = offset;
        }

        static PagedPlan of(
                RouteSpec route,
                Map<String, Object> pathParams,
                Map<String, Object> query,
                Object body,
                RequestOptions options) {
            if (options != null && options.idempotencyKey() != null && !route.idempotent()) {
                // One key reused across pages would replay page 1 forever.
                throw new ConfigException(
                        ConfigException.IDEMPOTENCY_UNSUPPORTED,
                        route.label()
                                + " does not deduplicate by Idempotency-Key; remove idempotencyKey"
                                + " from this call",
                        "idempotencyKey");
            }
            Map<String, Object> rest = new LinkedHashMap<>();
            if (body instanceof Map<?, ?> map) {
                map.forEach((k, v) -> rest.put(String.valueOf(k), v));
            } else if (body != null) {
                throw new IllegalArgumentException("a list call's body must be a JSON object");
            }
            Map<String, Object> restQuery = query == null ? null : new LinkedHashMap<>(query);
            Long limit = number(rest.remove("limit"));
            Long offset = number(rest.remove("offset"));
            if (restQuery != null) {
                Long l = number(restQuery.remove("limit"));
                Long o = number(restQuery.remove("offset"));
                limit = l != null ? l : limit;
                offset = o != null ? o : offset;
            }
            return new PagedPlan(
                    route,
                    pathParams,
                    restQuery,
                    rest,
                    options == null ? null : options.withoutIdempotencyKey(),
                    limit,
                    offset);
        }

        CallOptions callOptions(long limit, long offset) {
            if (route.method().equals("GET")) {
                Map<String, Object> q = new LinkedHashMap<>(query == null ? Map.of() : query);
                q.put("limit", limit);
                q.put("offset", offset);
                return Calls.callOptions(pathParams, q, body.isEmpty() ? null : body, options);
            }
            Map<String, Object> b = new LinkedHashMap<>(body);
            b.put("limit", limit);
            b.put("offset", offset);
            return Calls.callOptions(pathParams, query, b, options);
        }

        private static Long number(Object value) {
            if (value instanceof Number n) {
                return n.longValue();
            }
            if (value instanceof String s && !s.isEmpty()) {
                return Long.parseLong(s);
            }
            return null;
        }
    }
}
