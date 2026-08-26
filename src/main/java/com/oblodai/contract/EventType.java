// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook event types: {@code invoice.<status>}, {@code payout.<status>}, {@code wallet.paid}.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == EventType.INVOICE_SELECT} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class EventType implements Vocabulary {

    /** {@code invoice.select} */
    public static final EventType INVOICE_SELECT = new EventType("invoice.select");

    /** {@code invoice.created} */
    public static final EventType INVOICE_CREATED = new EventType("invoice.created");

    /** {@code invoice.confirm_check} */
    public static final EventType INVOICE_CONFIRM_CHECK = new EventType("invoice.confirm_check");

    /** {@code invoice.paid} */
    public static final EventType INVOICE_PAID = new EventType("invoice.paid");

    /** {@code invoice.paid_over} */
    public static final EventType INVOICE_PAID_OVER = new EventType("invoice.paid_over");

    /** {@code invoice.wrong_amount} */
    public static final EventType INVOICE_WRONG_AMOUNT = new EventType("invoice.wrong_amount");

    /** {@code invoice.expired} */
    public static final EventType INVOICE_EXPIRED = new EventType("invoice.expired");

    /** {@code invoice.cancelled} */
    public static final EventType INVOICE_CANCELLED = new EventType("invoice.cancelled");

    /** {@code payout.pending} */
    public static final EventType PAYOUT_PENDING = new EventType("payout.pending");

    /** {@code payout.approved} */
    public static final EventType PAYOUT_APPROVED = new EventType("payout.approved");

    /** {@code payout.awaiting_cosign} */
    public static final EventType PAYOUT_AWAITING_COSIGN = new EventType("payout.awaiting_cosign");

    /** {@code payout.broadcasting} */
    public static final EventType PAYOUT_BROADCASTING = new EventType("payout.broadcasting");

    /** {@code payout.sent} */
    public static final EventType PAYOUT_SENT = new EventType("payout.sent");

    /** {@code payout.confirmed} */
    public static final EventType PAYOUT_CONFIRMED = new EventType("payout.confirmed");

    /** {@code payout.failed} */
    public static final EventType PAYOUT_FAILED = new EventType("payout.failed");

    /** {@code payout.cancelled} */
    public static final EventType PAYOUT_CANCELLED = new EventType("payout.cancelled");

    /** {@code wallet.paid} */
    public static final EventType WALLET_PAID = new EventType("wallet.paid");

    private static final Map<String, EventType> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<EventType> VALUES = List.of(INVOICE_SELECT, INVOICE_CREATED, INVOICE_CONFIRM_CHECK, INVOICE_PAID,
            INVOICE_PAID_OVER, INVOICE_WRONG_AMOUNT, INVOICE_EXPIRED, INVOICE_CANCELLED,
            PAYOUT_PENDING, PAYOUT_APPROVED, PAYOUT_AWAITING_COSIGN, PAYOUT_BROADCASTING,
            PAYOUT_SENT, PAYOUT_CONFIRMED, PAYOUT_FAILED, PAYOUT_CANCELLED,
            WALLET_PAID);

    static {
        for (EventType value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private EventType(String wire) {
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
    public static EventType of(String wire) {
        if (wire == null) return null;
        EventType known = KNOWN.get(wire);
        return known != null ? known : new EventType(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static EventType from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EventType value && value.wire.equals(wire);
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
