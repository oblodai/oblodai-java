package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutStatus;

/**
 * {@code payout.&lt;status&gt;} — a payout (or refund) changed state; the body is the payout
 * itself, plus {@code type}, {@code event_at} and {@code sequence}.
 *
 * @param type always {@code payout} for this shape
 * @param uuid the payout's identifier
 * @param orderId merchant reference; null on refund payouts
 * @param status the payout's new lifecycle state
 * @param isFinal true once the status can no longer change
 * @param amount the amount sent to the recipient, a decimal string
 * @param currency the asset being sent
 * @param network the settlement network
 * @param address the recipient address
 * @param memo XRP destination tag, Stellar memo or TON memo, when the network needs one
 * @param payerAmount total debited from the balance, a decimal string
 * @param commission the fee charged for the payout, a decimal string
 * @param feeBearer who ended up paying the fee
 * @param source balance the payout was funded from
 * @param approvalRequired true when the payout waits for a second approval before it is sent
 * @param isRefund true when this payout is a refund of an invoice
 * @param refundFor for refunds: the invoice being refunded
 * @param paymentOrderId for refunds: the merchant reference of the invoice being refunded
 * @param txid the on-chain transaction id, empty until the payout is broadcast
 * @param documentUrl signed link to the payout's PDF document
 * @param createdAt when the payout was created, RFC 3339 UTC
 * @param updatedAt when the payout last changed, RFC 3339 UTC
 * @param eventAt when the state change was committed, RFC 3339 UTC
 * @param sequence global, increasing sequence number of the event
 */
public record PayoutEvent(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") String uuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") PayoutStatus status,
        @JsonProperty("is_final") Boolean isFinal,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("address") String address,
        @JsonProperty("memo") String memo,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("commission") String commission,
        @JsonProperty("fee_bearer") FeeBearerResult feeBearer,
        @JsonProperty("source") String source,
        @JsonProperty("approval_required") Boolean approvalRequired,
        @JsonProperty("is_refund") Boolean isRefund,
        @JsonProperty("refund_for") String refundFor,
        @JsonProperty("payment_order_id") String paymentOrderId,
        @JsonProperty("txid") String txid,
        @JsonProperty("document_url") String documentUrl,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("event_at") String eventAt,
        @JsonProperty("sequence") Long sequence)
        implements WebhookEvent {}
