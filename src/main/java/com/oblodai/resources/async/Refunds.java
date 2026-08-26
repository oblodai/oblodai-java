package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PaymentRefundRequest;
import com.oblodai.contract.requests.PaymentResolveRequest;
import com.oblodai.contract.requests.RefundBatchRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.Payout;
import com.oblodai.models.Resolution;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Refunds are payouts in the invoice's own asset; underpayments are resolved (accept or refund).
 *
 * <p>Because a refund leaves the platform, this namespace is gated by the payout key, not the
 * payment key that created the invoice.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Refunds}: the same methods,
 * returning {@link CompletableFuture}.
 */
public final class Refunds extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Refunds(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment/refund} — refund a paid invoice, fully or partially. Requires the
     * payout key.
     *
     * @param request which invoice to refund, and how much
     * @return a future of the refund, as the payout it is
     */
    public CompletableFuture<Payout> create(PaymentRefundRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/refund} — refund a paid invoice, fully or partially.
     *
     * <p>Errors worth branching on: {@code refund.nothing_to_refund} — nothing arrived that could
     * go back; {@code refund.exceeds_refundable} — the amount is more than what is still
     * refundable; {@code refund.no_address} — there is no address to send it to;
     * {@code refund.paid_internally} — the invoice was paid from inside the platform, so nothing
     * goes back on-chain; {@code refund.dust} — the amount is under the network's dust floor;
     * {@code payout.insufficient_funds} — a refund is a payout, and the balance is short.
     *
     * @param request which invoice to refund, and how much
     * @param options per-call options
     * @return a future of the refund, as the payout it is
     */
    public CompletableFuture<Payout> create(PaymentRefundRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_REFUND, request, options, Payout.class);
    }

    /**
     * {@code POST /v1/payment/resolve} — settle an underpaid ({@code wrong_amount}) invoice, by
     * accepting what arrived or by sending it back.
     *
     * @param request which invoice, and which way to settle it
     * @return a future of what the gateway did: an acceptance or a refund
     */
    public CompletableFuture<Resolution> resolve(PaymentResolveRequest request) {
        return resolve(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/resolve} — settle an underpaid ({@code wrong_amount}) invoice.
     *
     * @param request which invoice, and which way to settle it
     * @param options per-call options
     * @return a future of what the gateway did: an acceptance or a refund
     */
    public CompletableFuture<Resolution> resolve(
            PaymentResolveRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_RESOLVE, request, options, Resolution.class);
    }

    /**
     * {@code POST /v1/refund/batch} — up to 5000 refunds; track with {@code batches().info(...)}.
     *
     * @param request the refunds to submit
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(RefundBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/refund/batch} — up to 5000 refunds.
     *
     * @param request the refunds to submit
     * @param options per-call options
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(
            RefundBatchRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_REFUND_BATCH, request, options, BatchSubmitted.class);
    }
}
