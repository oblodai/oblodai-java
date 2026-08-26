// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Invoice lifecycle: {@code select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled}.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == PaymentStatus.SELECT} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class PaymentStatus implements Vocabulary {

    /** {@code select} */
    public static final PaymentStatus SELECT = new PaymentStatus("select");

    /** {@code created} */
    public static final PaymentStatus CREATED = new PaymentStatus("created");

    /** {@code confirm_check} */
    public static final PaymentStatus CONFIRM_CHECK = new PaymentStatus("confirm_check");

    /** {@code paid} */
    public static final PaymentStatus PAID = new PaymentStatus("paid");

    /** {@code paid_over} */
    public static final PaymentStatus PAID_OVER = new PaymentStatus("paid_over");

    /** {@code wrong_amount} */
    public static final PaymentStatus WRONG_AMOUNT = new PaymentStatus("wrong_amount");

    /** {@code expired} */
    public static final PaymentStatus EXPIRED = new PaymentStatus("expired");

    /** {@code cancelled} */
    public static final PaymentStatus CANCELLED = new PaymentStatus("cancelled");

    private static final Map<String, PaymentStatus> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<PaymentStatus> VALUES = List.of(SELECT, CREATED, CONFIRM_CHECK, PAID,
            PAID_OVER, WRONG_AMOUNT, EXPIRED, CANCELLED);

    static {
        for (PaymentStatus value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private PaymentStatus(String wire) {
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
    public static PaymentStatus of(String wire) {
        if (wire == null) return null;
        PaymentStatus known = KNOWN.get(wire);
        return known != null ? known : new PaymentStatus(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static PaymentStatus from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PaymentStatus value && value.wire.equals(wire);
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
