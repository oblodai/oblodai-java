package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/wallet/block} — the wallet's new blocked state.
 *
 * @param uuid the wallet that was blocked or unblocked
 * @param address the wallet's deposit address
 * @param blocked whether deposits to the address are refused now
 */
public record WalletBlocked(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("address") String address,
        @JsonProperty("blocked") Boolean blocked) {}
