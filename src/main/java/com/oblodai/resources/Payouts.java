package com.oblodai.resources;

import com.fasterxml.jackson.databind.JavaType;
import com.oblodai.RequestOptions;
import com.oblodai.contract.RouteSpec;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayoutApproveRequest;
import com.oblodai.contract.requests.PayoutBatchRequest;
import com.oblodai.contract.requests.PayoutCalculateRequest;
import com.oblodai.contract.requests.PayoutCancelRequest;
import com.oblodai.contract.requests.PayoutFeeConfigSetRequest;
import com.oblodai.contract.requests.PayoutHistoryRequest;
import com.oblodai.contract.requests.PayoutInfoRequest;
import com.oblodai.contract.requests.PayoutMassRequest;
import com.oblodai.contract.requests.PayoutRefundFeeConfigSetRequest;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.contract.requests.PayoutServicesRequest;
import com.oblodai.contract.requests.PayoutValidateRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchElement;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.Payout;
import com.oblodai.models.PayoutCalculation;
import com.oblodai.models.PayoutFeeConfig;
import com.oblodai.models.PayoutValidation;
import com.oblodai.models.RefundFeeConfig;
import com.oblodai.models.ServiceMethod;
import java.util.List;

/**
 * Outgoing transfers to external addresses. Every route here needs the payout key.
 *
 * <p>Creating a payout is idempotent by {@code order_id} and by {@code Idempotency-Key}, which the
 * SDK generates when you do not. The errors worth handling by name are
 * {@code payout.insufficient_funds} (retryable once the balance is topped up),
 * {@code payout.funds_maturing}, {@code payout.bad_address} and {@code payout.memo_required}.
 *
 * <p>Lookups take the payout's {@code uuid} or your {@code order_id}: pass the uuid as a string, or
 * a request object carrying whichever you have. Refunds are payouts too, marked {@code is_refund}.
 */
public final class Payouts extends Resource {

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
     * @return the payout, in the status it was accepted in
     */
    public Payout create(PayoutRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * @param request the payout to send
     * @param options per-call options
     * @return the payout
     */
    public Payout create(PayoutRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT, request, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/validate} — dry run: every check {@link #create} makes, with nothing
     * reserved and nothing sent. Fails with the same errors as {@code create}.
     *
     * @param request the payout to check
     * @return the verdict, and what would have happened
     */
    public PayoutValidation validate(PayoutValidateRequest request) {
        return validate(request, RequestOptions.none());
    }

    /**
     * @param request the payout to check
     * @param options per-call options
     * @return the verdict
     */
    public PayoutValidation validate(PayoutValidateRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_VALIDATE, request, options, PayoutValidation.class);
    }

    /**
     * {@code POST /v1/payout/calculate} — commission and net amount, without creating anything.
     *
     * @param request the amount, asset and network to price
     * @return what the payout would cost and what would arrive
     */
    public PayoutCalculation calculate(PayoutCalculateRequest request) {
        return calculate(request, RequestOptions.none());
    }

    /**
     * @param request the amount, asset and network to price
     * @param options per-call options
     * @return the calculation
     */
    public PayoutCalculation calculate(PayoutCalculateRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_CALCULATE, request, options, PayoutCalculation.class);
    }

    /**
     * {@code POST /v1/payout/info} — the payout, by its uuid.
     *
     * @param uuid the payout's uuid
     * @return the payout
     */
    public Payout info(String uuid) {
        return info(new PayoutInfoRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * @param lookup which payout to read, by {@code uuid} or by {@code order_id}
     * @return the payout
     */
    public Payout info(PayoutInfoRequest lookup) {
        return info(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to read
     * @param options per-call options
     * @return the payout
     */
    public Payout info(PayoutInfoRequest lookup, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_INFO, lookup, options, Payout.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param uuid the payout's uuid
     * @return the payout
     */
    public Payout get(String uuid) {
        return info(uuid);
    }

    /**
     * Alias of {@link #info(PayoutInfoRequest)}.
     *
     * @param lookup which payout to read
     * @return the payout
     */
    public Payout get(PayoutInfoRequest lookup) {
        return info(lookup);
    }

    /**
     * {@code POST /v1/payout/cancel} — cancels a payout not yet broadcast ({@code pending},
     * {@code approved}, {@code awaiting_cosign}); afterwards, 409 {@code payout.not_pending}.
     *
     * @param uuid the payout's uuid
     * @return the cancelled payout
     */
    public Payout cancel(String uuid) {
        return cancel(new PayoutCancelRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * @param lookup which payout to cancel, by {@code uuid}
     * @return the cancelled payout
     */
    public Payout cancel(PayoutCancelRequest lookup) {
        return cancel(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to cancel
     * @param options per-call options
     * @return the cancelled payout
     */
    public Payout cancel(PayoutCancelRequest lookup, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_CANCEL, lookup, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/approve} — approves a payout waiting for manual approval.
     *
     * @param uuid the payout's uuid
     * @return the approved payout
     */
    public Payout approve(String uuid) {
        return approve(new PayoutApproveRequest().uuid(uuid), RequestOptions.none());
    }

    /**
     * @param lookup which payout to approve, by {@code uuid}
     * @return the approved payout
     */
    public Payout approve(PayoutApproveRequest lookup) {
        return approve(lookup, RequestOptions.none());
    }

    /**
     * @param lookup which payout to approve
     * @param options per-call options
     * @return the approved payout
     */
    public Payout approve(PayoutApproveRequest lookup, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_APPROVE, lookup, options, Payout.class);
    }

    /**
     * {@code POST /v1/payout/history} — newest first; {@code kind: "refund"} lists refunds only.
     *
     * @return a lazy pager over the merchant's payouts
     */
    public Pager<Payout> history() {
        return history(new PayoutHistoryRequest(), RequestOptions.none());
    }

    /**
     * @param params filters and page bounds
     * @return a lazy pager over the matching payouts
     */
    public Pager<Payout> history(PayoutHistoryRequest params) {
        return history(params, RequestOptions.none());
    }

    /**
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy pager over the matching payouts
     */
    public Pager<Payout> history(PayoutHistoryRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYOUT_HISTORY, params, options, Payout.class);
    }

    /**
     * Alias of {@link #history()}.
     *
     * @return a lazy pager over the merchant's payouts
     */
    public Pager<Payout> list() {
        return history();
    }

    /**
     * Alias of {@link #history(PayoutHistoryRequest)}.
     *
     * @param params filters and page bounds
     * @return a lazy pager over the matching payouts
     */
    public Pager<Payout> list(PayoutHistoryRequest params) {
        return history(params);
    }

    /**
     * {@code POST /v1/payout/mass} — synchronous batch of at most 100: each element reports its own
     * outcome in the answer, so nothing has to be polled.
     *
     * @param request the payouts to send
     * @return one element per submitted payout, in the order they were sent
     */
    public List<BatchElement<Payout>> mass(PayoutMassRequest request) {
        return mass(request, RequestOptions.none());
    }

    /**
     * @param request the payouts to send
     * @param options per-call options
     * @return one element per submitted payout
     */
    public List<BatchElement<Payout>> mass(PayoutMassRequest request, RequestOptions options) {
        JavaType element = parametric(BatchElement.class, Payout.class);
        return plainList(Routes.POST_V1_PAYOUT_MASS, request, options, element);
    }

    /**
     * {@code POST /v1/payout/batch} — asynchronous batch of at most 5000: returns a ticket, poll
     * {@code batches().info(...)}. {@code order_id} is required on every item.
     *
     * @param request the payouts to submit
     * @return the batch ticket
     */
    public BatchSubmitted batch(PayoutBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * @param request the payouts to submit
     * @param options per-call options
     * @return the batch ticket
     */
    public BatchSubmitted batch(PayoutBatchRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_BATCH, request, options, BatchSubmitted.class);
    }

    /**
     * {@code POST /v1/payout/services} — the currencies and networks payouts can be sent on.
     *
     * @return a lazy pager over the available methods
     */
    public Pager<ServiceMethod> services() {
        return services(new PayoutServicesRequest(), RequestOptions.none());
    }

    /**
     * @param params page bounds
     * @return a lazy pager over the available methods
     */
    public Pager<ServiceMethod> services(PayoutServicesRequest params) {
        return services(params, RequestOptions.none());
    }

    /**
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the available methods
     */
    public Pager<ServiceMethod> services(PayoutServicesRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYOUT_SERVICES, params, options, ServiceMethod.class);
    }

    /**
     * {@code POST /v1/payout/fee-config/get} — who bears the network fee on payouts today.
     *
     * @return the current payout fee configuration
     */
    public PayoutFeeConfig getFeeConfig() {
        return getFeeConfig(RequestOptions.none());
    }

    /**
     * @param options per-call options
     * @return the current payout fee configuration
     */
    public PayoutFeeConfig getFeeConfig(RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_FEE_CONFIG_GET, null, options, PayoutFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/fee-config/set} — who bears the network fee by default.
     *
     * @param request the configuration to store
     * @return the stored configuration
     */
    public PayoutFeeConfig setFeeConfig(PayoutFeeConfigSetRequest request) {
        return setFeeConfig(request, RequestOptions.none());
    }

    /**
     * @param request the configuration to store
     * @param options per-call options
     * @return the stored configuration
     */
    public PayoutFeeConfig setFeeConfig(PayoutFeeConfigSetRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYOUT_FEE_CONFIG_SET, request, options, PayoutFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/refund-fee-config/get} — who bears the fee on refunds today.
     *
     * @return the current refund fee configuration
     */
    public RefundFeeConfig getRefundFeeConfig() {
        return getRefundFeeConfig(RequestOptions.none());
    }

    /**
     * @param options per-call options
     * @return the current refund fee configuration
     */
    public RefundFeeConfig getRefundFeeConfig(RequestOptions options) {
        return call(
                Routes.POST_V1_PAYOUT_REFUND_FEE_CONFIG_GET, null, options, RefundFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/refund-fee-config/set} — who bears the fee on refunds.
     *
     * @param request the configuration to store
     * @return the stored configuration
     */
    public RefundFeeConfig setRefundFeeConfig(PayoutRefundFeeConfigSetRequest request) {
        return setRefundFeeConfig(request, RequestOptions.none());
    }

    /**
     * @param request the configuration to store
     * @param options per-call options
     * @return the stored configuration
     */
    public RefundFeeConfig setRefundFeeConfig(
            PayoutRefundFeeConfigSetRequest request, RequestOptions options) {
        RouteSpec route = Routes.POST_V1_PAYOUT_REFUND_FEE_CONFIG_SET;
        return call(route, request, options, RefundFeeConfig.class);
    }
}
