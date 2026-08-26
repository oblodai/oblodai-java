// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle of a payout link (cheque).
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum PayoutLinkStatus {

    /** {@code funded} */
    FUNDED("funded"),

    /** {@code claiming} */
    CLAIMING("claiming"),

    /** {@code claimed} */
    CLAIMED("claimed"),

    /** {@code expired} */
    EXPIRED("expired"),

    /** {@code cancelled} */
    CANCELLED("cancelled"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    PayoutLinkStatus(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static PayoutLinkStatus from(String wire) {
        if (wire == null) return null;
        for (PayoutLinkStatus value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
