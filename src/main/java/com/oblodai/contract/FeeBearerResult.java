// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Who paid the network fee, as reported on a priced result.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum FeeBearerResult {

    /** {@code recipient} */
    RECIPIENT("recipient"),

    /** {@code merchant} */
    MERCHANT("merchant"),

    /** {@code gateway} */
    GATEWAY("gateway"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    FeeBearerResult(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static FeeBearerResult from(String wire) {
        if (wire == null) return null;
        for (FeeBearerResult value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
