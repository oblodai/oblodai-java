package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * One on-chain deposit attributed to an invoice.
 *
 * @param txid the on-chain transaction id
 * @param amount what landed, a decimal string at the asset's own scale
 * @param network the network the deposit arrived on
 * @param height the block height the transaction was mined at
 * @param createdAt when the deposit was attributed, RFC 3339 UTC
 */
public record PaymentTx(
        @JsonProperty("txid") String txid,
        @JsonProperty("amount") String amount,
        @JsonProperty("network") Network network,
        @JsonProperty("height") Integer height,
        @JsonProperty("created_at") String createdAt) {}
