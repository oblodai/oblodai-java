package com.oblodai.webhooks;

import java.util.List;
import java.util.Map;

/**
 * Case-insensitive access to the headers of a webhook delivery, whatever framework carried them.
 *
 * <p>Adapters exist for the shapes a Java web stack hands you: a flat map (Spring's {@code
 * HttpHeaders#toSingleValueMap}, a servlet filter's own map), a multi-value map (Jakarta's {@code
 * MultivaluedMap}, Netty's), and {@link java.net.http.HttpHeaders}. Anything else is a lambda.
 */
@FunctionalInterface
public interface WebhookHeaders {

    /**
     * @param name header name, matched case-insensitively
     * @return the first value, or null when the header is absent
     */
    String first(String name);

    /**
     * @param headers one value per header
     * @return a case-insensitive view of them
     */
    static WebhookHeaders of(Map<String, String> headers) {
        return name -> {
            if (headers == null) return null;
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
            }
            return null;
        };
    }

    /**
     * @param headers possibly several values per header
     * @return a case-insensitive view taking the first value of each
     */
    static WebhookHeaders ofMulti(Map<String, ? extends List<String>> headers) {
        return name -> {
            if (headers == null) return null;
            for (Map.Entry<String, ? extends List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    List<String> values = e.getValue();
                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }
            return null;
        };
    }

    /**
     * @param headers the JDK client's header type
     * @return a case-insensitive view of them
     */
    static WebhookHeaders of(java.net.http.HttpHeaders headers) {
        return name -> headers == null ? null : headers.firstValue(name).orElse(null);
    }
}
