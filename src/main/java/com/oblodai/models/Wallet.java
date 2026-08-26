package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * Static (permanent) deposit wallet — {@code /v1/wallet}.
 *
 * @param uuid the wallet's identifier
 * @param address the permanent deposit address
 * @param network the network the address lives on
 * @param currency the asset the wallet accepts
 * @param orderId merchant reference for the wallet
 * @param url hosted page showing the address and QR
 * @param documentUrl signed link to the wallet's PDF document
 * @param blocked true once {@code wallets().block(...)} was called: deposits to this address are
 *     quarantined instead of credited, with no webhook and no auto-refund — do not publish it
 * @param destinationTag XRP destination tag, or TON and Stellar memo, when the network needs one
 * @param memo the memo the payer must attach, when the network needs one
 * @param addressXaddress the XRP X-address form of the deposit address, when applicable
 * @param addressMuxed the Stellar muxed form of the deposit address, when applicable
 */
public record Wallet(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("address") String address,
        @JsonProperty("network") Network network,
        @JsonProperty("currency") String currency,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("url") String url,
        @JsonProperty("document_url") String documentUrl,
        @JsonProperty("blocked") Boolean blocked,
        @JsonProperty("destination_tag") String destinationTag,
        @JsonProperty("memo") String memo,
        @JsonProperty("address_xaddress") String addressXaddress,
        @JsonProperty("address_muxed") String addressMuxed) {}
