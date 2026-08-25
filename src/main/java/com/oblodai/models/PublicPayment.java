package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;

/**
 * The payer-facing view ({@code GET /v1/pay/&#123;id&#125;}, {@code /select}, link checkout): the
 * same invoice as {@link Payment} with every merchant-only field left out.
 *
 * @param uuid the invoice's identifier
 * @param orderId merchant reference
 * @param status the invoice's lifecycle state
 * @param isFinal true once the status can no longer change
 * @param amount priced amount in {@code currency}, a decimal string
 * @param currency the currency the invoice is priced in
 * @param network settlement network; empty until the payer selects one on a multi-network invoice
 * @param payerAmount amount due in the payer asset ({@code payer_currency}), a decimal string
 * @param payerCurrency the asset the payer actually sends
 * @param amountPaid what has landed so far, in {@code payer_currency}, a decimal string
 * @param amountRemaining what is still due, in {@code payer_currency}, a decimal string
 * @param address the deposit address the payer sends to
 * @param destinationTag XRP destination tag, Stellar memo or TON memo, when the network needs one
 * @param memo the memo the payer must attach, when the network needs one
 * @param addressXaddress the XRP X-address form of the deposit address, when applicable
 * @param addressMuxed the Stellar muxed form of the deposit address, when applicable
 * @param addressQrCode {@code data:image/png;base64,…} QR of the payment URI
 * @param isMulti true when the invoice accepts several partial payments
 * @param url the hosted pay page
 * @param urlReturn where the payer is sent back to on abandon
 * @param urlSuccess where the payer is sent on success
 * @param expiredAt when the invoice expires, RFC 3339 UTC
 * @param rateExpiresAt when the quoted rate stops holding, RFC 3339 UTC
 * @param confirmations confirmations seen so far
 * @param requiredConfirmations confirmations required before the invoice is credited
 * @param txid the on-chain transaction id of the deposit, empty until one lands
 * @param createdAt when the invoice was created, RFC 3339 UTC
 * @param updatedAt when the invoice last changed, RFC 3339 UTC
 */
public record PublicPayment(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("is_final") Boolean isFinal,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("network") Network network,
        @JsonProperty("payer_amount") String payerAmount,
        @JsonProperty("payer_currency") String payerCurrency,
        @JsonProperty("amount_paid") String amountPaid,
        @JsonProperty("amount_remaining") String amountRemaining,
        @JsonProperty("address") String address,
        @JsonProperty("destination_tag") String destinationTag,
        @JsonProperty("memo") String memo,
        @JsonProperty("address_xaddress") String addressXaddress,
        @JsonProperty("address_muxed") String addressMuxed,
        @JsonProperty("address_qr_code") String addressQrCode,
        @JsonProperty("is_multi") Boolean isMulti,
        @JsonProperty("url") String url,
        @JsonProperty("url_return") String urlReturn,
        @JsonProperty("url_success") String urlSuccess,
        @JsonProperty("expired_at") String expiredAt,
        @JsonProperty("rate_expires_at") String rateExpiresAt,
        @JsonProperty("confirmations") Integer confirmations,
        @JsonProperty("required_confirmations") Integer requiredConfirmations,
        @JsonProperty("txid") String txid,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {}
