package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.PaymentStatus;

/**
 * One invoice spawned by a payment link, as {@code /v1/payment/link/info} lists it.
 *
 * @param uuid the invoice's identifier
 * @param orderId merchant reference, when the checkout carried one
 * @param amount the priced amount, a decimal string at the asset's own scale
 * @param currency the currency the invoice was priced in
 * @param status the invoice's lifecycle state
 * @param createdAt when the invoice was created, RFC 3339 UTC
 */
public record PaymentLinkPayment(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("created_at") String createdAt) {}
