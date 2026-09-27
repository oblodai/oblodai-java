package com.oblodai.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What never reaches a log line, a {@code toString} or a hook: values of fields whose name says
 * they are secret (a webhook secret, a cheque passcode, a signature, a token, a device code, a claim
 * link or a signed document link), and the bearer parts of URLs ({@link #redactUrl}).
 */
public final class Redaction {

    /** Field names whose values are replaced. */
    public static final Pattern SENSITIVE =
            Pattern.compile(
                    "secret|signature|passcode|token|authorization|password|claim_?url|document_?url"
                            + "|device_?code|api[_-]?key|cookie",
                    Pattern.CASE_INSENSITIVE);

    /** Query parameters of a signed link ({@code exp}/{@code sig}) and of bearer URLs. */
    private static final java.util.Set<String> SENSITIVE_QUERY =
            java.util.Set.of("sig", "exp", "token", "code", "passcode", "signature");

    /** Path parameters that carry a bearer secret ({@code /v1/claim/{token}}). */
    private static final java.util.Set<String> SENSITIVE_PATH_PARAMS =
            java.util.Set.of("token", "code", "passcode");

    /** Path segments whose NEXT segment is a bearer secret, for URLs seen without their route. */
    private static final java.util.Set<String> SECRET_AFTER_SEGMENT = java.util.Set.of("claim", "aml");

    /** What a hidden value reads as. */
    public static final String REDACTED = "[redacted]";

    private Redaction() {}

    /**
     * @param name a field or header name
     * @return whether its value is hidden
     */
    public static boolean isSensitive(String name) {
        return name != null && SENSITIVE.matcher(name).find();
    }

    /**
     * A copy of {@code url} safe to log or show: no userinfo, the bearer path segments of claim and
     * AML links (and every {@code {token}}/{@code {code}}/{@code {passcode}} segment of {@code
     * routePath}, when given) and the signed-link query parameters ({@code sig}, {@code exp}, {@code
     * token}, ...) replaced by {@value #REDACTED}.
     *
     * @param url an absolute URL
     * @param routePath the route's path template, or {@code null}
     * @return the redacted URL
     */
    public static String redactUrl(String url, String routePath) {
        if (url == null) return null;
        java.net.URI uri;
        try {
            uri = java.net.URI.create(url);
        } catch (IllegalArgumentException e) {
            return "[unparseable url]";
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String[] segments = path.split("/", -1);
        boolean[] hide = new boolean[segments.length];
        for (int i = 1; i < segments.length; i++) {
            if (SECRET_AFTER_SEGMENT.contains(segments[i - 1])) hide[i] = true;
        }
        if (routePath != null) {
            String[] template = routePath.split("/", -1);
            int offset = segments.length - template.length;
            for (int j = 0; offset >= 0 && j < template.length; j++) {
                String part = template[j];
                if (part.startsWith("{") && part.endsWith("}")) {
                    String name = part.substring(1, part.length() - 1);
                    if (SENSITIVE_PATH_PARAMS.contains(name) || isSensitive(name)) hide[offset + j] = true;
                }
            }
        }
        StringBuilder out = new StringBuilder();
        if (uri.getScheme() != null) out.append(uri.getScheme()).append("://");
        if (uri.getHost() != null) out.append(uri.getHost());
        if (uri.getPort() != -1) out.append(':').append(uri.getPort());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) out.append('/');
            out.append(hide[i] && !segments[i].isEmpty() ? REDACTED : segments[i]);
        }
        String query = uri.getRawQuery();
        if (query != null) {
            out.append('?');
            String[] pairs = query.split("&", -1);
            for (int i = 0; i < pairs.length; i++) {
                if (i > 0) out.append('&');
                int eq = pairs[i].indexOf('=');
                String key = eq < 0 ? pairs[i] : pairs[i].substring(0, eq);
                boolean secret =
                        SENSITIVE_QUERY.contains(key.toLowerCase(java.util.Locale.ROOT)) || isSensitive(key);
                out.append(secret && eq >= 0 ? key + "=" + REDACTED : pairs[i]);
            }
        }
        return out.toString();
    }

    /**
     * @param value a JSON tree
     * @return a copy with the values of sensitive keys replaced, recursively
     */
    public static Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                out.put(key, isSensitive(key) ? REDACTED : redact(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Redaction::redact).toList();
        }
        return value;
    }
}
