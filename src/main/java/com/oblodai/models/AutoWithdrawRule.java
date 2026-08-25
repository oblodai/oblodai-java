package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * {@code /v1/auto-withdraw/*} entry: sweep an asset out to an address once it reaches a threshold.
 *
 * @param currency the asset the rule sweeps
 * @param network the network the sweep is sent on
 * @param address the destination the sweep is sent to
 * @param minAmount the threshold the balance must reach, a decimal string at the asset's own scale
 */
public record AutoWithdrawRule(
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("address") String address,
        @JsonProperty("min_amount") String minAmount) {}
