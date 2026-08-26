package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * {@code wallet.paid} — a deposit landed on a static wallet.
 *
 * @param type always {@code wallet} for this shape
 * @param uuid the wallet's identifier
 * @param orderId merchant reference of the wallet; null when it has none
 * @param status always {@code paid} for this shape
 * @param isFinal true once the status can no longer change
 * @param address the wallet's deposit address
 * @param currency the asset the wallet accepts
 * @param network the network the deposit arrived on
 * @param payerCurrency the asset the payer actually sent
 * @param paymentAmount what landed on the address, in {@code payer_currency}, a decimal string
 * @param txid the on-chain transaction id of the deposit
 * @param eventAt when the state change was committed, RFC 3339 UTC
 * @param sequence global, increasing sequence number of the event
 * @param test true ONLY on a rehearsal delivery ({@code webhooks().test(...)}, sandbox): the body is
 *     signed like a live one, so never act on it as if money moved; null on a live delivery
 */
public record WalletEvent(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") String uuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") String status,
        @JsonProperty("is_final") Boolean isFinal,
        @JsonProperty("address") String address,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("payer_currency") String payerCurrency,
        @JsonProperty("payment_amount") String paymentAmount,
        @JsonProperty("txid") String txid,
        @JsonProperty("event_at") String eventAt,
        @JsonProperty("sequence") Long sequence,
        @JsonProperty("test") Boolean test)
        implements WebhookEvent {}
