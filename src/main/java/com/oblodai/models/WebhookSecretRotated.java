package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/webhooks/rotate-secret} — the endpoint with its new secret, and how long the old
 * one keeps working.
 *
 * @param endpointId the endpoint's identifier
 * @param url where deliveries are posted
 * @param secret the new signing secret, shown once
 * @param previousSecretValidUntil until then deliveries also carry
 *     {@code X-Webhook-Signature-Prev} signed with the old secret, RFC 3339 UTC
 */
public record WebhookSecretRotated(
        @JsonProperty("endpoint_id") String endpointId,
        @JsonProperty("url") String url,
        @JsonProperty("secret") String secret,
        @JsonProperty("previous_secret_valid_until") String previousSecretValidUntil) {}
