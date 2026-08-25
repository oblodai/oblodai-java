package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;
import java.util.List;

/**
 * Invoice as {@code /v1/payment}, {@code /v1/payment/info}, {@code /v1/payment/history} and
 * {@code /v1/payment/cancel} render it (core {@code paymentResult}).
 *
 * <p>{@code refunds} and {@code refund_status} are present on {@code info} only.
 *
 * @param uuid the invoice's identifier
 * @param orderId merchant reference, and the invoice's idempotency key
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
 * @param exchangeRate the rate the invoice was priced at, a decimal string
 * @param confirmations confirmations seen so far
 * @param requiredConfirmations confirmations required before the invoice is credited
 * @param txid the on-chain transaction id of the deposit, empty until one lands
 * @param txList every on-chain deposit attributed to this invoice
 * @param paidAt when the invoice was paid, RFC 3339 UTC; null while unpaid
 * @param payerAddress the address the payer sent from, when known
 * @param payerAddressIsRefundable whether a refund can be sent back to {@code payer_address}
 * @param payerEmail the payer's email, when one was supplied
 * @param additionalData private merchant data, echoed back in webhooks
 * @param commission the acceptance fee, a decimal string
 * @param merchantAmount what the merchant nets after the fee, a decimal string
 * @param documentUrl signed link to the invoice's PDF document
 * @param isTest true for sandbox invoices
 * @param createdAt when the invoice was created, RFC 3339 UTC
 * @param updatedAt when the invoice last changed, RFC 3339 UTC
 * @param refunds refunds issued against this invoice; {@code info} only
 * @param refundStatus summary of the refund state; {@code info} only
 */
public record Payment(
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
        @JsonProperty("exchange_rate") String exchangeRate,
        @JsonProperty("confirmations") Integer confirmations,
        @JsonProperty("required_confirmations") Integer requiredConfirmations,
        @JsonProperty("txid") String txid,
        @JsonProperty("tx_list") List<PaymentTx> txList,
        @JsonProperty("paid_at") String paidAt,
        @JsonProperty("payer_address") String payerAddress,
        @JsonProperty("payer_address_is_refundable") Boolean payerAddressIsRefundable,
        @JsonProperty("payer_email") String payerEmail,
        @JsonProperty("additional_data") String additionalData,
        @JsonProperty("commission") String commission,
        @JsonProperty("merchant_amount") String merchantAmount,
        @JsonProperty("document_url") String documentUrl,
        @JsonProperty("is_test") Boolean isTest,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("refunds") List<PaymentRefund> refunds,
        @JsonProperty("refund_status") String refundStatus) {}
