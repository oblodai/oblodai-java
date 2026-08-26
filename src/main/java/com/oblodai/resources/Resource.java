package com.oblodai.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.oblodai.RequestOptions;
import com.oblodai.contract.RouteSpec;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.CallOptions;
import com.oblodai.core.FileResult;
import com.oblodai.core.Page;
import com.oblodai.core.Pager;
import com.oblodai.core.RawResponse;
import com.oblodai.core.Transport;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared plumbing for every namespace: turn a route, a body and the caller's options into a call,
 * and decode what comes back. Both the blocking and the asynchronous clients extend this, so the
 * request-shaping rules (how list parameters become pages, how a caller key is kept off list pages)
 * exist in exactly one place.
 */
public abstract class Resource {

    /** The engine every call goes through. */
    protected final Transport transport;

    /**
     * @param transport the engine to call through
     */
    protected Resource(Transport transport) {
        this.transport = transport;
    }

    /** A plain {@code {items}} answer — a list the gateway caps rather than paginates. */
    private record PlainList<T>(@JsonProperty("items") List<T> items) {

        /** Whether {@code items} was there at all, as opposed to an empty array. */
        boolean present() {
            return items != null;
        }
    }

    /**
     * A list route must answer with the list envelope. Without this check a body that lost its
     * {@code items} — a proxy's error page with a 200, a route that changed shape — decodes into an
     * empty page, and "nothing happened yet" is indistinguishable from "the answer was unreadable".
     */
    private static <T> Page<T> assertListEnvelope(RouteSpec route, Page<T> page) {
        if (page == null || !page.isListEnvelope()) {
            throw new ContractException(
                    route.method()
                            + " "
                            + route.path()
                            + ": expected a {items, paginate} list envelope",
                    200,
                    null);
        }
        return page;
    }

    private static <T> List<T> assertItems(RouteSpec route, PlainList<T> list) {
        if (list == null || !list.present()) {
            throw new ContractException(
                    route.method() + " " + route.path() + ": expected an {items} list envelope", 200, null);
        }
        return list.items();
    }

    // --- blocking -------------------------------------------------------------------------------

    /**
     * @param route the route to call
     * @param options prepared call options
     * @param type result type
     * @param <T> result type
     * @return the decoded result
     */
    protected <T> T call(RouteSpec route, CallOptions options, Class<T> type) {
        return transport.call(route, options, type(type));
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param type result type
     * @param <T> result type
     * @return the decoded result
     */
    protected <T> T call(RouteSpec route, Object body, RequestOptions options, Class<T> type) {
        return call(route, CallOptions.from(options).body(body), type);
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param item element type of the {@code items} array
     * @param <T> element type
     * @return the items of a plain (non-paginated) list
     */
    protected <T> List<T> plainList(
            RouteSpec route, Object body, RequestOptions options, Class<T> item) {
        PlainList<T> list =
                transport.call(
                        route,
                        CallOptions.from(options).body(body),
                        parametric(PlainList.class, item));
        return assertItems(route, list);
    }

    /**
     * A lazy pager over a paginated route. Nothing is requested until it is consumed.
     *
     * @param route the route to call
     * @param params the caller's list parameters; {@code limit}/{@code offset} seed the walk
     * @param options the caller's per-call options
     * @param item element type
     * @param <T> element type
     * @return the pager
     */
    protected <T> Pager<T> pager(
            RouteSpec route, Object params, RequestOptions options, Class<T> item) {
        assertNoIdempotencyKey(route, options);
        Map<String, Object> base = toMap(params);
        Integer limit = intOf(base.remove("limit"));
        Integer offset = intOf(base.remove("offset"));
        JavaType pageType = parametric(Page.class, item);
        return new Pager<>(
                (l, o) ->
                        assertListEnvelope(
                                route,
                                transport.call(route, pageOptions(route, base, options, l, o), pageType)),
                limit,
                offset);
    }

    /**
     * @param route the route to call
     * @param options prepared call options
     * @return the bytes of a {@code bare} route, with its content type and filename
     */
    protected FileResult file(RouteSpec route, CallOptions options) {
        return fileOf(transport.callRaw(route, options));
    }

    // --- asynchronous ---------------------------------------------------------------------------

    /**
     * @param route the route to call
     * @param options prepared call options
     * @param type result type
     * @param <T> result type
     * @return a future of the decoded result
     */
    protected <T> CompletableFuture<T> callAsync(
            RouteSpec route, CallOptions options, Class<T> type) {
        return transport.callAsync(route, options, type(type));
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param type result type
     * @param <T> result type
     * @return a future of the decoded result
     */
    protected <T> CompletableFuture<T> callAsync(
            RouteSpec route, Object body, RequestOptions options, Class<T> type) {
        return callAsync(route, CallOptions.from(options).body(body), type);
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param item element type of the {@code items} array
     * @param <T> element type
     * @return a future of the items of a plain (non-paginated) list
     */
    protected <T> CompletableFuture<List<T>> plainListAsync(
            RouteSpec route, Object body, RequestOptions options, Class<T> item) {
        CompletableFuture<PlainList<T>> future =
                transport.callAsync(
                        route,
                        CallOptions.from(options).body(body),
                        parametric(PlainList.class, item));
        return future.thenApply(list -> assertItems(route, list));
    }

    /**
     * @param route the route to call
     * @param params the caller's list parameters
     * @param options the caller's per-call options
     * @param item element type
     * @param <T> element type
     * @return a non-blocking pager
     */
    protected <T> AsyncPager<T> pagerAsync(
            RouteSpec route, Object params, RequestOptions options, Class<T> item) {
        assertNoIdempotencyKey(route, options);
        Map<String, Object> base = toMap(params);
        Integer limit = intOf(base.remove("limit"));
        Integer offset = intOf(base.remove("offset"));
        JavaType pageType = parametric(Page.class, item);
        return new AsyncPager<>(
                (l, o) ->
                        transport
                                .<Page<T>>callAsync(route, pageOptions(route, base, options, l, o), pageType)
                                .thenApply(page -> assertListEnvelope(route, page)),
                limit,
                offset);
    }

    /**
     * @param route the route to call
     * @param options prepared call options
     * @return a future of the bytes of a {@code bare} route
     */
    protected CompletableFuture<FileResult> fileAsync(RouteSpec route, CallOptions options) {
        return transport.callRawAsync(route, options).thenApply(Resource::fileOf);
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param item element type of the {@code items} array, as a JSON type
     * @param <T> element type
     * @return the items of a plain (non-paginated) list
     */
    protected <T> List<T> plainList(
            RouteSpec route, Object body, RequestOptions options, JavaType item) {
        PlainList<T> list =
                transport.call(
                        route,
                        CallOptions.from(options).body(body),
                        transport.mapper().getTypeFactory().constructParametricType(PlainList.class, item));
        return assertItems(route, list);
    }

    /**
     * @param route the route to call
     * @param body request body, or null
     * @param options the caller's per-call options
     * @param item element type of the {@code items} array, as a JSON type
     * @param <T> element type
     * @return a future of the items of a plain (non-paginated) list
     */
    protected <T> CompletableFuture<List<T>> plainListAsync(
            RouteSpec route, Object body, RequestOptions options, JavaType item) {
        CompletableFuture<PlainList<T>> future =
                transport.callAsync(
                        route,
                        CallOptions.from(options).body(body),
                        transport.mapper().getTypeFactory().constructParametricType(PlainList.class, item));
        return future.thenApply(list -> assertItems(route, list));
    }

    /**
     * @param route the route to call
     * @param options prepared call options
     * @param type the JSON type to decode the result into
     * @param <T> result type
     * @return the decoded result
     */
    protected <T> T callTyped(RouteSpec route, CallOptions options, JavaType type) {
        return transport.call(route, options, type);
    }

    /**
     * @param route the route to call
     * @param options prepared call options
     * @param type the JSON type to decode the result into
     * @param <T> result type
     * @return a future of the decoded result
     */
    protected <T> CompletableFuture<T> callTypedAsync(
            RouteSpec route, CallOptions options, JavaType type) {
        return transport.callAsync(route, options, type);
    }

    // --- shaping --------------------------------------------------------------------------------

    /** Fresh call options carrying the caller's per-call overrides. */
    protected CallOptions options(RequestOptions options) {
        return CallOptions.from(options);
    }

    /** A JSON type for a plain class. */
    protected JavaType type(Class<?> raw) {
        return transport.mapper().getTypeFactory().constructType(raw);
    }

    /** A JSON type for a generic class, such as {@code Page<Payment>}. */
    protected JavaType parametric(Class<?> raw, Class<?>... parameters) {
        return transport.mapper().getTypeFactory().constructParametricType(raw, parameters);
    }

    /** The request object as its wire map, so page parameters can be merged into it. */
    protected Map<String, Object> toMap(Object request) {
        if (request == null) return new LinkedHashMap<>();
        if (request instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> converted = transport.mapper().convertValue(request, Map.class);
            return new LinkedHashMap<>(converted);
        } catch (IllegalArgumentException notSerializable) {
            // The caller handed a request object the mapper cannot write. Nothing was sent, so this
            // is a configuration failure, not an API one.
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "request parameters cannot be serialized: " + notSerializable.getMessage(),
                    null);
        }
    }

    /**
     * A caller key on a list route is refused, not quietly dropped. The gateway does not deduplicate
     * these routes, so the header would be ignored — and a caller who passed one would go on
     * believing a re-send was deduplicated when it was not. Refusing at the point the pager is built
     * says so before any page is fetched.
     */
    private static void assertNoIdempotencyKey(RouteSpec route, RequestOptions options) {
        if (options == null || options.idempotencyKey() == null) return;
        throw new ConfigException(
                ConfigException.IDEMPOTENCY_UNSUPPORTED,
                route.method()
                        + " "
                        + route.path()
                        + " does not deduplicate by Idempotency-Key; remove idempotencyKey from this"
                        + " call",
                "idempotencyKey");
    }

    private CallOptions pageOptions(
            RouteSpec route, Map<String, Object> base, RequestOptions options, int limit, int offset) {
        // One idempotency key per page would be wrong on both sides: the gateway would replay page
        // one for ever.
        CallOptions call = CallOptions.from(options).withoutIdempotencyKey();
        if (route.method().equals("GET")) {
            base.forEach(call::query);
            return call.query("limit", limit).query("offset", offset);
        }
        Map<String, Object> body = new LinkedHashMap<>(base);
        body.put("limit", limit);
        body.put("offset", offset);
        return call.body(body);
    }

    private static Integer intOf(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final Pattern FILENAME_UTF8 =
            Pattern.compile("filename\\*=UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME_PLAIN =
            Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    private static FileResult fileOf(RawResponse raw) {
        String disposition = raw.header("content-disposition").orElse(null);
        String filename = null;
        if (disposition != null) {
            Matcher utf8 = FILENAME_UTF8.matcher(disposition);
            if (utf8.find()) {
                filename = java.net.URLDecoder.decode(utf8.group(1), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                Matcher plain = FILENAME_PLAIN.matcher(disposition);
                if (plain.find()) filename = plain.group(1);
            }
        }
        String contentType = raw.contentType();
        return new FileResult(
                raw.body(), contentType == null ? "application/octet-stream" : contentType, filename);
    }
}
