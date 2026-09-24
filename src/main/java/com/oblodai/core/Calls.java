package com.oblodai.core;

import com.oblodai.RequestOptions;
import com.oblodai.errors.ContractException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What {@link Resource} and the job waiters share: building a call, reading its answer. */
public final class Calls {

    private Calls() {}

    /**
     * @param pathParams path parameters, or null
     * @param query query parameters, or null
     * @param body the request body, or null
     * @param options per-call options, or null
     * @return what the transport runs the call with
     */
    public static CallOptions callOptions(
            Map<String, ?> pathParams, Map<String, ?> query, Object body, RequestOptions options) {
        CallOptions out = CallOptions.from(options);
        out.pathParams(pathParams);
        out.query(query);
        out.body(body);
        return out;
    }

    /**
     * Parses a result, turning a parser's complaint into a contract failure.
     *
     * @param route the route that answered
     * @param result the result tree
     * @param parse the parser
     * @param <T> the model
     * @return the model, or null for a null result
     */
    public static <T> T parse(RouteSpec route, Object result, Function<Object, T> parse) {
        if (result == null) {
            return null;
        }
        try {
            return parse.apply(result);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException e) {
            throw new ContractException(
                    route.label() + ": the result does not match the contract (" + e.getMessage() + ")",
                    200,
                    result);
        }
    }

    /**
     * One page of a list result.
     *
     * @param route the route that answered
     * @param result the result tree: {@code {items, paginate}}
     * @param item the item parser
     * @param <T> the item model
     * @return the page
     */
    public static <T> Page<T> page(RouteSpec route, Object result, Function<Object, T> item) {
        if (!(result instanceof Map<?, ?> map)
                || !(map.get("items") instanceof List<?> items)
                || !(map.get("paginate") instanceof Map<?, ?> paginate)) {
            throw new ContractException(
                    route.label() + ": a list route did not answer with {items, paginate}", 200, result);
        }
        List<T> out = new ArrayList<>(items.size());
        for (Object element : items) {
            out.add(parse(route, element, item));
        }
        return new Page<>(List.copyOf(out), Paginate.fromMap(paginate));
    }

    private static final Pattern FILENAME_UTF8 =
            Pattern.compile("filename\\*=UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME_PLAIN =
            Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    /**
     * @param raw a {@code bare} route's answer
     * @return its bytes, type and file name
     */
    public static FileResult fileResult(RawResponse raw) {
        String type = raw.contentType();
        return new FileResult(
                raw.body(),
                type == null ? "application/octet-stream" : type,
                filename(raw.header("content-disposition").orElse(null)));
    }

    /**
     * @param disposition a {@code Content-Disposition} header, or null
     * @return the file name it carries, or null
     */
    public static String filename(String disposition) {
        if (disposition == null) {
            return null;
        }
        Matcher utf8 = FILENAME_UTF8.matcher(disposition);
        if (utf8.find()) {
            return URLDecoder.decode(utf8.group(1).replace("+", "%2B"), StandardCharsets.UTF_8);
        }
        Matcher plain = FILENAME_PLAIN.matcher(disposition);
        return plain.find() ? plain.group(1) : null;
    }

}
