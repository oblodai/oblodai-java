package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayIdSelectRequest;
import com.oblodai.contract.requests.PaymentBatchRequest;
import com.oblodai.contract.requests.PaymentCancelRequest;
import com.oblodai.contract.requests.PaymentHistoryRequest;
import com.oblodai.contract.requests.PaymentInfoRequest;
import com.oblodai.contract.requests.PaymentQrRequest;
import com.oblodai.contract.requests.PaymentResendRequest;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.contract.requests.PaymentSendEmailRequest;
import com.oblodai.contract.requests.PaymentServicesRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.EmailSent;
import com.oblodai.models.OkResult;
import com.oblodai.models.Payment;
import com.oblodai.models.PublicPayment;
import com.oblodai.models.QrCode;
import com.oblodai.models.ServiceMethod;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Invoices: create, look up, cancel, list, and the payer-facing checkout endpoints. Payment key.
 *
 * <p>Lookups take either the invoice's {@code uuid} or your {@code order_id}: pass the uuid as a
 * string, or a request object carrying whichever you have.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Payments}: the same methods,
 * returning {@link CompletableFuture} and {@link AsyncPager}.
 */
public final class Payments extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Payments(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment} — creates an invoice. Idempotent by {@code order_id} and by
     * {@code Idempotency-Key}, which the SDK generates when you do not.
     *
     * @param request the invoice to create
     * @return a future of the invoice, in status {@code created} (or {@code select} when no network
     *     was pinned)
     */
    public CompletableFuture<Payment> create(PaymentRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment} — creates an invoice.
     *
     * @param request the invoice to create
     * @param options per-call options
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> create(PaymentRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT, request, options, Payment.class);
    }

    /**
     * {@code POST /v1/payment/info} — the invoice, including its refunds and refund status.
     *
     * @param uuid the invoice's uuid
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> info(String uuid) {
        return info(new PaymentInfoRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info} — by {@code uuid} or by {@code order_id}.
     *
     * @param lookup which invoice to read
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> info(PaymentInfoRequest lookup) {
        return info(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info}.
     *
     * @param lookup which invoice to read
     * @param options per-call options
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> info(PaymentInfoRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_INFO, lookup, options, Payment.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param uuid the invoice's uuid
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> get(String uuid) {
        return info(uuid);
    }

    /**
     * Alias of {@link #info(PaymentInfoRequest)}.
     *
     * @param lookup which invoice to read
     * @return a future of the invoice
     */
    public CompletableFuture<Payment> get(PaymentInfoRequest lookup) {
        return info(lookup);
    }

    /**
     * {@code POST /v1/payment/cancel} — cancels an unpaid invoice. Once a deposit has been seen the
     * gateway answers 409 {@code invoice.not_payable}.
     *
     * @param uuid the invoice's uuid
     * @return a future of the cancelled invoice
     */
    public CompletableFuture<Payment> cancel(String uuid) {
        return cancel(new PaymentCancelRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/cancel}.
     *
     * @param lookup which invoice to cancel
     * @return a future of the cancelled invoice
     */
    public CompletableFuture<Payment> cancel(PaymentCancelRequest lookup) {
        return cancel(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/cancel}.
     *
     * @param lookup which invoice to cancel
     * @param options per-call options
     * @return a future of the cancelled invoice
     */
    public CompletableFuture<Payment> cancel(PaymentCancelRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_CANCEL, lookup, options, Payment.class);
    }

    /**
     * {@code POST /v1/payment/history} — newest first. Read one page, iterate, or stream.
     *
     * @return a lazy non-blocking pager over the merchant's invoices
     */
    public AsyncPager<Payment> history() {
        return history(new PaymentHistoryRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history}.
     *
     * @param params filters and page bounds
     * @return a lazy non-blocking pager over the matching invoices
     */
    public AsyncPager<Payment> history(PaymentHistoryRequest params) {
        return history(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history}.
     *
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the matching invoices
     */
    public AsyncPager<Payment> history(PaymentHistoryRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_PAYMENT_HISTORY, params, options, Payment.class);
    }

    /**
     * Alias of {@link #history()}.
     *
     * @return a lazy non-blocking pager over the merchant's invoices
     */
    public AsyncPager<Payment> list() {
        return history();
    }

    /**
     * Alias of {@link #history(PaymentHistoryRequest)}.
     *
     * @param params filters and page bounds
     * @return a lazy non-blocking pager over the matching invoices
     */
    public AsyncPager<Payment> list(PaymentHistoryRequest params) {
        return history(params);
    }

    /**
     * {@code POST /v1/payment/batch} — creates up to 5000 invoices asynchronously; follow it with
     * {@code batches().info(...)}.
     *
     * @param request the invoices to create
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(PaymentBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/batch}.
     *
     * @param request the invoices to create
     * @param options per-call options
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(
            PaymentBatchRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_BATCH, request, options, BatchSubmitted.class);
    }

    /**
     * {@code POST /v1/payment/qr} — QR image of the invoice's payment URI.
     *
     * @param uuid the invoice's uuid
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> qr(String uuid) {
        return qr(new PaymentQrRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/qr}.
     *
     * @param lookup which invoice to render
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> qr(PaymentQrRequest lookup) {
        return qr(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/qr}.
     *
     * @param lookup which invoice to render
     * @param options per-call options
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> qr(PaymentQrRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_QR, lookup, options, QrCode.class);
    }

    /**
     * {@code POST /v1/payment/services} — the currency and network pairs deposits are accepted in,
     * with their limits and fees.
     *
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<ServiceMethod> services() {
        return services(new PaymentServicesRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/services}.
     *
     * @param params page bounds
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<ServiceMethod> services(PaymentServicesRequest params) {
        return services(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/services}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<ServiceMethod> services(
            PaymentServicesRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_PAYMENT_SERVICES, params, options, ServiceMethod.class);
    }

    /**
     * {@code POST /v1/payment/send-email} — emails the receipt, by default to the invoice's
     * {@code payer_email}.
     *
     * @param request which invoice, and optionally which address
     * @return a future of what was sent, and to whom
     */
    public CompletableFuture<EmailSent> sendEmail(PaymentSendEmailRequest request) {
        return sendEmail(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/send-email}.
     *
     * @param request which invoice, and optionally which address
     * @param options per-call options
     * @return a future of what was sent, and to whom
     */
    public CompletableFuture<EmailSent> sendEmail(
            PaymentSendEmailRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_SEND_EMAIL, request, options, EmailSent.class);
    }

    /**
     * {@code POST /v1/payment/resend} — re-delivers the invoice's last webhook.
     *
     * @param uuid the invoice's uuid
     * @return a future of whether the delivery was queued
     */
    public CompletableFuture<OkResult> resend(String uuid) {
        return resend(new PaymentResendRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/resend}.
     *
     * @param lookup which invoice to re-deliver
     * @return a future of whether the delivery was queued
     */
    public CompletableFuture<OkResult> resend(PaymentResendRequest lookup) {
        return resend(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/resend}.
     *
     * @param lookup which invoice to re-deliver
     * @param options per-call options
     * @return a future of whether the delivery was queued
     */
    public CompletableFuture<OkResult> resend(PaymentResendRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_RESEND, lookup, options, OkResult.class);
    }

    // --- payer-facing (public, unsigned) — for custom checkout pages ----------------------------

    /**
     * {@code GET /v1/pay/{id}} — the invoice as the payer sees it. No credentials needed.
     *
     * @param uuid the invoice's uuid
     * @return a future of the payer-facing view
     */
    public CompletableFuture<PublicPayment> publicView(String uuid) {
        return publicView(uuid, RequestOptions.none());
    }

    /**
     * {@code GET /v1/pay/{id}}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return a future of the payer-facing view
     */
    public CompletableFuture<PublicPayment> publicView(String uuid, RequestOptions options) {
        return callAsync(
                Routes.GET_V1_PAY_ID,
                options(options).pathParam("id", uuid),
                PublicPayment.class);
    }

    /**
     * {@code POST /v1/pay/{id}/select} — picks the asset and network on a multi-currency invoice.
     * No credentials needed.
     *
     * @param uuid the invoice's uuid
     * @param request the chosen asset and network
     * @return a future of the invoice with its deposit address assigned
     */
    public CompletableFuture<PublicPayment> select(String uuid, PayIdSelectRequest request) {
        return select(uuid, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/pay/{id}/select}.
     *
     * @param uuid the invoice's uuid
     * @param request the chosen asset and network
     * @param options per-call options
     * @return a future of the invoice with its deposit address assigned
     */
    public CompletableFuture<PublicPayment> select(
            String uuid, PayIdSelectRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAY_ID_SELECT,
                options(options).pathParam("id", uuid).body(request),
                PublicPayment.class);
    }

    /**
     * {@code GET /v1/pay/{id}/qr} — QR for the payer page. No credentials needed.
     *
     * @param uuid the invoice's uuid
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> publicQr(String uuid) {
        return publicQr(uuid, RequestOptions.none());
    }

    /**
     * {@code GET /v1/pay/{id}/qr}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> publicQr(String uuid, RequestOptions options) {
        return callAsync(
                Routes.GET_V1_PAY_ID_QR, options(options).pathParam("id", uuid), QrCode.class);
    }
}
