package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PaymentQrRequest;
import com.oblodai.contract.requests.PaymentResendRequest;
import com.oblodai.contract.requests.PaymentSendEmailRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.EmailSent;
import com.oblodai.models.OkResult;
import com.oblodai.models.QrCode;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * The rendering and re-delivery routes of {@link Payments}: {@code POST /v1/payment/qr},
 * {@code POST /v1/payment/send-email} and {@code POST /v1/payment/resend} — the routes that put
 * an invoice in front of someone rather than change it.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Payments} and exists only to keep source files small. Reach every method here through
 * {@code payments()}.
 */
public abstract sealed class PaymentsNotifyRoutes extends Resource permits PaymentsCheckoutRoutes {

    /**
     * @param transport the engine to call through
     */
    protected PaymentsNotifyRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment/qr} — QR image of the invoice's payment URI.
     *
     * @param uuid the invoice's uuid
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> qr(String uuid) {
        return qr(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/qr}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<QrCode> qr(String uuid, RequestOptions options) {
        return qr(new PaymentQrRequest().uuid(uuid), options);
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
        return resend(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/resend}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return a future of whether the delivery was queued
     */
    public CompletableFuture<OkResult> resend(String uuid, RequestOptions options) {
        return resend(new PaymentResendRequest().uuid(uuid), options);
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
}
