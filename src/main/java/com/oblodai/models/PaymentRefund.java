package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.PayoutStatus;

/**
 * A refund issued against an invoice — a payout in disguise; full detail via
 * {@code payouts.info}.
 *
 * @param uuid the refund payout's identifier; pass it to {@code payouts.info}
 * @param address the address the refund was sent to
 * @param amount the refunded amount, a decimal string at the asset's own scale
 * @param status the refund payout's lifecycle state
 * @param isFinal true once the status can no longer change
 * @param txid the on-chain transaction id, empty until the refund is broadcast
 * @param createdAt when the refund was created, RFC 3339 UTC
 */
public record PaymentRefund(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("address") String address,
        @JsonProperty("amount") String amount,
        @JsonProperty("status") PayoutStatus status,
        @JsonProperty("is_final") Boolean isFinal,
        @JsonProperty("txid") String txid,
        @JsonProperty("created_at") String createdAt) {}
