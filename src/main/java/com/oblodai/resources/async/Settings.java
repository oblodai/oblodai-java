package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PaymentAcceptedListRequest;
import com.oblodai.contract.requests.PaymentAcceptedSetRequest;
import com.oblodai.contract.requests.PaymentAccuracySetRequest;
import com.oblodai.contract.requests.PaymentAutorefundSetRequest;
import com.oblodai.contract.requests.PaymentDiscountListRequest;
import com.oblodai.contract.requests.PaymentDiscountSetRequest;
import com.oblodai.contract.requests.PaymentFeeConfigSetRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.AcceptedMethod;
import com.oblodai.models.AccuracyConfig;
import com.oblodai.models.AutoRefundConfig;
import com.oblodai.models.DiscountRule;
import com.oblodai.models.OkResult;
import com.oblodai.models.PaymentFeeConfig;
import java.util.concurrent.CompletableFuture;

/**
 * Merchant-level configuration exposed over the API.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Settings}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Settings extends SettingsPayoutKeyRoutes {

    /**
     * @param transport the engine to call through
     */
    public Settings(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment/discount/set} — a payer-facing discount or markup, per pair.
     *
     * @param request which currency and network, and by how much
     * @return a future of the stored rule
     */
    public CompletableFuture<DiscountRule> setDiscount(PaymentDiscountSetRequest request) {
        return setDiscount(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/discount/set}.
     *
     * @param request which currency and network, and by how much
     * @param options per-call options
     * @return a future of the stored rule
     */
    public CompletableFuture<DiscountRule> setDiscount(
            PaymentDiscountSetRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_DISCOUNT_SET, request, options, DiscountRule.class);
    }

    /**
     * {@code POST /v1/payment/discount/list} — the discounts in force, newest first.
     *
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<DiscountRule> listDiscounts() {
        return listDiscounts(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/discount/list}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<DiscountRule> listDiscounts(RequestOptions options) {
        return listDiscounts(null, options);
    }

    /**
     * {@code POST /v1/payment/discount/list}.
     *
     * @param params page bounds, or null for the gateway's defaults
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<DiscountRule> listDiscounts(PaymentDiscountListRequest params) {
        return listDiscounts(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/discount/list}.
     *
     * @param params page bounds, or null for the gateway's defaults
     * @param options per-call options
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<DiscountRule> listDiscounts(
            PaymentDiscountListRequest params, RequestOptions options) {
        return pagerAsync(
                Routes.POST_V1_PAYMENT_DISCOUNT_LIST, params, options, DiscountRule.class);
    }

    /**
     * {@code POST /v1/payment/accuracy/get} — how far off a payment may be and still count.
     *
     * @return a future of the current tolerance
     */
    public CompletableFuture<AccuracyConfig> getAccuracy() {
        return getAccuracy(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/accuracy/get}.
     *
     * @param options per-call options
     * @return a future of the current tolerance
     */
    public CompletableFuture<AccuracyConfig> getAccuracy(RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_ACCURACY_GET, null, options, AccuracyConfig.class);
    }

    /**
     * {@code POST /v1/payment/accuracy/set}.
     *
     * @param request the tolerance to store
     * @return a future of the stored tolerance
     */
    public CompletableFuture<AccuracyConfig> setAccuracy(PaymentAccuracySetRequest request) {
        return setAccuracy(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/accuracy/set}.
     *
     * @param request the tolerance to store
     * @param options per-call options
     * @return a future of the stored tolerance
     */
    public CompletableFuture<AccuracyConfig> setAccuracy(
            PaymentAccuracySetRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_ACCURACY_SET, request, options, AccuracyConfig.class);
    }

    /**
     * {@code POST /v1/payment/autorefund/get}.
     *
     * @return a future of the current auto-refund settings
     */
    public CompletableFuture<AutoRefundConfig> getAutoRefund() {
        return getAutoRefund(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/autorefund/get}.
     *
     * @param options per-call options
     * @return a future of the current auto-refund settings
     */
    public CompletableFuture<AutoRefundConfig> getAutoRefund(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_AUTOREFUND_GET, null, options, AutoRefundConfig.class);
    }

    /**
     * {@code POST /v1/payment/autorefund/set} — refunds over- and underpayments by itself.
     *
     * @param request the settings to store
     * @return a future of the stored settings
     */
    public CompletableFuture<AutoRefundConfig> setAutoRefund(PaymentAutorefundSetRequest request) {
        return setAutoRefund(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/autorefund/set}.
     *
     * @param request the settings to store
     * @param options per-call options
     * @return a future of the stored settings
     */
    public CompletableFuture<AutoRefundConfig> setAutoRefund(
            PaymentAutorefundSetRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_AUTOREFUND_SET, request, options, AutoRefundConfig.class);
    }

    /**
     * {@code POST /v1/payment/accepted/list} — the currency and network pairs invoices may be paid
     * in.
     *
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<AcceptedMethod> listAccepted() {
        return listAccepted(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/accepted/list}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<AcceptedMethod> listAccepted(RequestOptions options) {
        return listAccepted(null, options);
    }

    /**
     * {@code POST /v1/payment/accepted/list}.
     *
     * @param params page bounds, or null for the gateway's defaults
     * @return a lazy non-blocking pager over the accepted methods
     */
    public AsyncPager<AcceptedMethod> listAccepted(PaymentAcceptedListRequest params) {
        return listAccepted(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/accepted/list}.
     *
     * @param params page bounds, or null for the gateway's defaults
     * @param options per-call options
     * @return a lazy non-blocking pager over the accepted pairs
     */
    public AsyncPager<AcceptedMethod> listAccepted(
            PaymentAcceptedListRequest params, RequestOptions options) {
        return pagerAsync(
                Routes.POST_V1_PAYMENT_ACCEPTED_LIST, params, options, AcceptedMethod.class);
    }

    /**
     * {@code POST /v1/payment/accepted/set}.
     *
     * @param request which pairs to accept
     * @return a future of whether the change was stored
     */
    public CompletableFuture<OkResult> setAccepted(PaymentAcceptedSetRequest request) {
        return setAccepted(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/accepted/set}.
     *
     * @param request which pairs to accept
     * @param options per-call options
     * @return a future of whether the change was stored
     */
    public CompletableFuture<OkResult> setAccepted(
            PaymentAcceptedSetRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_PAYMENT_ACCEPTED_SET, request, options, OkResult.class);
    }

    /**
     * {@code POST /v1/payment/fee-config/get} — the share of the network fee the payer carries.
     *
     * @return a future of the current fee split
     */
    public CompletableFuture<PaymentFeeConfig> getPaymentFeeConfig() {
        return getPaymentFeeConfig(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/fee-config/get}.
     *
     * @param options per-call options
     * @return a future of the current fee split
     */
    public CompletableFuture<PaymentFeeConfig> getPaymentFeeConfig(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_FEE_CONFIG_GET, null, options, PaymentFeeConfig.class);
    }

    /**
     * {@code POST /v1/payment/fee-config/set}.
     *
     * @param request the fee split to store
     * @return a future of the stored fee split
     */
    public CompletableFuture<PaymentFeeConfig> setPaymentFeeConfig(
            PaymentFeeConfigSetRequest request) {
        return setPaymentFeeConfig(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/fee-config/set}.
     *
     * @param request the fee split to store
     * @param options per-call options
     * @return a future of the stored fee split
     */
    public CompletableFuture<PaymentFeeConfig> setPaymentFeeConfig(
            PaymentFeeConfigSetRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_FEE_CONFIG_SET, request, options, PaymentFeeConfig.class);
    }
}
