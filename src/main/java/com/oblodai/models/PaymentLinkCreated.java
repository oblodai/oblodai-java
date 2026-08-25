package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /v1/payment/link} acknowledgement.
 *
 * @param linkId the new link's identifier
 * @param url the hosted checkout page
 * @param documentUrl signed link to the payment link's PDF document
 */
public record PaymentLinkCreated(
        @JsonProperty("link_id") String linkId,
        @JsonProperty("url") String url,
        @JsonProperty("document_url") String documentUrl) {}
