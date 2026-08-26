// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Invoice lifecycle: {@code select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled}.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum PaymentStatus {

    /** {@code select} */
    SELECT("select"),

    /** {@code created} */
    CREATED("created"),

    /** {@code confirm_check} */
    CONFIRM_CHECK("confirm_check"),

    /** {@code paid} */
    PAID("paid"),

    /** {@code paid_over} */
    PAID_OVER("paid_over"),

    /** {@code wrong_amount} */
    WRONG_AMOUNT("wrong_amount"),

    /** {@code expired} */
    EXPIRED("expired"),

    /** {@code cancelled} */
    CANCELLED("cancelled"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    PaymentStatus(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static PaymentStatus from(String wire) {
        if (wire == null) return null;
        for (PaymentStatus value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
