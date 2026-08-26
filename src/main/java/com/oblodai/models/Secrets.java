package com.oblodai.models;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Keeping shown-once values out of logs. A key secret, a webhook signing secret and a cheque's claim
 * token are one {@code log.info(model)} away from a log aggregator, and the two paths that get them
 * there are always the same: the object's own {@code toString()} and whatever serializes it to JSON.
 *
 * <p>Models that carry such a value render it as {@value #REDACTED} on both paths and keep it
 * readable through its accessor, which is the only place a program should be reading it from.
 */
public final class Secrets {

    /** What a protected value renders as. */
    public static final String REDACTED = "[redacted]";

    private Secrets() {}

    /**
     * @param value the secret, or null
     * @return the placeholder when there is a value, "null" when there is not
     */
    public static String describe(String value) {
        return value == null ? "null" : REDACTED;
    }

    /** Writes {@value #REDACTED} instead of the value it was given. */
    public static final class RedactingSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator json, SerializerProvider provider)
                throws IOException {
            json.writeString(REDACTED);
        }
    }
}
