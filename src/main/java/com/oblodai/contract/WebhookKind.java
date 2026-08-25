// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kinds of rehearsal webhook the gateway can deliver.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum WebhookKind {

    /** {@code payment} */
    PAYMENT("payment"),

    /** {@code payout} */
    PAYOUT("payout"),

    /** {@code wallet} */
    WALLET("wallet"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    WebhookKind(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static WebhookKind from(String wire) {
        if (wire == null) return null;
        for (WebhookKind value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
