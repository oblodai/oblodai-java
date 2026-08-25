package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.AmountMode;
import com.oblodai.contract.Network;

/**
 * {@code GET /v1/link/&#123;id&#125;} — the payer-facing view of a payment link.
 *
 * @param linkId the link's identifier
 * @param title the title the payer sees
 * @param description the description the payer sees
 * @param amountMode how the link prices a checkout: {@code fixed}, {@code open} or {@code range}
 * @param currency the currency the link prices in
 * @param amountFixed the fixed price, a decimal string; {@code fixed} links
 * @param minAmount the lower bound, a decimal string; {@code range} links
 * @param maxAmount the upper bound, a decimal string; {@code range} links
 * @param pinnedCurrency the settlement asset the link forces, when it pins one
 * @param pinnedNetwork the settlement network the link forces, when it pins one
 */
public record PublicPaymentLink(
        @JsonProperty("link_id") String linkId,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("amount_mode") AmountMode amountMode,
        @JsonProperty("currency") String currency,
        @JsonProperty("amount_fixed") String amountFixed,
        @JsonProperty("min_amount") String minAmount,
        @JsonProperty("max_amount") String maxAmount,
        @JsonProperty("pinned_currency") String pinnedCurrency,
        @JsonProperty("pinned_network") Network pinnedNetwork) {}
