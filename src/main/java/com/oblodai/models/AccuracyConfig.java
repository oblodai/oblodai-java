package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/accuracy/*} — the under and overpayment tolerance applied to invoices.
 *
 * @param enabled whether the tolerance is applied
 * @param accuracyPercent the tolerance, in percent
 */
public record AccuracyConfig(
        @JsonProperty("enabled") Boolean enabled,
        @JsonProperty("accuracy_percent") Integer accuracyPercent) {}
