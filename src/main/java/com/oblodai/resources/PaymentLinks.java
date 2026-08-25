package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.LinkIdCheckoutRequest;
import com.oblodai.contract.requests.PaymentLinkInfoRequest;
import com.oblodai.contract.requests.PaymentLinkListRequest;
import com.oblodai.contract.requests.PaymentLinkRequest;
import com.oblodai.contract.requests.PaymentLinkToggleRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.PaymentLink;
import com.oblodai.models.PaymentLinkCreated;
import com.oblodai.models.PaymentLinkToggled;
import com.oblodai.models.PublicPayment;
import com.oblodai.models.PublicPaymentLink;

/**
 * Reusable payment links (tip jars, price tags): each checkout spawns an invoice. Payment key.
 *
 * <p>A link is not an invoice: it is a template that stays open, and every payer who checks out
 * gets their own invoice from it.
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
     * @return the link, with the URL to hand to payers
     */
    public PaymentLinkCreated create(PaymentLinkRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link}.
     *
     * @param request the link to create
     * @param options per-call options
     * @return the link, with the URL to hand to payers
     */
    public PaymentLinkCreated create(PaymentLinkRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_LINK, request, options, PaymentLinkCreated.class);
    }

    /**
     * {@code POST /v1/payment/link/info} — the link plus a page of the invoices it spawned
     * ({@code payments}).
     *
     * @param linkId the link's id
     * @return the link and its first page of invoices
     */
    public PaymentLink info(String linkId) {
        return info(new PaymentLinkInfoRequest().linkId(linkId), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info} — the page bounds on the request choose which slice of the
     * spawned invoices comes back with the link.
     *
     * @param request which link to read, and which page of its invoices
     * @return the link and the requested page of invoices
     */
    public PaymentLink info(PaymentLinkInfoRequest request) {
        return info(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/info}.
     *
     * @param request which link to read, and which page of its invoices
     * @param options per-call options
     * @return the link and the requested page of invoices
     */
    public PaymentLink info(PaymentLinkInfoRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_LINK_INFO, request, options, PaymentLink.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param linkId the link's id
     * @return the link and its first page of invoices
     */
    public PaymentLink get(String linkId) {
        return info(linkId);
    }

    /**
     * Alias of {@link #info(PaymentLinkInfoRequest)}.
     *
     * @param request which link to read, and which page of its invoices
     * @return the link and the requested page of invoices
     */
    public PaymentLink get(PaymentLinkInfoRequest request) {
        return info(request);
    }

    /**
     * {@code POST /v1/payment/link/list} — the merchant's links, newest first.
     *
     * @return a lazy pager over the merchant's links
     */
    public Pager<PaymentLink> list() {
        return list(new PaymentLinkListRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/list}.
     *
     * @param params page bounds
     * @return a lazy pager over the merchant's links
     */
    public Pager<PaymentLink> list(PaymentLinkListRequest params) {
        return list(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/list}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the merchant's links
     */
    public Pager<PaymentLink> list(PaymentLinkListRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYMENT_LINK_LIST, params, options, PaymentLink.class);
    }

    /**
     * {@code POST /v1/payment/link/toggle} — enable or disable a link. Invoices already spawned
     * from it stay payable; a disabled link simply stops spawning new ones.
     *
     * @param linkId the link's id
     * @param active true to enable the link, false to disable it
     * @return the link's new state
     */
    public PaymentLinkToggled toggle(String linkId, boolean active) {
        return toggle(
                new PaymentLinkToggleRequest().linkId(linkId).active(active),
                RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/toggle} — enable or disable a link.
     *
     * @param request which link, and the state to put it in
     * @return the link's new state
     */
    public PaymentLinkToggled toggle(PaymentLinkToggleRequest request) {
        return toggle(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/link/toggle}.
     *
     * @param request which link, and the state to put it in
     * @param options per-call options
     * @return the link's new state
     */
    public PaymentLinkToggled toggle(PaymentLinkToggleRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_PAYMENT_LINK_TOGGLE, request, options, PaymentLinkToggled.class);
    }

    // --- payer-facing (public, unsigned) — for custom checkout pages ----------------------------

    /**
     * {@code GET /v1/link/{id}} — the link as the payer sees it. No credentials needed.
     *
     * @param linkId the link's id
     * @return the payer-facing view
     */
    public PublicPaymentLink publicView(String linkId) {
        return publicView(linkId, RequestOptions.none());
    }

    /**
     * {@code GET /v1/link/{id}}.
     *
     * @param linkId the link's id
     * @param options per-call options
     * @return the payer-facing view
     */
    public PublicPaymentLink publicView(String linkId, RequestOptions options) {
        return call(
                Routes.GET_V1_LINK_ID,
                options(options).pathParam("id", linkId),
                PublicPaymentLink.class);
    }

    /**
     * {@code POST /v1/link/{id}/checkout} — spawn an invoice from the link. Rate-capped per IP, and
     * no credentials needed.
     *
     * @param linkId the link's id
     * @return the invoice, as the payer sees it
     */
    public PublicPayment checkout(String linkId) {
        return checkout(linkId, new LinkIdCheckoutRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/link/{id}/checkout}.
     *
     * @param linkId the link's id
     * @param request what the payer chose: amount, asset, network, contact
     * @return the invoice, as the payer sees it
     */
    public PublicPayment checkout(String linkId, LinkIdCheckoutRequest request) {
        return checkout(linkId, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/link/{id}/checkout}.
     *
     * @param linkId the link's id
     * @param request what the payer chose: amount, asset, network, contact
     * @param options per-call options
     * @return the invoice, as the payer sees it
     */
    public PublicPayment checkout(
            String linkId, LinkIdCheckoutRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_LINK_ID_CHECKOUT,
                options(options).pathParam("id", linkId).body(request),
                PublicPayment.class);
    }
}
