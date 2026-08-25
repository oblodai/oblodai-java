package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearerResult;
import com.oblodai.contract.Network;

/**
 * {@code /v1/payout/validate} — the dry run; errors are the same the create call would raise.
 *
 * @param valid true when the payout would be accepted as described
 * @param amount the amount that would reach the recipient, a decimal string
 * @param currency the asset being sent
 * @param network the settlement network
 * @param commission the fee that would be charged, a decimal string
 * @param payerAmount the total that would be debited from the balance, a decimal string
 * @param feeBearer who would pay the fee
 * @param fundedBy which balance would fund it ({@code business} or {@code personal}), when
 *     reported
 * @param maturityNote non-empty when part of the balance is still maturing (reorg window)
 */
public record PayoutValidation(
        @JsonProperty("valid") Boolean valid,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("commission") String commission,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("fee_bearer") FeeBearerResult feeBearer,
        @JsonProperty("funded_by") String fundedBy,
        @JsonProperty("maturity_note") String maturityNote) {}
