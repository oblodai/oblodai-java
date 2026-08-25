package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;

/**
 * How a fee was settled on a priced result.
 *
 * @param commission the fee itself, a decimal string at the asset's own scale
 * @param feeBearer who ended up paying the fee
 * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
 *     {@code estimated}, …)
 */
public record FeeInfo(
        @JsonProperty("commission") String commission,
        @JsonProperty("fee_bearer") FeeBearerResult feeBearer,
        @JsonProperty("fee_type") String feeType) {}
