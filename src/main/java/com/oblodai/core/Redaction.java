package com.oblodai.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What never reaches a log line, a {@code toString} or a hook: values of fields whose name says
 * they are secret (a webhook secret, a cheque passcode, a signature, a token).
 */
public final class Redaction {

    /** Field names whose values are replaced. */
    public static final Pattern SENSITIVE =
            Pattern.compile(
                    "secret|signature|passcode|token|authorization|password", Pattern.CASE_INSENSITIVE);

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
