package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/merchants} — a freshly provisioned merchant and its keys.
 *
 * @param merchantId the new merchant's identifier
 * @param projectId the merchant's default project (store) identifier
 * @param apiKey the unified key (same as {@code payment_key} and {@code payout_key} for merchants
 *     created now)
 * @param paymentKey the key scoped to payment acceptance
 * @param payoutKey the key scoped to payouts
 */
public record MerchantOnboarded(
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("api_key") ApiKeyPair apiKey,
        @JsonProperty("payment_key") ApiKeyPair paymentKey,
        @JsonProperty("payout_key") ApiKeyPair payoutKey) {}
