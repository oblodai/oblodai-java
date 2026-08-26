package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.LinkIdCheckoutRequest;
import com.oblodai.contract.requests.PaymentLinkInfoRequest;
import com.oblodai.contract.requests.PaymentLinkListRequest;
import com.oblodai.contract.requests.PaymentLinkRequest;
import com.oblodai.contract.requests.PaymentLinkToggleRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.PaymentLink;
import com.oblodai.models.PaymentLinkCreated;
import com.oblodai.models.PaymentLinkToggled;
import com.oblodai.models.PublicPayment;
import com.oblodai.models.PublicPaymentLink;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Reusable payment links (tip jars, price tags): each checkout spawns an invoice.
 *
 * <p>A link is not an invoice: it is a template that stays open, and every payer who checks out
 * gets their own invoice from it.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.PaymentLinks}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class PaymentLinks extends Resource {

    /**
     * @param transport the engine to call through
     */
    public PaymentLinks(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment/link} — creates a reusable link.
     *
     * @param request the link to create
     * @return a future of the link, with the URL to hand to payers
     */
    public CompletableFuture<PaymentLinkCreated> create(PaymentLinkRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link}.
     *
     * <p>Errors worth branching on: {@code paylink.bad_mode} — the mode is not one the gateway
     * knows; {@code paylink.bad_amount} — the fixed amount is not a valid price;
     * {@code paylink.bad_range} — the minimum and maximum do not make a range;
     * {@code paylink.order_id_too_long} — the {@code order_id} is over the length cap;
     * {@code paylink.expires_in_too_large} — the lifetime is beyond the cap.
     *
     * @param request the link to create
     * @param options per-call options
     * @return a future of the link, with the URL to hand to payers
     */
    public CompletableFuture<PaymentLinkCreated> create(
            PaymentLinkRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_LINK, request, options, PaymentLinkCreated.class);
    }

    /**
     * {@code POST /v1/payment/link/info} — the link plus a page of the invoices it spawned
     * ({@code payments}).
     *
     * @param linkId the link's id
     * @return a future of the link and its first page of invoices
     */
    public CompletableFuture<PaymentLink> info(String linkId) {
        return info(linkId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info}.
     *
     * @param linkId the link's id
     * @param options per-call options
     * @return a future of the link and its first page of invoices
     */
    public CompletableFuture<PaymentLink> info(String linkId, RequestOptions options) {
        return info(new PaymentLinkInfoRequest().linkId(linkId), options);
    }

    /**
     * {@code POST /v1/payment/link/info} — the page bounds on the request choose which slice of the
     * spawned invoices comes back with the link.
     *
     * @param request which link to read, and which page of its invoices
     * @return a future of the link and the requested page of invoices
     */
    public CompletableFuture<PaymentLink> info(PaymentLinkInfoRequest request) {
        return info(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info}.
     *
     * @param request which link to read, and which page of its invoices
     * @param options per-call options
     * @return a future of the link and the requested page of invoices
     */
    public CompletableFuture<PaymentLink> info(
            PaymentLinkInfoRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_LINK_INFO, request, options, PaymentLink.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param linkId the link's id
     * @return a future of the link and its first page of invoices
     */
    public CompletableFuture<PaymentLink> get(String linkId) {
        return get(linkId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info} — alias of {@link #info(String, RequestOptions)}.
     *
     * @param linkId the link's id
     * @param options per-call options
     * @return a future of the link and its first page of invoices
     */
    public CompletableFuture<PaymentLink> get(String linkId, RequestOptions options) {
        return info(linkId, options);
    }

    /**
     * Alias of {@link #info(PaymentLinkInfoRequest)}.
     *
     * @param request which link to read, and which page of its invoices
     * @return a future of the link and the requested page of invoices
     */
    public CompletableFuture<PaymentLink> get(PaymentLinkInfoRequest request) {
        return get(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info} — alias of
     * {@link #info(PaymentLinkInfoRequest, RequestOptions)}.
     *
     * @param request which link to read, and which page of its invoices
     * @param options per-call options
     * @return a future of the link and the requested page of invoices
     */
    public CompletableFuture<PaymentLink> get(
            PaymentLinkInfoRequest request, RequestOptions options) {
        return info(request, options);
    }

    /**
     * {@code POST /v1/payment/link/list} — the merchant's links, newest first.
     *
     * @return a lazy non-blocking pager over the merchant's links
     */
    public AsyncPager<PaymentLink> list() {
        return list(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/list}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the merchant's links
     */
    public AsyncPager<PaymentLink> list(RequestOptions options) {
        return list(new PaymentLinkListRequest(), options);
    }

    /**
     * {@code POST /v1/payment/link/list}.
     *
     * @param params page bounds
     * @return a lazy non-blocking pager over the merchant's links
     */
    public AsyncPager<PaymentLink> list(PaymentLinkListRequest params) {
        return list(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/list}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the merchant's links
     */
    public AsyncPager<PaymentLink> list(PaymentLinkListRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_PAYMENT_LINK_LIST, params, options, PaymentLink.class);
    }

    /**
     * {@code POST /v1/payment/link/toggle} — enable or disable a link. Invoices already spawned
     * from it stay payable; a disabled link simply stops spawning new ones.
     *
     * @param linkId the link's id
     * @param active true to enable the link, false to disable it
     * @return a future of the link's new state
     */
    public CompletableFuture<PaymentLinkToggled> toggle(String linkId, boolean active) {
        return toggle(linkId, active, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/toggle}.
     *
     * @param linkId the link's id
     * @param active true to enable the link, false to disable it
     * @param options per-call options
     * @return a future of the link's new state
     */
    public CompletableFuture<PaymentLinkToggled> toggle(
            String linkId, boolean active, RequestOptions options) {
        return toggle(new PaymentLinkToggleRequest().linkId(linkId).active(active), options);
    }

    /**
     * {@code POST /v1/payment/link/toggle} — enable or disable a link.
     *
     * @param request which link, and the state to put it in
     * @return a future of the link's new state
     */
    public CompletableFuture<PaymentLinkToggled> toggle(PaymentLinkToggleRequest request) {
        return toggle(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/toggle}.
     *
     * @param request which link, and the state to put it in
     * @param options per-call options
     * @return a future of the link's new state
     */
    public CompletableFuture<PaymentLinkToggled> toggle(
            PaymentLinkToggleRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_LINK_TOGGLE, request, options, PaymentLinkToggled.class);
    }

    // --- payer-facing (public, unsigned) — for custom checkout pages ----------------------------

    /**
     * {@code GET /v1/link/{id}} — the link as the payer sees it. No credentials needed.
     *
     * @param linkId the link's id
     * @return a future of the payer-facing view
     */
    public CompletableFuture<PublicPaymentLink> publicView(String linkId) {
        return publicView(linkId, RequestOptions.none());
    }

    /**
     * {@code GET /v1/link/{id}}.
     *
     * @param linkId the link's id
     * @param options per-call options
     * @return a future of the payer-facing view
     */
    public CompletableFuture<PublicPaymentLink> publicView(String linkId, RequestOptions options) {
        return callAsync(
                Routes.GET_V1_LINK_ID,
                options(options).pathParam("id", linkId),
                PublicPaymentLink.class);
    }

    /**
     * {@code POST /v1/link/{id}/checkout} — spawn an invoice from the link. Rate-capped per IP, and
     * no credentials needed.
     *
     * @param linkId the link's id
     * @return a future of the invoice, as the payer sees it
     */
    public CompletableFuture<PublicPayment> checkout(String linkId) {
        return checkout(linkId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/link/{id}/checkout}.
     *
     * @param linkId the link's id
     * @param options per-call options
     * @return a future of the invoice, as the payer sees it
     */
    public CompletableFuture<PublicPayment> checkout(String linkId, RequestOptions options) {
        return checkout(linkId, new LinkIdCheckoutRequest(), options);
    }

    /**
     * {@code POST /v1/link/{id}/checkout}.
     *
     * @param linkId the link's id
     * @param request what the payer chose: amount, asset, network, contact
     * @return a future of the invoice, as the payer sees it
     */
    public CompletableFuture<PublicPayment> checkout(String linkId, LinkIdCheckoutRequest request) {
        return checkout(linkId, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/link/{id}/checkout}.
     *
     * @param linkId the link's id
     * @param request what the payer chose: amount, asset, network, contact
     * @param options per-call options
     * @return a future of the invoice, as the payer sees it
     */
    public CompletableFuture<PublicPayment> checkout(
            String linkId, LinkIdCheckoutRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_LINK_ID_CHECKOUT,
                options(options).pathParam("id", linkId).body(request),
                PublicPayment.class);
    }
}
