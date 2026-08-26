package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PaymentRefundRequest;
import com.oblodai.contract.requests.PaymentResolveRequest;
import com.oblodai.contract.requests.RefundBatchRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.Payout;
import com.oblodai.models.Resolution;

/**
 * Refunds are payouts in the invoice's own asset; underpayments are resolved (accept or refund).
 *
 * <p>A refund leaves the platform, so it is a money-out route: it is signed with the merchant's
 * API key, exactly like the invoice it refunds.
 */
public final class Refunds extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Refunds(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment/refund} — refund a paid invoice, fully or partially.
     *
     * @param request which invoice to refund, and how much
     * @return the refund, as the payout it is
     */
    public Payout create(PaymentRefundRequest request) {
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
     * @return the refund, as the payout it is
     */
    public Payout create(PaymentRefundRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_REFUND, request, options, Payout.class);
    }

    /**
     * {@code POST /v1/payment/resolve} — settle an underpaid ({@code wrong_amount}) invoice, by
     * accepting what arrived or by sending it back.
     *
     * @param request which invoice, and which way to settle it
     * @return what the gateway did: an acceptance or a refund
     */
    public Resolution resolve(PaymentResolveRequest request) {
        return resolve(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/resolve} — settle an underpaid ({@code wrong_amount}) invoice.
     *
     * @param request which invoice, and which way to settle it
     * @param options per-call options
     * @return what the gateway did: an acceptance or a refund
     */
    public Resolution resolve(PaymentResolveRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_RESOLVE, request, options, Resolution.class);
    }

    /**
     * {@code POST /v1/refund/batch} — up to 5000 refunds; track with {@code batches().info(...)}.
     *
     * @param request the refunds to submit
     * @return the batch ticket
     */
    public BatchSubmitted batch(RefundBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/refund/batch} — up to 5000 refunds.
     *
     * @param request the refunds to submit
     * @param options per-call options
     * @return the batch ticket
     */
    public BatchSubmitted batch(RefundBatchRequest request, RequestOptions options) {
        return call(Routes.POST_V1_REFUND_BATCH, request, options, BatchSubmitted.class);
    }
}
