package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/send-email} — acknowledgement that the cheque was mailed.
 *
 * @param ok true when the mail was accepted for delivery
 * @param email the recipient it went to
 * @param uuid the invoice the cheque belongs to
 */
public record EmailSent(
        @JsonProperty("ok") Boolean ok,
        @JsonProperty("email") String email,
        @JsonProperty("uuid") String uuid) {}
