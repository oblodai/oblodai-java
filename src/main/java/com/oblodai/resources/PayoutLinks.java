package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.ClaimTokenRequest;
import com.oblodai.contract.requests.PayoutLinkBatchRequest;
import com.oblodai.contract.requests.PayoutLinkCancelRequest;
import com.oblodai.contract.requests.PayoutLinkChequeRequest;
import com.oblodai.contract.requests.PayoutLinkInfoRequest;
import com.oblodai.contract.requests.PayoutLinkListRequest;
import com.oblodai.contract.requests.PayoutLinkRequest;
import com.oblodai.core.FileResult;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchElement;
import com.oblodai.models.ClaimPreview;
import com.oblodai.models.ClaimResult;
import com.oblodai.models.PayoutLink;
import java.util.List;

/**
 * Payout links (cheques): funds reserved now, claimed later by whoever holds the token. Payout key.
 *
 * <p>The money leaves the balance when the link is created, not when it is claimed, so an unclaimed
 * link keeps holding it until you cancel.
 */
public final class PayoutLinks extends Resource {

    /**
     * @param transport the engine to call through
     */
    public PayoutLinks(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payout/link} — reserve funds and mint a claim token. {@code claim_token} and
     * {@code claim_url} are returned once, at creation, and never again. Idempotent by
     * {@code reference}.
     *
     * @param request the link to create
     * @return the link, with its claim token
     */
    public PayoutLink create(PayoutLinkRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link} — reserve funds and mint a claim token.
     *
     * @param request the link to create
     * @param options per-call options
     * @return the link, with its claim token
     */
    public PayoutLink create(PayoutLinkRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_LINK, request, options, PayoutLink.class);
    }

    /**
     * {@code POST /v1/payout/link/info} — the link's state and, once claimed, where it went.
     *
     * @param linkId the link's id
     * @return the link
     */
    public PayoutLink info(String linkId) {
        return info(new PayoutLinkInfoRequest().linkId(linkId), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/info}.
     *
     * @param request which link to read
     * @return the link
     */
    public PayoutLink info(PayoutLinkInfoRequest request) {
        return info(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/info}.
     *
     * @param request which link to read
     * @param options per-call options
     * @return the link
     */
    public PayoutLink info(PayoutLinkInfoRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_LINK_INFO, request, options, PayoutLink.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param linkId the link's id
     * @return the link
     */
    public PayoutLink get(String linkId) {
        return info(linkId);
    }

    /**
     * Alias of {@link #info(PayoutLinkInfoRequest)}.
     *
     * @param request which link to read
     * @return the link
     */
    public PayoutLink get(PayoutLinkInfoRequest request) {
        return info(request);
    }

    /**
     * {@code POST /v1/payout/link/list} — the merchant's payout links, newest first.
     *
     * @return a lazy pager over the merchant's payout links
     */
    public Pager<PayoutLink> list() {
        return list(new PayoutLinkListRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/list}.
     *
     * @param params page bounds
     * @return a lazy pager over the merchant's payout links
     */
    public Pager<PayoutLink> list(PayoutLinkListRequest params) {
        return list(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/list}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the merchant's payout links
     */
    public Pager<PayoutLink> list(PayoutLinkListRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYOUT_LINK_LIST, params, options, PayoutLink.class);
    }

    /**
     * {@code POST /v1/payout/link/cancel} — release the reserved funds of an unclaimed link. A link
     * that has already been claimed cannot be cancelled.
     *
     * @param linkId the link's id
     * @return the cancelled link
     */
    public PayoutLink cancel(String linkId) {
        return cancel(new PayoutLinkCancelRequest().linkId(linkId), RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/cancel}.
     *
     * @param request which link to cancel
     * @return the cancelled link
     */
    public PayoutLink cancel(PayoutLinkCancelRequest request) {
        return cancel(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/cancel}.
     *
     * @param request which link to cancel
     * @param options per-call options
     * @return the cancelled link
     */
    public PayoutLink cancel(PayoutLinkCancelRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_LINK_CANCEL, request, options, PayoutLink.class);
    }

    /**
     * {@code POST /v1/payout/link/batch} — synchronous: many links in one signed call, each element
     * reporting its own outcome. {@code reference} is required on every item.
     *
     * @param request the links to create
     * @return one element per submitted link, in the order they were sent
     */
    public List<BatchElement<PayoutLink>> batch(PayoutLinkBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/batch} — synchronous batch of payout links.
     *
     * @param request the links to create
     * @param options per-call options
     * @return one element per submitted link, in the order they were sent
     */
    public List<BatchElement<PayoutLink>> batch(
            PayoutLinkBatchRequest request, RequestOptions options) {
        return plainList(
                Routes.POST_V1_PAYOUT_LINK_BATCH,
                request,
                options,
                parametric(BatchElement.class, PayoutLink.class));
    }

    /**
     * {@code POST /v1/payout/link/cheque} — printable PDF cheque for a claim token.
     *
     * @param request which link to render, and in which language
     * @return the PDF bytes, with their content type and filename
     */
    public FileResult cheque(PayoutLinkChequeRequest request) {
        return cheque(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/link/cheque}.
     *
     * @param request which link to render, and in which language
     * @param options per-call options
     * @return the PDF bytes, with their content type and filename
     */
    public FileResult cheque(PayoutLinkChequeRequest request, RequestOptions options) {
        return file(Routes.POST_V1_PAYOUT_LINK_CHEQUE, options(options).body(request));
    }

    // --- recipient side (public, unsigned) ------------------------------------------------------

    /**
     * {@code GET /v1/claim/{token}} — what the recipient sees before claiming. No credentials
     * needed.
     *
     * @param token the claim token from the link's {@code claim_url}
     * @return the recipient-facing view
     */
    public ClaimPreview claimPreview(String token) {
        return claimPreview(token, RequestOptions.none());
    }

    /**
     * {@code GET /v1/claim/{token}}.
     *
     * @param token the claim token from the link's {@code claim_url}
     * @param options per-call options
     * @return the recipient-facing view
     */
    public ClaimPreview claimPreview(String token, RequestOptions options) {
        return call(
                Routes.GET_V1_CLAIM_TOKEN,
                options(options).pathParam("token", token),
                ClaimPreview.class);
    }

    /**
     * {@code POST /v1/claim/{token}} — claim to an address, with the passcode when the link has
     * one. No credentials needed.
     *
     * @param token the claim token
     * @param request the destination address, and the passcode when one is set
     * @return the claim's outcome, with the payout it started
     */
    public ClaimResult claim(String token, ClaimTokenRequest request) {
        return claim(token, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/claim/{token}}.
     *
     * @param token the claim token
     * @param request the destination address, and the passcode when one is set
     * @param options per-call options
     * @return the claim's outcome, with the payout it started
     */
    public ClaimResult claim(String token, ClaimTokenRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_CLAIM_TOKEN,
                options(options).pathParam("token", token).body(request),
                ClaimResult.class);
    }
}
