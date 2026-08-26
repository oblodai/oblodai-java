package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/merchants} — a freshly provisioned merchant and its API key.
 *
 * @param merchantId the new merchant's identifier
 * @param projectId the merchant's default project (store) identifier
 * @param apiKey the merchant's one API key; it signs every signed route
 */
public record MerchantOnboarded(
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("api_key") ApiKeyPair apiKey) {}
