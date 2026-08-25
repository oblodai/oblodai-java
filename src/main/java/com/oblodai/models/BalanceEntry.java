package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One asset's balance.
 *
 * @param currency the asset
 * @param balance available (spendable) balance, a decimal string at the asset's own scale
 */
public record BalanceEntry(
        @JsonProperty("currency") String currency, @JsonProperty("balance") String balance) {}
