package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * {@code POST /v1/webhooks} — the registered endpoint.
 *
 * @param endpointId the endpoint's identifier
 * @param url where deliveries are posted
 * @param secret shown once: at first registration and at rotation. Absent when only the URL was
 *     changed. Readable here, redacted in {@code toString()} and in JSON — store it, do not log it
 */
public record WebhookEndpoint(
        @JsonProperty("endpoint_id") String endpointId,
        @JsonProperty("url") String url,
        @JsonProperty("secret") @JsonSerialize(using = Secrets.RedactingSerializer.class)
                String secret) {

    @Override
    public String toString() {
        return "WebhookEndpoint[endpointId="
                + endpointId
                + ", url="
                + url
                + ", secret="
                + Secrets.describe(secret)
                + "]";
    }
}
