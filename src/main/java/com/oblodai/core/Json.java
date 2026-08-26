package com.oblodai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

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
 *   <li>Decimal amounts are strings end to end; nothing here turns one into a float — and nothing
 *       turns a float into one either: a JSON number arriving where the contract says string is
 *       refused rather than stringified, because {@code 0.1 + 0.2} has already happened by then and
 *       {@code "0.30000000000000004"} would look like an amount the gateway sent.
 * </ul>
 */
public final class Json {

    private static final ObjectMapper MAPPER = newMapper();

    private Json() {}

    /** A mapper configured the way the SDK needs it. Callers may reuse or reconfigure a copy. */
    public static ObjectMapper newMapper() {
        ObjectMapper mapper =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        return mapper;
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
