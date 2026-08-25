package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/sandbox/reset} — what the reset swept away.
 *
 * @param invoicesCancelled how many open invoices were cancelled
 * @param balancesZeroed how many balances were zeroed
 */
public record SandboxReset(
        @JsonProperty("invoices_cancelled") Integer invoicesCancelled,
        @JsonProperty("balances_zeroed") Integer balancesZeroed) {}
