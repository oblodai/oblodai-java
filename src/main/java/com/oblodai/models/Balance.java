package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code /v1/balance} — every asset the merchant holds.
 *
 * @param balance the wallets, grouped by owner
 */
public record Balance(@JsonProperty("balance") Wallets balance) {

    /**
     * The wallets a balance answer is grouped into.
     *
     * @param merchant the merchant's own balances, one entry per asset
     */
    public record Wallets(@JsonProperty("merchant") List<BalanceEntry> merchant) {}
}
