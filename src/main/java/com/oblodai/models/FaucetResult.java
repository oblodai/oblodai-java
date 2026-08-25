package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/sandbox/faucet} — test funds credited to a sandbox balance.
 *
 * @param asset the credited asset
 * @param amount the credited amount, a decimal string at the asset's own scale
 * @param journalId the ledger journal entry that recorded the credit
 */
public record FaucetResult(
        @JsonProperty("asset") String asset,
        @JsonProperty("amount") String amount,
        @JsonProperty("journal_id") String journalId) {}
