package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.FeeBearer;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutLinkStatus;

/**
 * {@code GET /v1/claim/&#123;token&#125;} — what the recipient sees before claiming.
 *
 * @param status the link's lifecycle state
 * @param claimable whether the link can be claimed right now
 * @param amount the amount the recipient would receive, a decimal string
 * @param currency the asset being sent
 * @param network the settlement network
 * @param commission the fee that would be charged, a decimal string; null while the asset cannot
 *     be priced
 * @param payerAmount the total that would be debited from the sender's balance, a decimal string;
 *     null while the asset cannot be priced
 * @param feeBearer who was configured to pay the fee
 * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
 *     {@code estimated}, …)
 * @param title the title the recipient sees
 * @param note the note the recipient sees
 * @param expiresAt when the link stops being claimable, RFC 3339 UTC
 */
public record ClaimPreview(
        @JsonProperty("status") PayoutLinkStatus status,
        @JsonProperty("claimable") Boolean claimable,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("commission") String commission,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("fee_bearer") FeeBearer feeBearer,
        @JsonProperty("fee_type") String feeType,
        @JsonProperty("title") String title,
        @JsonProperty("note") String note,
        @JsonProperty("expires_at") String expiresAt) {}
