package com.oblodai.kotlin

import com.oblodai.contract.requests.PayIdSelectRequest
import com.oblodai.contract.requests.PaymentBatchRequest
import com.oblodai.contract.requests.PaymentLinkRequest
import com.oblodai.contract.requests.PaymentRefundRequest
import com.oblodai.contract.requests.PaymentRequest
import com.oblodai.contract.requests.PayoutLinkRequest
import com.oblodai.contract.requests.PayoutRequest
import com.oblodai.contract.requests.TransferToUserRequest
import com.oblodai.contract.requests.WalletRequest
import com.oblodai.models.WebhookEvent
import com.oblodai.webhooks.WebhookHeaders
import com.oblodai.webhooks.WebhookVerifier

/**
 * Builders for the generated request types. The generated setters are fluent, so a trailing lambda
 * over the request object reads as a DSL without a second set of classes to maintain:
 *
 * ```kotlin
 * val invoice = oblodai.payments().create(payment {
 *     amount("25")
 *     currency("USDT")
 *     network(Network.TRON)
 *     orderId("order-1001")
 *     urlCallback("https://shop.example/oblodai/webhook")
 * })
 * ```
 */

/** Builds the body of `POST /v1/payment`. */
public inline fun payment(block: PaymentRequest.() -> Unit): PaymentRequest =
    PaymentRequest().apply(block)

/** Builds the body of `POST /v1/payment/batch`. */
public inline fun paymentBatch(block: PaymentBatchRequest.() -> Unit): PaymentBatchRequest =
    PaymentBatchRequest().apply(block)

/** Builds one element of a payment batch. */
public inline fun paymentBatchItem(
    block: PaymentBatchRequest.Payment.() -> Unit
): PaymentBatchRequest.Payment = PaymentBatchRequest.Payment().apply(block)

/** Builds the body of `POST /v1/pay/{id}/select`, the payer's choice of asset and network. */
public inline fun paymentMethod(block: PayIdSelectRequest.() -> Unit): PayIdSelectRequest =
    PayIdSelectRequest().apply(block)

/** Builds the body of `POST /v1/payment/link`. */
public inline fun paymentLink(block: PaymentLinkRequest.() -> Unit): PaymentLinkRequest =
    PaymentLinkRequest().apply(block)

/** Builds the body of `POST /v1/payout`. */
public inline fun payout(block: PayoutRequest.() -> Unit): PayoutRequest =
    PayoutRequest().apply(block)

/** Builds the body of `POST /v1/payout/link` — a cheque the recipient claims. */
public inline fun payoutLink(block: PayoutLinkRequest.() -> Unit): PayoutLinkRequest =
    PayoutLinkRequest().apply(block)

/** Builds the body of `POST /v1/payment/refund`. */
public inline fun refund(block: PaymentRefundRequest.() -> Unit): PaymentRefundRequest =
    PaymentRefundRequest().apply(block)

/** Builds the body of `POST /v1/transfer/to-user`. */
public inline fun transferToUser(block: TransferToUserRequest.() -> Unit): TransferToUserRequest =
    TransferToUserRequest().apply(block)

/** Builds the body of `POST /v1/wallet` — a static deposit address. */
public inline fun wallet(block: WalletRequest.() -> Unit): WalletRequest =
    WalletRequest().apply(block)

/**
 * Verifies a webhook delivery whose headers arrived as a plain map — the shape most Kotlin web
 * frameworks hand you. Verify over the RAW bytes; a re-serialized body will not match.
 *
 * @param headers the request headers, matched case-insensitively
 * @param secret the endpoint secret
 * @param previousSecret the outgoing secret during a rotation overlap
 */
public fun ByteArray.verifyWebhook(
    headers: Map<String, String>,
    secret: String,
    previousSecret: String? = null,
): WebhookEvent {
    var options = WebhookVerifier.options(secret)
    if (previousSecret != null) options = options.previousSecret(previousSecret)
    return WebhookVerifier.verify(this, WebhookHeaders.of(headers), options)
}
