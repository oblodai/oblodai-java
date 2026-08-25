package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.AmountMode;
import com.oblodai.contract.Network;
import java.util.List;

/**
 * Payment link as {@code /v1/payment/link/info} and {@code /list} render it. Which amount fields
 * are present depends on {@code amount_mode}.
 *
 * @param linkId the link's identifier
 * @param url the hosted checkout page
 * @param active whether the link accepts checkouts right now
 * @param title the title the payer sees
 * @param description the description the payer sees
 * @param amountMode how the link prices a checkout: {@code fixed}, {@code open} or {@code range}
 * @param currency the currency the link prices in
 * @param amountFixed the fixed price, a decimal string; {@code fixed} links
 * @param minAmount the lower bound, a decimal string; {@code range} links
 * @param maxAmount the upper bound, a decimal string; {@code range} links
 * @param pinnedCurrency the settlement asset the link forces, when it pins one
 * @param pinnedNetwork the settlement network the link forces, when it pins one
 * @param expiresAt when the link stops accepting checkouts, RFC 3339 UTC
 * @param documentUrl signed link to the payment link's PDF document
 * @param createdAt when the link was created, RFC 3339 UTC
 * @param payments {@code info} only: invoices spawned by this link
 */
public record PaymentLink(
        @JsonProperty("link_id") String linkId,
        @JsonProperty("url") String url,
        @JsonProperty("active") Boolean active,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("amount_mode") AmountMode amountMode,
        @JsonProperty("currency") String currency,
        @JsonProperty("amount_fixed") String amountFixed,
        @JsonProperty("min_amount") String minAmount,
        @JsonProperty("max_amount") String maxAmount,
        @JsonProperty("pinned_currency") String pinnedCurrency,
        @JsonProperty("pinned_network") Network pinnedNetwork,
        @JsonProperty("expires_at") String expiresAt,
        @JsonProperty("document_url") String documentUrl,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("payments") List<PaymentLinkPayment> payments) {}
