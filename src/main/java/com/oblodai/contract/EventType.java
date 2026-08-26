// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Webhook event types: {@code invoice.<status>}, {@code payout.<status>}, {@code wallet.paid}.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum EventType {

    /** {@code invoice.select} */
    INVOICE_SELECT("invoice.select"),

    /** {@code invoice.created} */
    INVOICE_CREATED("invoice.created"),

    /** {@code invoice.confirm_check} */
    INVOICE_CONFIRM_CHECK("invoice.confirm_check"),

    /** {@code invoice.paid} */
    INVOICE_PAID("invoice.paid"),

    /** {@code invoice.paid_over} */
    INVOICE_PAID_OVER("invoice.paid_over"),

    /** {@code invoice.wrong_amount} */
    INVOICE_WRONG_AMOUNT("invoice.wrong_amount"),

    /** {@code invoice.expired} */
    INVOICE_EXPIRED("invoice.expired"),

    /** {@code invoice.cancelled} */
    INVOICE_CANCELLED("invoice.cancelled"),

    /** {@code payout.pending} */
    PAYOUT_PENDING("payout.pending"),

    /** {@code payout.approved} */
    PAYOUT_APPROVED("payout.approved"),

    /** {@code payout.awaiting_cosign} */
    PAYOUT_AWAITING_COSIGN("payout.awaiting_cosign"),

    /** {@code payout.broadcasting} */
    PAYOUT_BROADCASTING("payout.broadcasting"),

    /** {@code payout.sent} */
    PAYOUT_SENT("payout.sent"),

    /** {@code payout.confirmed} */
    PAYOUT_CONFIRMED("payout.confirmed"),

    /** {@code payout.failed} */
    PAYOUT_FAILED("payout.failed"),

    /** {@code payout.cancelled} */
    PAYOUT_CANCELLED("payout.cancelled"),

    /** {@code wallet.paid} */
    WALLET_PAID("wallet.paid"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    EventType(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static EventType from(String wire) {
        if (wire == null) return null;
        for (EventType value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
