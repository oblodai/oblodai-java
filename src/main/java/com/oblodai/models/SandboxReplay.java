package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/sandbox/webhooks/replay} — acknowledgement of a redelivery.
 *
 * @param ok true when the delivery was queued again
 * @param deliveryId the delivery that was replayed
 */
public record SandboxReplay(
        @JsonProperty("ok") Boolean ok, @JsonProperty("delivery_id") String deliveryId) {}
