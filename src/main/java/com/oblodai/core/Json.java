package com.oblodai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The SDK's JSON configuration, in one place.
 *
 * <ul>
 *   <li>Unknown response fields are ignored: a gateway that starts sending a new field must not
 *       break a deployed client.
 *   <li>Null request fields are omitted: an absent option and an explicit {@code null} mean
 *       different things to the gateway, and only the first is what a caller who left a field alone
 *       meant.
 *   <li>Wire names are declared field by field with {@code @JsonProperty}, never inferred by a
 *       naming strategy, so the mapping is exact and testable.
 *   <li>Decimal amounts are strings end to end; nothing here turns one into a float.
 * </ul>
 */
public final class Json {

    private static final ObjectMapper MAPPER = newMapper();

    private Json() {}

    /** A mapper configured the way the SDK needs it. Callers may reuse or reconfigure a copy. */
    public static ObjectMapper newMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** The shared mapper the client uses when none is supplied. */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** {@code List<Payment>}-style types for generic decoding. */
    public static JavaType listOf(ObjectMapper mapper, Class<?> element) {
        return mapper.getTypeFactory().constructCollectionType(java.util.List.class, element);
    }

    /** {@code Page<Payment>}-style types for generic decoding. */
    public static JavaType parametric(ObjectMapper mapper, Class<?> raw, Class<?>... parameters) {
        return mapper.getTypeFactory().constructParametricType(raw, parameters);
    }
}
