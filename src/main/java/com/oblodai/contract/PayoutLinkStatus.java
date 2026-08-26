// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lifecycle of a payout link (cheque).
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == PayoutLinkStatus.FUNDED} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class PayoutLinkStatus implements Vocabulary {

    /** {@code funded} */
    public static final PayoutLinkStatus FUNDED = new PayoutLinkStatus("funded");

    /** {@code claiming} */
    public static final PayoutLinkStatus CLAIMING = new PayoutLinkStatus("claiming");

    /** {@code claimed} */
    public static final PayoutLinkStatus CLAIMED = new PayoutLinkStatus("claimed");

    /** {@code expired} */
    public static final PayoutLinkStatus EXPIRED = new PayoutLinkStatus("expired");

    /** {@code cancelled} */
    public static final PayoutLinkStatus CANCELLED = new PayoutLinkStatus("cancelled");

    private static final Map<String, PayoutLinkStatus> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<PayoutLinkStatus> VALUES = List.of(FUNDED, CLAIMING, CLAIMED, EXPIRED,
            CANCELLED);

    static {
        for (PayoutLinkStatus value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private PayoutLinkStatus(String wire) {
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
    public static PayoutLinkStatus of(String wire) {
        if (wire == null) return null;
        PayoutLinkStatus known = KNOWN.get(wire);
        return known != null ? known : new PayoutLinkStatus(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static PayoutLinkStatus from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PayoutLinkStatus value && value.wire.equals(wire);
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
