package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/wallet/qr} — the address QR as a data URI.
 *
 * @param image the QR itself, as {@code data:image/png;base64,…}
 */
public record WalletQr(@JsonProperty("image") String image) {}
