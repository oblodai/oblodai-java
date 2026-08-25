package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.DeliveryStatus;
import com.oblodai.contract.EventType;
import java.util.Map;

/**
 * Item of {@code /v1/webhooks/deliveries} and {@code GET /v1/sandbox/webhooks} (which adds
 * {@code payload} and drops {@code sequence}).
 *
 * @param id the delivery's identifier
 * @param url where the delivery was posted
 * @param eventType the event that was delivered
 * @param status how the delivery ended
 * @param attempts how many attempts have been made
 * @param lastError the last transport or receiver error, empty when there was none
 * @param sequence the event's global sequence number; absent on sandbox listings
 * @param createdAt when the delivery was queued, RFC 3339 UTC
 * @param updatedAt when the delivery last changed, RFC 3339 UTC
 * @param payload the delivered body; sandbox listings only
 */
public record WebhookDelivery(
        @JsonProperty("id") String id,
        @JsonProperty("url") String url,
        @JsonProperty("event_type") EventType eventType,
        @JsonProperty("status") DeliveryStatus status,
        @JsonProperty("attempts") Integer attempts,
        @JsonProperty("last_error") String lastError,
        @JsonProperty("sequence") Long sequence,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("payload") Map<String, Object> payload) {}
