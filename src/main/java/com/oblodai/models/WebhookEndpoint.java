package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/webhooks} — the registered endpoint.
 *
 * @param endpointId the endpoint's identifier
 * @param url where deliveries are posted
 * @param secret shown once: at first registration and at rotation. Absent when only the URL was
 *     changed
 */
public record WebhookEndpoint(
        @JsonProperty("endpoint_id") String endpointId,
        @JsonProperty("url") String url,
        @JsonProperty("secret") String secret) {}
