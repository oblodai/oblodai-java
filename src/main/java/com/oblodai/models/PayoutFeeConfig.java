package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payout/fee-config/*} — whether the recipient bears the payout fee.
 *
 * @param feeOnRecipient true when the fee is deducted from the recipient's amount
 * @param configured {@code get} only: whether the merchant ever set it
 */
public record PayoutFeeConfig(
        @JsonProperty("fee_on_recipient") Boolean feeOnRecipient,
        @JsonProperty("configured") Boolean configured) {}
