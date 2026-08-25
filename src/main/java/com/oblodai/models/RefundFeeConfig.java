package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payout/refund-fee-config/*} — whether the customer bears the refund fee.
 *
 * @param feeOnCustomer true when the fee is deducted from the refunded amount
 * @param configured {@code get} only: whether the merchant ever set it
 */
public record RefundFeeConfig(
        @JsonProperty("fee_on_customer") Boolean feeOnCustomer,
        @JsonProperty("configured") Boolean configured) {}
