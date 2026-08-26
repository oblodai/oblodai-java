package com.oblodai.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Minimal structured logging contract. Anything with debug/info/warn/error(message, fields) fits
 * (SLF4J adapters, a console, a test recorder).
 *
 * <p>Values under sensitive-looking keys are redacted by the transport <b>before</b> the fields are
 * handed to the logger, so an injected logger never sees a key, a signature or a cheque passcode —
 * redaction is not something the logger implementation has to remember to do. {@link #console(Level)}
 * redacts again on its own path, which costs nothing and keeps the guarantee if the transport is
 * bypassed.
 */
public interface Logger {

    /** Per-attempt detail: route, attempt number, idempotency key. */
    void debug(String message, Map<String, Object> fields);

    /** Notable but expected events. */
    void info(String message, Map<String, Object> fields);

    /** Something the caller should look at: clock skew, an exhausted retry budget. */
    void warn(String message, Map<String, Object> fields);

    /** A failure the SDK is about to surface. */
    void error(String message, Map<String, Object> fields);

    /** Levels of {@link #console(Level)}. */
    enum Level {
        /** Everything. */
        DEBUG,
        /** Notable events and worse. */
        INFO,
        /** Warnings and errors. */
        WARN,
        /** Errors only. */
        ERROR
    }

    /** A logger that discards everything. The default. */
    static Logger noop() {
        return new Logger() {
            @Override
            public void debug(String message, Map<String, Object> fields) {}

            @Override
            public void info(String message, Map<String, Object> fields) {}

            @Override
            public void warn(String message, Map<String, Object> fields) {}

            @Override
            public void error(String message, Map<String, Object> fields) {}
        };
    }

    /**
     * A logger writing to {@code System.err}, gated by level. {@code OBLODAI_LOG=debug|info|warn|
     * error} selects it when the client is built without an explicit logger.
     *
     * @param level lowest level that is printed
     * @return the logger
     */
    static Logger console(Level level) {
        return new Logger() {
            private void emit(Level at, String message, Map<String, Object> fields) {
                if (at.ordinal() < level.ordinal()) return;
                String line = "[oblodai] " + at + " " + message;
                System.err.println(fields == null || fields.isEmpty() ? line : line + " " + redact(fields));
            }

            @Override
            public void debug(String message, Map<String, Object> fields) {
                emit(Level.DEBUG, message, fields);
            }

            @Override
            public void info(String message, Map<String, Object> fields) {
                emit(Level.INFO, message, fields);
            }

            @Override
            public void warn(String message, Map<String, Object> fields) {
                emit(Level.WARN, message, fields);
            }

            @Override
            public void error(String message, Map<String, Object> fields) {
                emit(Level.ERROR, message, fields);
            }
        };
    }

    /** Pattern of field names whose values are replaced before logging. */
    Pattern SENSITIVE = Pattern.compile("secret|signature|passcode|token|authorization|password", Pattern.CASE_INSENSITIVE);

    /**
     * The transport's redaction hook: a copy of the fields with sensitive values replaced.
     *
     * @param fields the fields about to be logged, or null
     * @return a redacted copy, never null
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> redactFields(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) return Map.of();
        return (Map<String, Object>) redact(fields);
    }

    /** Replaces values of sensitive-looking keys, recursively, without touching the original. */
    static Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                out.put(key, SENSITIVE.matcher(key).find() ? "[redacted]" : redact(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) return list.stream().map(Logger::redact).toList();
        return value;
    }
}
