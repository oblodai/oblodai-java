// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Payout lifecycle: {@code pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled}.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum PayoutStatus {

    /** {@code pending} */
    PENDING("pending"),

    /** {@code approved} */
    APPROVED("approved"),

    /** {@code awaiting_cosign} */
    AWAITING_COSIGN("awaiting_cosign"),

    /** {@code broadcasting} */
    BROADCASTING("broadcasting"),

    /** {@code sent} */
    SENT("sent"),

    /** {@code confirmed} */
    CONFIRMED("confirmed"),

    /** {@code failed} */
    FAILED("failed"),

    /** {@code cancelled} */
    CANCELLED("cancelled"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    PayoutStatus(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static PayoutStatus from(String wire) {
        if (wire == null) return null;
        for (PayoutStatus value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
