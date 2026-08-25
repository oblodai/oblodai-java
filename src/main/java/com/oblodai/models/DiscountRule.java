package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * A payer-facing price adjustment for one currency-and-network pair.
 *
 * @param currency the asset the adjustment applies to
 * @param network the network the adjustment applies to
 * @param discountPercent positive = discount for the payer, negative = markup
 */
public record DiscountRule(
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("discount_percent") Integer discountPercent) {}
