package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/qr} and {@code GET /v1/pay/&#123;id&#125;/qr}.
 *
 * <p>All fields are empty while the invoice has no real address: sandbox invoices (which carry a
 * synthetic {@code sandbox:} address) and {@code select} invoices still awaiting a network.
 *
 * @param image the QR itself, as {@code data:image/png;base64,…}
 * @param payload what the QR encodes: a payment URI when {@code is_uri}, else the bare address
 * @param isUri whether {@code payload} is a payment URI rather than a bare address
 * @param address the deposit address the QR points at
 */
public record QrCode(
        @JsonProperty("image") String image,
        @JsonProperty("payload") String payload,
        @JsonProperty("is_uri") Boolean isUri,
        @JsonProperty("address") String address) {}
