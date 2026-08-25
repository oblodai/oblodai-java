package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutStatus;

/**
 * Payout as {@code /v1/payout}, {@code /info}, {@code /history}, {@code /cancel}, mass and batch
 * elements and refunds render it (core {@code PayoutResult}).
 *
 * <p>{@code error} and {@code error_code} appear on {@code info} for failed payouts.
 *
 * @param uuid the payout's identifier
 * @param orderId merchant reference; null for refunds (they are keyed by {@code reference} and
 *     {@code refund_for})
 * @param status the payout's lifecycle state
 * @param isFinal true once the status can no longer change
 * @param amount the amount sent to the recipient, a decimal string at the asset's own scale
 * @param currency the asset being sent
 * @param network the settlement network
 * @param address the recipient address
 * @param memo XRP destination tag, Stellar memo or TON memo, when the network needs one
 * @param payerAmount total debited from the balance (amount plus commission when the merchant
 *     bears the fee), a decimal string
 * @param commission the fee charged for the payout, a decimal string
 * @param feeBearer who ended up paying the fee
 * @param source balance the payout was funded from ({@code business}, {@code personal}, …)
 * @param approvalRequired true when the payout waits for a second approval before it is sent
 * @param isRefund true when this payout is a refund of an invoice
 * @param refundFor for refunds: the invoice being refunded
 * @param paymentOrderId for refunds: the merchant reference of the invoice being refunded
 * @param txid the on-chain transaction id, empty until the payout is broadcast
 * @param documentUrl signed link to the payout's PDF document
 * @param createdAt when the payout was created, RFC 3339 UTC
 * @param updatedAt when the payout last changed, RFC 3339 UTC
 * @param error human-readable failure text; {@code info} only, on failed payouts
 * @param errorCode machine-readable failure code; {@code info} only, on failed payouts
 * @param walletUuid set on refunds of blocked static-wallet deposits
 */
public record Payout(
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
        @JsonProperty("wallet_uuid") String walletUuid) {}
