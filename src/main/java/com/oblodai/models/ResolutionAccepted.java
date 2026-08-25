package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/resolve} with {@code action: "accept"} — the underpayment was kept as full
 * settlement.
 *
 * @param resolution always {@code accepted} for this shape
 * @param paymentUuid the invoice that was resolved
 * @param orderId the merchant reference of that invoice
 * @param currency the asset the invoice settled in
 * @param amountKept what was kept as full settlement, a decimal string at the asset's own scale
 */
public record ResolutionAccepted(
        @JsonProperty("resolution") String resolution,
        @JsonProperty("payment_uuid") String paymentUuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("currency") String currency,
        @JsonProperty("amount_kept") String amountKept)
        implements Resolution {}
