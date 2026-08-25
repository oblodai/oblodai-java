package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/payment/link/toggle} — the link's new activation state.
 *
 * @param linkId the link that was toggled
 * @param active whether the link accepts checkouts now
 */
public record PaymentLinkToggled(
        @JsonProperty("link_id") String linkId, @JsonProperty("active") Boolean active) {}
