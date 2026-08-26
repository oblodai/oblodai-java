package com.oblodai.resources.async;

import com.fasterxml.jackson.databind.JavaType;
import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayoutApproveRequest;
import com.oblodai.contract.requests.PayoutBatchRequest;
import com.oblodai.contract.requests.PayoutCancelRequest;
import com.oblodai.contract.requests.PayoutHistoryRequest;
import com.oblodai.contract.requests.PayoutInfoRequest;
import com.oblodai.contract.requests.PayoutMassRequest;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchElement;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.Payout;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outgoing transfers to external addresses.
 *
 * <p>Creating a payout is idempotent by {@code order_id} and by {@code Idempotency-Key}, which the
 * SDK generates when you do not. The errors worth handling by name are
 * {@code payout.insufficient_funds} (retryable once the balance is topped up),
 * {@code payout.funds_maturing}, {@code payout.bad_address} and {@code payout.memo_required}.
 *
 * <p>Lookups take the payout's {@code uuid} or your {@code order_id}: pass the uuid as a string, or
 * a request object carrying whichever you have. Refunds are payouts too, marked {@code is_refund}.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Payouts}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Payouts extends PayoutsPreflightRoutes {

    /**
     * @param transport the engine to call through
     */
    public Payouts(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payout} — creates and, for API keys, auto-approves a payout.
     *
     * @param request the payout to send
     * @return a future of the payout, in the status it was accepted in
     */
    public CompletableFuture<Payout> create(PayoutRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout} — creates and, for API keys, auto-approves a payout.
     *
     * <p>Errors worth branching on: {@code payout.insufficient_funds} — the balance is short, so
     * retry after a top-up; {@code payout.funds_maturing} — the funds have not matured yet;
     * {@code payout.bad_address} — the destination address is malformed;
     * {@code payout.address_network_mismatch} — the address does not belong to the chosen network;
     * {@code payout.memo_required} — the network needs a memo or tag and none was given;
     * {@code payout.amount_below_fee} — the amount does not cover the network fee.
     *
     * @param request the payout to send
     * @param options per-call options
     * @return a future of the payout
     */
    public CompletableFuture<Payout> create(PayoutRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYOUT, request, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/info} — the payout, by its uuid.
     *
     * @param uuid the payout's uuid
     * @return a future of the payout
     */
    public CompletableFuture<Payout> info(String uuid) {
        return info(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/info}.
     *
     * @param uuid the payout's uuid
     * @param options per-call options
     * @return a future of the payout
     */
    public CompletableFuture<Payout> info(String uuid, RequestOptions options) {
        return info(new PayoutInfoRequest().uuid(uuid), options);
    }

    /**
     * @param lookup which payout to read, by {@code uuid} or by {@code order_id}
     * @return a future of the payout
     */
    public CompletableFuture<Payout> info(PayoutInfoRequest lookup) {
        return info(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to read
     * @param options per-call options
     * @return a future of the payout
     */
    public CompletableFuture<Payout> info(PayoutInfoRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYOUT_INFO, lookup, options, Payout.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param uuid the payout's uuid
     * @return a future of the payout
     */
    public CompletableFuture<Payout> get(String uuid) {
        return get(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/info} — alias of {@link #info(String, RequestOptions)}.
     *
     * @param uuid the payout's uuid
     * @param options per-call options
     * @return a future of the payout
     */
    public CompletableFuture<Payout> get(String uuid, RequestOptions options) {
        return info(uuid, options);
    }

    /**
     * Alias of {@link #info(PayoutInfoRequest)}.
     *
     * @param lookup which payout to read
     * @return a future of the payout
     */
    public CompletableFuture<Payout> get(PayoutInfoRequest lookup) {
        return get(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/info} — alias of {@link #info(PayoutInfoRequest, RequestOptions)}.
     *
     * @param lookup which payout to read
     * @param options per-call options
     * @return a future of the payout
     */
    public CompletableFuture<Payout> get(PayoutInfoRequest lookup, RequestOptions options) {
        return info(lookup, options);
    }

    /**
     * {@code POST /v1/payout/cancel} — cancels a payout not yet broadcast ({@code pending},
     * {@code approved}, {@code awaiting_cosign}); afterwards, 409 {@code payout.not_pending}.
     *
     * @param uuid the payout's uuid
     * @return a future of the cancelled payout
     */
    public CompletableFuture<Payout> cancel(String uuid) {
        return cancel(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/cancel}.
     *
     * @param uuid the payout's uuid
     * @param options per-call options
     * @return a future of the cancelled payout
     */
    public CompletableFuture<Payout> cancel(String uuid, RequestOptions options) {
        return cancel(new PayoutCancelRequest().uuid(uuid), options);
    }

    /**
     * @param lookup which payout to cancel, by {@code uuid}
     * @return a future of the cancelled payout
     */
    public CompletableFuture<Payout> cancel(PayoutCancelRequest lookup) {
        return cancel(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to cancel
     * @param options per-call options
     * @return a future of the cancelled payout
     */
    public CompletableFuture<Payout> cancel(PayoutCancelRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYOUT_CANCEL, lookup, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/approve} — approves a payout waiting for manual approval.
     *
     * @param uuid the payout's uuid
     * @return a future of the approved payout
     */
    public CompletableFuture<Payout> approve(String uuid) {
        return approve(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/approve}.
     *
     * @param uuid the payout's uuid
     * @param options per-call options
     * @return a future of the approved payout
     */
    public CompletableFuture<Payout> approve(String uuid, RequestOptions options) {
        return approve(new PayoutApproveRequest().uuid(uuid), options);
    }

    /**
     * @param lookup which payout to approve, by {@code uuid}
     * @return a future of the approved payout
     */
    public CompletableFuture<Payout> approve(PayoutApproveRequest lookup) {
        return approve(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to approve
     * @param options per-call options
     * @return a future of the approved payout
     */
    public CompletableFuture<Payout> approve(PayoutApproveRequest lookup, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYOUT_APPROVE, lookup, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/history} — newest first; {@code kind: "refund"} lists refunds only.
     *
     * @return a lazy non-blocking pager over the merchant's payouts
     */
    public AsyncPager<Payout> history() {
        return history(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/history}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the merchant's payouts
     */
    public AsyncPager<Payout> history(RequestOptions options) {
        return history(new PayoutHistoryRequest(), options);
    }

    /**
     * @param params filters and page bounds
     * @return a lazy non-blocking pager over the matching payouts
     */
    public AsyncPager<Payout> history(PayoutHistoryRequest params) {
        return history(params, RequestOptions.none());
    }

    /**
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the matching payouts
     */
    public AsyncPager<Payout> history(PayoutHistoryRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_PAYOUT_HISTORY, params, options, Payout.class);
    }

    /**
     * Alias of {@link #history()}.
     *
     * @return a lazy non-blocking pager over the merchant's payouts
     */
    public AsyncPager<Payout> list() {
        return list(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/history} — alias of {@link #history(RequestOptions)}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the merchant's payouts
     */
    public AsyncPager<Payout> list(RequestOptions options) {
        return history(options);
    }

    /**
     * Alias of {@link #history(PayoutHistoryRequest)}.
     *
     * @param params filters and page bounds
     * @return a lazy non-blocking pager over the matching payouts
     */
    public AsyncPager<Payout> list(PayoutHistoryRequest params) {
        return list(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/history} — alias of
     * {@link #history(PayoutHistoryRequest, RequestOptions)}.
     *
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the matching payouts
     */
    public AsyncPager<Payout> list(PayoutHistoryRequest params, RequestOptions options) {
        return history(params, options);
    }

    /**
     * {@code POST /v1/payout/mass} — synchronous batch of at most 100: each element reports its own
     * outcome in the answer, so nothing has to be polled.
     *
     * @param request the payouts to send
     * @return a future of one element per submitted payout, in the order they were sent
     */
    public CompletableFuture<List<BatchElement<Payout>>> mass(PayoutMassRequest request) {
        return mass(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/mass} — synchronous batch of at most 100.
     *
     * <p>Errors worth branching on: {@code payout.empty_batch} — nothing to send;
     * {@code payout.batch_too_large} — over the 100-item cap; {@code payout.insufficient_funds} —
     * the balance cannot cover the batch; {@code payout.reference_collision} — an item reuses a
     * reference already spent; {@code payout.frozen} — payouts are frozen for this merchant. A
     * single item failing does not raise: it reports on its own element.
     *
     * @param request the payouts to send
     * @param options per-call options
     * @return a future of one element per submitted payout
     */
    public CompletableFuture<List<BatchElement<Payout>>> mass(
            PayoutMassRequest request, RequestOptions options) {
        JavaType element = parametric(BatchElement.class, Payout.class);
        return plainListAsync(Routes.POST_V1_PAYOUT_MASS, request, options, element);
    }

    /**
     * {@code POST /v1/payout/batch} — asynchronous batch of at most 5000: returns a ticket, poll
     * {@code batches().info(...)}. {@code order_id} is required on every item.
     *
     * @param request the payouts to submit
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(PayoutBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/batch} — asynchronous batch of at most 5000.
     *
     * <p>Errors worth branching on: {@code batch.empty} — nothing to submit;
     * {@code batch.too_large} — over the 5000-item cap; {@code batch.order_id_required} — an item
     * carries no {@code order_id}; {@code batch.duplicate_order_id} — two items share one
     * {@code order_id}; {@code batch.disabled} — batches are switched off for this merchant.
     *
     * @param request the payouts to submit
     * @param options per-call options
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(
            PayoutBatchRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYOUT_BATCH, request, options, BatchSubmitted.class);
    }
}
