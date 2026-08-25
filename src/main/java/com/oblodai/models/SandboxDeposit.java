package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/sandbox/deposit} — a simulated on-chain deposit against a sandbox invoice.
 *
 * @param invoiceId the invoice the deposit was attributed to
 * @param amount the deposited amount, a decimal string at the asset's own scale
 * @param confirmations how many confirmations the synthetic transaction was given
 * @param txid the synthetic transaction id
 */
public record SandboxDeposit(
        @JsonProperty("invoice_id") String invoiceId,
        @JsonProperty("amount") String amount,
        @JsonProperty("confirmations") Integer confirmations,
        @JsonProperty("txid") String txid) {}
