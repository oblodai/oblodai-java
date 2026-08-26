package com.oblodai.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.contract.RouteAuth;
import com.oblodai.contract.RouteSpec;
import com.oblodai.errors.ConfigException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the outgoing request — URL, headers, body — as a pure function of its inputs, so the
 * signing material (what is signed) and the wire bytes (what is sent) come from one place and cannot
 * disagree. Nothing here touches the network or the clock.
 */
public final class RequestBuilder {

    /**
     * Headers the SDK owns, plus the ones the JDK client refuses to let a caller set. A caller
     * header whose name matches one of these — case-insensitively — is dropped: the SDK's value
     * wins, and the request never goes out with two of anything the signature or the gateway reads.
     * {@code X-Admin-Token} is on the list because it is a credential the transport attaches to
     * onboarding routes only; a client-wide copy of it must not ride along on every call.
     */
    private static final Set<String> RESERVED_HEADERS =
            Set.of(
                    Signing.HEADER_PUBLIC_ID.toLowerCase(Locale.ROOT),
                    Signing.HEADER_SIGNATURE.toLowerCase(Locale.ROOT),
                    Signing.HEADER_TIMESTAMP.toLowerCase(Locale.ROOT),
                    Signing.HEADER_IDEMPOTENCY_KEY.toLowerCase(Locale.ROOT),
                    Signing.HEADER_ADMIN_TOKEN.toLowerCase(Locale.ROOT),
                    "accept",
                    "user-agent",
                    "content-type",
                    "content-length",
                    "host",
                    "connection",
                    "expect",
                    "upgrade",
                    "transfer-encoding");

    /** Header names a caller may not set, exposed for tests and for the client's own validation. */
    public static Set<String> reservedHeaders() {
        return RESERVED_HEADERS;
    }

    private static final Pattern PATH_PARAM = Pattern.compile("\\{([a-zA-Z_]+)}");

    private RequestBuilder() {}

    /**
     * What to send, and what was signed.
     *
     * @param uri absolute URL of the request
     * @param method HTTP method
     * @param headers headers to set, keyed case-insensitively
     * @param body body bytes, empty for GET
     * @param requestUri path plus query — the string the signature covers
     */
    public record BuiltRequest(
            URI uri, String method, Map<String, String> headers, byte[] body, String requestUri) {}

    /**
     * Builds one attempt.
     *
     * @param baseUrl API origin, optionally with a path prefix
     * @param route the route being called
     * @param pathParams values for the {@code {name}} segments of the route path
     * @param query query parameters; null and empty values are dropped
     * @param body already-serialized body bytes
     * @param credentials the key pair to sign with, or null on public/onboard routes
     * @param idempotencyKey the {@code Idempotency-Key} to send, or null
     * @param ts unix seconds to sign with
     * @param userAgent the SDK's user agent
     * @param extraHeaders caller headers; ones colliding with an SDK-owned header are dropped
     * @param adminToken admin token to attach, on the onboarding routes that take one; null otherwise
     * @return the request to send
     */
    public static BuiltRequest build(
            String baseUrl,
            RouteSpec route,
            Map<String, String> pathParams,
            Map<String, Object> query,
            byte[] body,
            Credentials credentials,
            String idempotencyKey,
            long ts,
            String userAgent,
            Map<String, String> extraHeaders,
            String adminToken) {

        String path = joinPath(baseUrl, fillPath(route.path(), pathParams));
        String queryString = queryString(query);
        String requestUri = queryString.isEmpty() ? path : path + "?" + queryString;
        URI uri = URI.create(origin(baseUrl) + requestUri);

        // Case-insensitive so a caller's "accept" cannot sit next to the SDK's "Accept".
        Map<String, String> headers = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (extraHeaders != null) {
            for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                if (e.getValue() == null) continue;
                if (RESERVED_HEADERS.contains(e.getKey().toLowerCase(Locale.ROOT))) continue;
                assertHeader(e.getKey(), e.getValue());
                headers.put(e.getKey(), e.getValue());
            }
        }
        if (adminToken != null) headers.put(Signing.HEADER_ADMIN_TOKEN, adminToken);
        headers.put("Accept", "application/json");
        headers.put("User-Agent", userAgent);
        boolean hasBody = !route.method().equals("GET");
        if (hasBody) headers.put("Content-Type", "application/json");
        if (idempotencyKey != null) headers.put(Signing.HEADER_IDEMPOTENCY_KEY, idempotencyKey);

        if (route.auth() != RouteAuth.PUBLIC && route.auth() != RouteAuth.ONBOARD) {
            if (credentials == null) {
                throw new ConfigException(
                        ConfigException.MISSING_CREDENTIALS,
                        route.method()
                                + " "
                                + route.path()
                                + " needs a "
                                + (route.auth() == RouteAuth.ANY
                                        ? "merchant"
                                        : route.auth().name().toLowerCase(Locale.ROOT))
                                + " API key: pass publicId/secret to Oblodai.builder() or set"
                                + " OBLODAI_PUBLIC_ID and OBLODAI_SECRET",
                        null);
            }
            headers.put(Signing.HEADER_PUBLIC_ID, credentials.publicId());
            headers.put(Signing.HEADER_TIMESTAMP, Long.toString(ts));
            headers.put(
                    Signing.HEADER_SIGNATURE,
                    Signing.signRequest(
                            credentials.secret(),
                            ts,
                            route.method(),
                            requestUri,
                            idempotencyKey,
                            hasBody ? body : new byte[0]));
        }

        return new BuiltRequest(uri, route.method(), headers, hasBody ? body : new byte[0], requestUri);
    }

    /**
     * Refuses a caller header outright: one the HTTP layer could not carry verbatim, and one whose
     * name the SDK owns. Used by both the client builder and the per-call options, so "set a header"
     * means the same thing wherever a caller says it.
     *
     * @param name header name
     * @param value header value
     * @throws ConfigException ({@code sdk.bad_header}) when the header cannot be sent as written or
     *     would override one the SDK sets itself
     */
    public static void assertCallerHeader(String name, String value) {
        assertHeader(name, value);
        if (RESERVED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            throw new ConfigException(
                    ConfigException.BAD_HEADER,
                    "header \""
                            + name
                            + "\" is set by the SDK itself and cannot be overridden"
                            + (name.equalsIgnoreCase(Signing.HEADER_ADMIN_TOKEN)
                                    ? "; use adminToken(...), which sends it on onboarding routes only"
                                    : ""),
                    name);
        }
    }

    /**
     * Refuses a caller header the HTTP layer could not carry verbatim. A CR or LF would split the
     * request; a byte above ASCII has no agreed encoding in a header value and different servers
     * read it differently.
     *
     * @param name header name
     * @param value header value
     * @throws ConfigException ({@code sdk.bad_header}) when either cannot be sent as written
     */
    public static void assertHeader(String name, String value) {
        if (name == null || name.isEmpty()) {
            throw new ConfigException(ConfigException.BAD_HEADER, "header name must not be empty", null);
        }
        if (value == null) {
            throw new ConfigException(
                    ConfigException.BAD_HEADER, "header \"" + name + "\" has a null value", name);
        }
        if (!name.matches("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+")) {
            throw new ConfigException(
                    ConfigException.BAD_HEADER,
                    "header name \"" + name + "\" is not a valid HTTP token",
                    name);
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new ConfigException(
                        ConfigException.BAD_HEADER,
                        "header \"" + name + "\" contains a line break",
                        name);
            }
            if (c > 0x7e || (c < 0x20 && c != '\t')) {
                throw new ConfigException(
                        ConfigException.BAD_HEADER,
                        "header \""
                                + name
                                + "\" contains a character HTTP headers cannot carry (U+"
                                + String.format("%04X", (int) c)
                                + "); send it in the body or percent-encode it",
                        name);
            }
        }
    }

    /** Serializes a request body once: GET sends nothing, a missing POST body becomes {@code {}}. */
    public static byte[] serializeBody(ObjectMapper mapper, Object body, String method) {
        if (method.equals("GET")) return new byte[0];
        if (body == null) return "{}".getBytes(StandardCharsets.UTF_8);
        if (body instanceof byte[] raw) return raw;
        try {
            return mapper.writeValueAsBytes(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG, "request body cannot be serialized: " + e.getMessage(), null);
        }
    }

    /** The scheme and authority of the base URL, without any path. */
    static String origin(String baseUrl) {
        URI base = URI.create(baseUrl);
        StringBuilder sb = new StringBuilder(base.getScheme()).append("://").append(base.getHost());
        if (base.getPort() > 0) sb.append(':').append(base.getPort());
        return sb.toString();
    }

    /** Appends a route path to the base URL, keeping any path prefix the base carries. */
    static String joinPath(String baseUrl, String routePath) {
        String prefix = URI.create(baseUrl).getPath();
        if (prefix == null) prefix = "";
        while (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
        return prefix + routePath;
    }

    /**
     * Substitutes {@code {name}} segments. Every placeholder must be supplied, values are
     * percent-encoded, and a value that could climb out of its segment is refused: an id is data,
     * not a way to reach another route.
     */
    static String fillPath(String template, Map<String, String> params) {
        Matcher m = PATH_PARAM.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = params == null ? null : params.get(name);
            if (value == null) value = "";
            if (value.isEmpty() || value.equals(".") || value.equals("..") || value.contains("/")) {
                throw new ConfigException(
                        ConfigException.BAD_PATH_PARAM,
                        "path parameter \""
                                + name
                                + "\" for "
                                + template
                                + " must be a non-empty single segment (got \""
                                + value
                                + "\")",
                        name);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(encode(value)));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Query string in the caller's order; null values are dropped, empty strings are sent. */
    static String queryString(Map<String, Object> query) {
        if (query == null || query.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : query.entrySet()) {
            if (e.getValue() == null) continue;
            String value = String.valueOf(e.getValue());
            if (sb.length() > 0) sb.append('&');
            sb.append(encode(e.getKey())).append('=').append(encode(value));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        // URLEncoder writes a space as '+', which a signature over the raw request line would not
        // match on every server; %20 is unambiguous in both a path and a query.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
