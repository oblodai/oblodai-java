package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A delivered webhook event, discriminated by its {@code type} field: {@code payment} for
 * {@code invoice.&lt;status&gt;}, {@code payout} for {@code payout.&lt;status&gt;} and
 * {@code wallet} for {@code wallet.paid}.
 *
 * <p>The accessors declared here are the fields every delivered event carries, whatever its type.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PaymentEvent.class, name = "payment"),
    @JsonSubTypes.Type(value = PayoutEvent.class, name = "payout"),
    @JsonSubTypes.Type(value = WalletEvent.class, name = "wallet")
})
public sealed interface WebhookEvent permits PaymentEvent, PayoutEvent, WalletEvent {

    /** The discriminator: {@code payment}, {@code payout} or {@code wallet}. */
    String type();

    /** The identifier of the object the event is about. */
    String uuid();

    /** Merchant reference; null on refund payouts. */
    String orderId();

    /** True once the status can no longer change. */
    Boolean isFinal();

    /**
     * When the state change was committed, RFC 3339 UTC — order events by this, or by
     * {@link #sequence()}.
     */
    String eventAt();

    /**
     * Global, increasing (gaps are normal); a lower sequence arriving later is stale and should be
     * dropped.
     */
    Long sequence();

    /** The on-chain transaction id, empty when there is none yet. */
    String txid();

    /**
     * Present and true ONLY on rehearsal deliveries ({@code webhooks().test(...)}, sandbox); null on
     * a live one. The body is signed exactly like a live delivery, so a handler must check this flag
     * (or the {@code X-Webhook-Test} header) and never act on a test event as if money moved.
     */
    Boolean test();
}
