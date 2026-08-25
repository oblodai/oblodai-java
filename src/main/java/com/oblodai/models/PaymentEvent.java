package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;

/**
 * {@code invoice.&lt;status&gt;} — an invoice changed state.
 *
 * @param type always {@code payment} for this shape
 * @param uuid the invoice's identifier
 * @param orderId merchant reference; null when the invoice has none
 * @param status the invoice's new lifecycle state
 * @param isFinal true once the status can no longer change
 * @param amount priced amount in {@code currency}, a decimal string
 * @param currency the currency the invoice is priced in
 * @param network the settlement network
 * @param payerAmount amount due in the payer asset, a decimal string
 * @param payerCurrency the asset the payer sends
 * @param paymentAmount what actually landed on the address, in {@code payer_currency}, a decimal
 *     string
 * @param payerAddress the address the payer sent from, when known
 * @param payerAddressIsRefundable whether a refund can be sent back to {@code payer_address}
 * @param additionalData private merchant data, echoed back from the invoice
 * @param txid the on-chain transaction id, empty until one lands
 * @param eventAt when the state change was committed, RFC 3339 UTC
 * @param sequence global, increasing sequence number of the event
 */
public record PaymentEvent(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") String uuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("is_final") Boolean isFinal,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("payer_currency") String payerCurrency,
        @JsonProperty("payment_amount") String paymentAmount,
        @JsonProperty("payer_address") String payerAddress,
        @JsonProperty("payer_address_is_refundable") Boolean payerAddressIsRefundable,
        @JsonProperty("additional_data") String additionalData,
        @JsonProperty("txid") String txid,
        @JsonProperty("event_at") String eventAt,
        @JsonProperty("sequence") Long sequence)
        implements WebhookEvent {}
