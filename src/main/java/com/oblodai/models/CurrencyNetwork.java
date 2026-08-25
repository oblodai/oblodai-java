package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * One network a currency lives on, as {@code GET /v1/currencies} reports it.
 *
 * @param network the settlement network
 * @param kind {@code native} for the chain's own coin, {@code token} for a contract asset
 * @param contract token contract address, for tokens
 * @param minConfirmations confirmations required before a deposit on this network is credited
 * @param available deposits and payouts both possible right now
 * @param depositAvailable deposits are possible right now
 * @param payoutAvailable payouts are possible right now
 * @param defaultOffer whether this is the network offered first on the pay page
 */
public record CurrencyNetwork(
        @JsonProperty("network") Network network,
        @JsonProperty("kind") String kind,
        @JsonProperty("contract") String contract,
        @JsonProperty("min_confirmations") Integer minConfirmations,
        @JsonProperty("available") Boolean available,
        @JsonProperty("deposit_available") Boolean depositAvailable,
        @JsonProperty("payout_available") Boolean payoutAvailable,
        @JsonProperty("default_offer") Boolean defaultOffer) {}
