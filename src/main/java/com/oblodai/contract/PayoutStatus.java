// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payout lifecycle: {@code pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled}.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == PayoutStatus.PENDING} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class PayoutStatus implements Vocabulary {

    /** {@code pending} */
    public static final PayoutStatus PENDING = new PayoutStatus("pending");

    /** {@code approved} */
    public static final PayoutStatus APPROVED = new PayoutStatus("approved");

    /** {@code awaiting_cosign} */
    public static final PayoutStatus AWAITING_COSIGN = new PayoutStatus("awaiting_cosign");

    /** {@code broadcasting} */
    public static final PayoutStatus BROADCASTING = new PayoutStatus("broadcasting");

    /** {@code sent} */
    public static final PayoutStatus SENT = new PayoutStatus("sent");

    /** {@code confirmed} */
    public static final PayoutStatus CONFIRMED = new PayoutStatus("confirmed");

    /** {@code failed} */
    public static final PayoutStatus FAILED = new PayoutStatus("failed");

    /** {@code cancelled} */
    public static final PayoutStatus CANCELLED = new PayoutStatus("cancelled");

    private static final Map<String, PayoutStatus> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<PayoutStatus> VALUES = List.of(PENDING, APPROVED, AWAITING_COSIGN, BROADCASTING,
            SENT, CONFIRMED, FAILED, CANCELLED);

    static {
        for (PayoutStatus value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private PayoutStatus(String wire) {
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
    public static PayoutStatus of(String wire) {
        if (wire == null) return null;
        PayoutStatus known = KNOWN.get(wire);
        return known != null ? known : new PayoutStatus(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static PayoutStatus from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PayoutStatus value && value.wire.equals(wire);
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
