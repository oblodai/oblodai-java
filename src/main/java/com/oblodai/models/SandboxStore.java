package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/merchants/&#123;id&#125;/sandbox} — the merchant's dev store and its
 * {@code test_} key.
 *
 * <p>Carries everything {@link MerchantOnboarded} carries, plus {@code created}. It repeats those
 * components rather than extending the type because a Java record cannot extend another record.
 *
 * @param merchantId the merchant the dev store belongs to
 * @param projectId the dev store's project identifier
 * @param apiKey the dev store's API key
 * @param created false when the dev store already existed (the call is idempotent)
 */
public record SandboxStore(
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("api_key") ApiKeyPair apiKey,
        @JsonProperty("created") Boolean created) {}
