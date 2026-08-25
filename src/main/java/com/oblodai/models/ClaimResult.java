package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearer;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutLinkStatus;

/**
 * {@code POST /v1/claim/&#123;token&#125;} — the payout minted by a claim.
 *
 * @param payoutId the payout that pays the recipient (look it up with
 *     {@code payouts.info(&#123; uuid: payout_id &#125;)})
 * @param status the payout's lifecycle state
 * @param address the address the recipient claimed to
 * @param amount the amount sent to the recipient, a decimal string
 * @param currency the asset being sent
 * @param network the settlement network
 * @param commission the fee charged, a decimal string; null while the asset cannot be priced
 * @param payerAmount total debited from the sender's balance, a decimal string; null while the
 *     asset cannot be priced
 * @param feeBearer who was configured to pay the fee
 * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
 *     {@code estimated}, …)
 */
public record ClaimResult(
        @JsonProperty("payout_id") String payoutId,
        @JsonProperty("status") PayoutLinkStatus status,
        @JsonProperty("address") String address,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("commission") String commission,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("fee_bearer") FeeBearer feeBearer,
        @JsonProperty("fee_type") String feeType) {}
