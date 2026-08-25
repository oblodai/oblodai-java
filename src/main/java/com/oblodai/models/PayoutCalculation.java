package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;
import com.oblodai.contract.Network;

/**
 * {@code /v1/payout/calculate}. Amounts are null when the asset cannot be priced right now.
 *
 * @param amount the amount that would reach the recipient, a decimal string; null when unpriceable
 * @param currency the asset being sent
 * @param network the settlement network
 * @param commission the fee that would be charged, a decimal string; null when unpriceable
 * @param payerAmount the total that would be debited from the balance, a decimal string; null when
 *     unpriceable
 * @param feeBearer who would pay the fee
 * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
 *     {@code estimated}, …)
 */
public record PayoutCalculation(
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("commission") String commission,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("fee_bearer") FeeBearerResult feeBearer,
        @JsonProperty("fee_type") String feeType) {}
