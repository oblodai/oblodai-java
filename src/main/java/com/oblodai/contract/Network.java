// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blockchain networks the gateway settles on.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == Network.ETHEREUM} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class Network implements Vocabulary {

    /** {@code ethereum} */
    public static final Network ETHEREUM = new Network("ethereum");

    /** {@code bsc} */
    public static final Network BSC = new Network("bsc");

    /** {@code polygon} */
    public static final Network POLYGON = new Network("polygon");

    /** {@code avalanche} */
    public static final Network AVALANCHE = new Network("avalanche");

    /** {@code base} */
    public static final Network BASE = new Network("base");

    /** {@code arbitrum} */
    public static final Network ARBITRUM = new Network("arbitrum");

    /** {@code tron} */
    public static final Network TRON = new Network("tron");

    /** {@code solana} */
    public static final Network SOLANA = new Network("solana");

    /** {@code ton} */
    public static final Network TON = new Network("ton");

    /** {@code bitcoin} */
    public static final Network BITCOIN = new Network("bitcoin");

    /** {@code litecoin} */
    public static final Network LITECOIN = new Network("litecoin");

    /** {@code dogecoin} */
    public static final Network DOGECOIN = new Network("dogecoin");

    /** {@code bitcoincash} */
    public static final Network BITCOINCASH = new Network("bitcoincash");

    /** {@code dash} */
    public static final Network DASH = new Network("dash");

    /** {@code xrp} */
    public static final Network XRP = new Network("xrp");

    /** {@code stellar} */
    public static final Network STELLAR = new Network("stellar");

    /** {@code monero} */
    public static final Network MONERO = new Network("monero");

    private static final Map<String, Network> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<Network> VALUES = List.of(ETHEREUM, BSC, POLYGON, AVALANCHE,
            BASE, ARBITRUM, TRON, SOLANA,
            TON, BITCOIN, LITECOIN, DOGECOIN,
            BITCOINCASH, DASH, XRP, STELLAR,
            MONERO);

    static {
        for (Network value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private Network(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    @Override
    public String wire() {
        return wire;
    }

    /** Whether this is one of the values this contract snapshot declares. */
    @Override
    public boolean isKnown() {
        return KNOWN.get(wire) == this;
    }

    /**
     * Decodes a wire value. A value outside this snapshot's vocabulary is kept as it arrived,
     * readable through {@link #wire()}.
     *
     * @param wire the string the API sent
     * @return the interned constant, or a new instance carrying the raw value; null for null
     */
    @JsonCreator
    public static Network of(String wire) {
        if (wire == null) return null;
        Network known = KNOWN.get(wire);
        return known != null ? known : new Network(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static Network from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Network value && value.wire.equals(wire);
    }

    @Override
    public int hashCode() {
        return wire.hashCode();
    }

    /** The observed wire value — including one this snapshot does not know. */
    @Override
    public String toString() {
        return wire;
    }
}
