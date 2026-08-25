package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutStatus;

/**
 * {@code /v1/payment/resolve} with {@code action: "refund"} — the underpayment was sent back; the
 * body is the refund payout itself, with a {@code resolution} field added.
 *
 * <p>Every component of {@link Payout} is repeated here because a Java record cannot extend
 * another record.
 *
 * @param resolution always {@code refunded} for this shape
 * @param uuid the refund payout's identifier
 * @param orderId merchant reference; null for refunds (they are keyed by {@code refund_for})
 * @param status the refund payout's lifecycle state
 * @param isFinal true once the status can no longer change
 * @param amount the refunded amount, a decimal string at the asset's own scale
 * @param currency the asset being sent back
 * @param network the settlement network
 * @param address the address the refund was sent to
 * @param memo XRP destination tag, Stellar memo or TON memo, when the network needs one
 * @param payerAmount total debited from the balance, a decimal string
 * @param commission the fee charged for the refund, a decimal string
 * @param feeBearer who ended up paying the fee
 * @param source balance the refund was funded from
 * @param approvalRequired true when the refund waits for a second approval before it is sent
 * @param isRefund true — this payout is a refund
 * @param refundFor the invoice being refunded
 * @param paymentOrderId the merchant reference of the invoice being refunded
 * @param txid the on-chain transaction id, empty until the refund is broadcast
 * @param documentUrl signed link to the refund's PDF document
 * @param createdAt when the refund was created, RFC 3339 UTC
 * @param updatedAt when the refund last changed, RFC 3339 UTC
 * @param error human-readable failure text, on failed refunds
 * @param errorCode machine-readable failure code, on failed refunds
 * @param walletUuid set on refunds of blocked static-wallet deposits
 */
public record ResolutionRefunded(
        @JsonProperty("resolution") String resolution,
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
        @JsonProperty("error") String error,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("wallet_uuid") String walletUuid)
        implements Resolution {}
