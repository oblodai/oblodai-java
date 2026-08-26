package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.oblodai.contract.FeeBearer;
import com.oblodai.contract.Network;
import com.oblodai.contract.PayoutLinkStatus;

/**
 * Payout link (cheque) as {@code /v1/payout/link}, {@code /info}, {@code /list}, {@code /cancel}
 * and batch elements render it.
 *
 * @param linkId the link's identifier
 * @param status the link's lifecycle state
 * @param amount the amount the recipient claims, a decimal string at the asset's own scale
 * @param currency the asset being sent
 * @param network the settlement network
 * @param commission the fee charged for the claim payout, a decimal string; null while the asset
 *     cannot be priced
 * @param payerAmount total debited from the balance, a decimal string; null while the asset cannot
 *     be priced
 * @param feeBearer who was configured to pay the fee
 * @param feeType the pricing mode ({@code percent}, {@code fixed}, {@code exact},
 *     {@code estimated}, …)
 * @param reference merchant reference for the link
 * @param title the title the recipient sees
 * @param note the note the recipient sees
 * @param passcodeProtected true when claiming needs a passcode
 * @param expiresAt when the link stops being claimable, RFC 3339 UTC
 * @param createdAt when the link was created, RFC 3339 UTC
 * @param claimToken present on create and batch-create only — the secret the recipient claims
 *     with. Readable here, redacted in {@code toString()} and in JSON
 * @param claimUrl present on create and batch-create only — the ready-made claim link. It embeds
 *     the claim token, so it is protected exactly like the token: readable here, redacted in
 *     {@code toString()} and in JSON
 * @param batchId the batch this link was created by, on batch-create
 * @param payoutId set once claimed: the payout that paid the recipient
 * @param claimAddress set once claimed: the address the recipient claimed to
 * @param email the recipient's email, when one was supplied
 * @param passcode the generated passcode, shown once on create when {@code passcode: "auto"} was
 *     requested. Readable here, redacted in {@code toString()} and in JSON
 */
public record PayoutLink(
        @JsonProperty("link_id") String linkId,
        @JsonProperty("status") PayoutLinkStatus status,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("commission") String commission,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("fee_bearer") FeeBearer feeBearer,
        @JsonProperty("fee_type") String feeType,
        @JsonProperty("reference") String reference,
        @JsonProperty("title") String title,
        @JsonProperty("note") String note,
        @JsonProperty("passcode_protected") Boolean passcodeProtected,
        @JsonProperty("expires_at") String expiresAt,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("claim_token") @JsonSerialize(using = Secrets.RedactingSerializer.class)
                String claimToken,
        @JsonProperty("claim_url") @JsonSerialize(using = Secrets.RedactingSerializer.class)
                String claimUrl,
        @JsonProperty("batch_id") String batchId,
        @JsonProperty("payout_id") String payoutId,
        @JsonProperty("claim_address") String claimAddress,
        @JsonProperty("email") String email,
        @JsonProperty("passcode") @JsonSerialize(using = Secrets.RedactingSerializer.class)
                String passcode) {

    /**
     * The claim token and the passcode are the two halves of "whoever holds this can take the
     * money", so neither appears here. Read them from {@link #claimToken()} and {@link #passcode()}.
     */
    @Override
    public String toString() {
        return "PayoutLink[linkId="
                + linkId
                + ", status="
                + status
                + ", amount="
                + amount
                + " "
                + currency
                + ", network="
                + network
                + ", reference="
                + reference
                + ", expiresAt="
                + expiresAt
                + ", claimToken="
                + Secrets.describe(claimToken)
                + ", claimUrl="
                + Secrets.describe(claimUrl)
                + ", passcode="
                + Secrets.describe(passcode)
                + "]";
    }
}
