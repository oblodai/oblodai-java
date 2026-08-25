package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * Item of {@code /v1/payment/services} and {@code /v1/payout/services}: one currency-and-network
 * pair, with its limits and its fee.
 *
 * @param currency the asset
 * @param network the settlement network
 * @param isAvailable whether the pair can be used right now
 * @param limit the amount bounds; null amounts mean the asset cannot be priced right now
 * @param commission how the fee for this pair is computed
 */
public record ServiceMethod(
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("is_available") Boolean isAvailable,
        @JsonProperty("limit") Limit limit,
        @JsonProperty("commission") Commission commission) {

    /**
     * Amount bounds for a currency-and-network pair. Limits are null when the asset cannot be
     * priced right now.
     *
     * @param currency the currency the bounds are expressed in, when reported
     * @param minAmount the smallest accepted amount, a decimal string; null when unpriceable
     * @param maxAmount the largest accepted amount, a decimal string; null when unpriceable
     */
    public record Limit(
            @JsonProperty("currency") String currency,
            @JsonProperty("min_amount") String minAmount,
            @JsonProperty("max_amount") String maxAmount) {}

    /**
     * How the fee for a currency-and-network pair is computed.
     *
     * @param currency the currency the fee is expressed in
     * @param feeAmount the fixed part of the fee, a decimal string; null when unpriceable
     * @param percent the proportional part of the fee, in percent, as a decimal string; null when
     *     unpriceable
     * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
     *     {@code estimated}, …)
     */
    public record Commission(
            @JsonProperty("currency") String currency,
            @JsonProperty("fee_amount") String feeAmount,
            @JsonProperty("percent") String percent,
            @JsonProperty("fee_type") String feeType) {}
}
