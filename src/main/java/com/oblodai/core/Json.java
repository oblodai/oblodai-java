package com.oblodai.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The SDK's JSON configuration, in one place. Jackson only reads and writes the JSON tree; the
 * generated models do the mapping to Java types, field by field.
 *
 * <ul>
 *   <li>Fractions are read as {@link java.math.BigDecimal}, never {@code double}: an amount the
 *       gateway sends as a JSON number keeps every digit.
 *   <li>Null values in a request body are written: an explicit {@code null} set on a model means
 *       "clear this field", and the model only puts it there when the caller asked for it.
 * </ul>
 */
public final class Json {

    private static final ObjectMapper MAPPER = newMapper();

    private Json() {}

    /** @return a mapper configured the way the SDK needs it */
    public static ObjectMapper newMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
    }

    /** @return the shared mapper the client uses when none is supplied */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
