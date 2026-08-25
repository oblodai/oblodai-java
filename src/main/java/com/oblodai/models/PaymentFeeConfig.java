package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/fee-config/*} — how much of the acceptance fee the payer carries.
 *
 * @param payerPaysPercent the share of the fee charged to the payer, in percent
 * @param enabled {@code get} only: whether the merchant ever set it
 */
public record PaymentFeeConfig(
        @JsonProperty("payer_pays_percent") Integer payerPaysPercent,
        @JsonProperty("enabled") Boolean enabled) {}
