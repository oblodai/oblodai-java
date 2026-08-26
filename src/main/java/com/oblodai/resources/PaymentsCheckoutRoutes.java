package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayIdSelectRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.PublicPayment;
import com.oblodai.models.QrCode;

/**
 * The payer-facing routes of {@link Payments}: {@code GET /v1/pay/{id}},
 * {@code POST /v1/pay/{id}/select} and {@code GET /v1/pay/{id}/qr} — the unsigned routes a
 * custom checkout page calls, where no credentials are needed.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Payments} and exists only to keep source files small. Reach every method here through
 * {@code payments()}.
 */
public abstract sealed class PaymentsCheckoutRoutes extends PaymentsNotifyRoutes permits Payments {

    /**
     * @param transport the engine to call through
     */
    protected PaymentsCheckoutRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code GET /v1/pay/{id}} — the invoice as the payer sees it. No credentials needed.
     *
     * @param uuid the invoice's uuid
     * @return the payer-facing view
     */
    public PublicPayment publicView(String uuid) {
        return publicView(uuid, RequestOptions.none());
    }

    /**
     * {@code GET /v1/pay/{id}}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return the payer-facing view
     */
    public PublicPayment publicView(String uuid, RequestOptions options) {
        return call(
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
     * @return the invoice with its deposit address assigned
     */
    public PublicPayment select(String uuid, PayIdSelectRequest request) {
        return select(uuid, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/pay/{id}/select}.
     *
     * @param uuid the invoice's uuid
     * @param request the chosen asset and network
     * @param options per-call options
     * @return the invoice with its deposit address assigned
     */
    public PublicPayment select(String uuid, PayIdSelectRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_PAY_ID_SELECT,
                options(options).pathParam("id", uuid).body(request),
                PublicPayment.class);
    }

    /**
     * {@code GET /v1/pay/{id}/qr} — QR for the payer page. No credentials needed.
     *
     * @param uuid the invoice's uuid
     * @return the QR image and what it encodes
     */
    public QrCode publicQr(String uuid) {
        return publicQr(uuid, RequestOptions.none());
    }

    /**
     * {@code GET /v1/pay/{id}/qr}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return the QR image and what it encodes
     */
    public QrCode publicQr(String uuid, RequestOptions options) {
        return call(Routes.GET_V1_PAY_ID_QR, options(options).pathParam("id", uuid), QrCode.class);
    }
}
