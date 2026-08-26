// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Blockchain networks the gateway settles on.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum Network {

    /** {@code ethereum} */
    ETHEREUM("ethereum"),

    /** {@code bsc} */
    BSC("bsc"),

    /** {@code polygon} */
    POLYGON("polygon"),

    /** {@code avalanche} */
    AVALANCHE("avalanche"),

    /** {@code base} */
    BASE("base"),

    /** {@code arbitrum} */
    ARBITRUM("arbitrum"),

    /** {@code tron} */
    TRON("tron"),

    /** {@code solana} */
    SOLANA("solana"),

    /** {@code ton} */
    TON("ton"),

    /** {@code bitcoin} */
    BITCOIN("bitcoin"),

    /** {@code litecoin} */
    LITECOIN("litecoin"),

    /** {@code dogecoin} */
    DOGECOIN("dogecoin"),

    /** {@code bitcoincash} */
    BITCOINCASH("bitcoincash"),

    /** {@code dash} */
    DASH("dash"),

    /** {@code xrp} */
    XRP("xrp"),

    /** {@code stellar} */
    STELLAR("stellar"),

    /** {@code monero} */
    MONERO("monero"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    Network(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static Network from(String wire) {
        if (wire == null) return null;
        for (Network value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
