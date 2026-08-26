package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.RouteSpec;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayoutFeeConfigSetRequest;
import com.oblodai.contract.requests.PayoutRefundFeeConfigSetRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.PayoutFeeConfig;
import com.oblodai.models.RefundFeeConfig;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * The fee-configuration routes of {@link Payouts}: {@code POST /v1/payout/fee-config/get} and
 * {@code /set}, {@code POST /v1/payout/refund-fee-config/get} and {@code /set} — who bears the
 * network fee on payouts, and who bears it on refunds.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Payouts} and exists only to keep source files small. Reach every method here through
 * {@code payouts()}.
 */
public abstract sealed class PayoutsFeeRoutes extends Resource permits PayoutsPreflightRoutes {

    /**
     * @param transport the engine to call through
     */
    protected PayoutsFeeRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payout/fee-config/get} — who bears the network fee on payouts today.
     *
     * @return a future of the current payout fee configuration
     */
    public CompletableFuture<PayoutFeeConfig> getFeeConfig() {
        return getFeeConfig(RequestOptions.none());
    }

    /**
     * @param options per-call options
     * @return a future of the current payout fee configuration
     */
    public CompletableFuture<PayoutFeeConfig> getFeeConfig(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYOUT_FEE_CONFIG_GET, null, options, PayoutFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/fee-config/set} — who bears the network fee by default.
     *
     * @param request the configuration to store
     * @return a future of the stored configuration
     */
    public CompletableFuture<PayoutFeeConfig> setFeeConfig(PayoutFeeConfigSetRequest request) {
        return setFeeConfig(request, RequestOptions.none());
    }

    /**
     * @param request the configuration to store
     * @param options per-call options
     * @return a future of the stored configuration
     */
    public CompletableFuture<PayoutFeeConfig> setFeeConfig(
            PayoutFeeConfigSetRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYOUT_FEE_CONFIG_SET, request, options, PayoutFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/refund-fee-config/get} — who bears the fee on refunds today.
     *
     * @return a future of the current refund fee configuration
     */
    public CompletableFuture<RefundFeeConfig> getRefundFeeConfig() {
        return getRefundFeeConfig(RequestOptions.none());
    }

    /**
     * @param options per-call options
     * @return a future of the current refund fee configuration
     */
    public CompletableFuture<RefundFeeConfig> getRefundFeeConfig(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYOUT_REFUND_FEE_CONFIG_GET, null, options, RefundFeeConfig.class);
    }

    /**
     * {@code POST /v1/payout/refund-fee-config/set} — who bears the fee on refunds.
     *
     * @param request the configuration to store
     * @return a future of the stored configuration
     */
    public CompletableFuture<RefundFeeConfig> setRefundFeeConfig(
            PayoutRefundFeeConfigSetRequest request) {
        return setRefundFeeConfig(request, RequestOptions.none());
    }

    /**
     * @param request the configuration to store
     * @param options per-call options
     * @return a future of the stored configuration
     */
    public CompletableFuture<RefundFeeConfig> setRefundFeeConfig(
            PayoutRefundFeeConfigSetRequest request, RequestOptions options) {
        RouteSpec route = Routes.POST_V1_PAYOUT_REFUND_FEE_CONFIG_SET;
        return callAsync(route, request, options, RefundFeeConfig.class);
    }
}
