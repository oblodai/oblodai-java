package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * One currency-and-network pair the merchant accepts, and whether it can be offered right now.
 *
 * @param currency the asset
 * @param network the settlement network
 * @param available whether the pair can be offered right now
 * @param reason why it is unavailable, when it is
 */
public record AcceptedMethod(
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("available") Boolean available,
        @JsonProperty("reason") String reason) {}
