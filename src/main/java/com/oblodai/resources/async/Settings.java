package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.ApiAllowlistAddRequest;
import com.oblodai.contract.requests.ApiAllowlistEnableRequest;
import com.oblodai.contract.requests.ApiAllowlistRemoveRequest;
import com.oblodai.contract.requests.AutoWithdrawDeleteRequest;
import com.oblodai.contract.requests.AutoWithdrawSetRequest;
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
import com.oblodai.models.ApiAllowlist;
import com.oblodai.models.AutoRefundConfig;
import com.oblodai.models.AutoWithdrawRule;
import com.oblodai.models.DiscountRule;
import com.oblodai.models.OkResult;
import com.oblodai.models.PaymentFeeConfig;
import com.oblodai.resources.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Merchant-level configuration exposed over the API.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Settings}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Settings extends Resource {

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
        return listDiscounts(null, RequestOptions.none());
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
        return listAccepted(null, RequestOptions.none());
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

    /**
     * {@code POST /v1/auto-withdraw/list} — the sweep rules in force. Payout key.
     *
     * @return a future of every rule, as a plain list the gateway caps rather than paginates
     */
    public CompletableFuture<List<AutoWithdrawRule>> listAutoWithdraw() {
        return listAutoWithdraw(RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/list}.
     *
     * @param options per-call options
     * @return a future of every rule
     */
    public CompletableFuture<List<AutoWithdrawRule>> listAutoWithdraw(RequestOptions options) {
        return plainListAsync(
                Routes.POST_V1_AUTO_WITHDRAW_LIST, null, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/auto-withdraw/set} — sweeps a currency once it passes {@code min_amount}.
     *
     * @param request the currency, the destination and the threshold
     * @return a future of every rule after the change
     */
    public CompletableFuture<List<AutoWithdrawRule>> setAutoWithdraw(
            AutoWithdrawSetRequest request) {
        return setAutoWithdraw(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/set}. Payout key.
     *
     * @param request the currency, the destination and the threshold
     * @param options per-call options
     * @return a future of every rule after the change
     */
    public CompletableFuture<List<AutoWithdrawRule>> setAutoWithdraw(
            AutoWithdrawSetRequest request, RequestOptions options) {
        return plainListAsync(
                Routes.POST_V1_AUTO_WITHDRAW_SET, request, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/auto-withdraw/delete} — stops sweeping that currency. Payout key.
     *
     * @param currency the currency whose rule to drop
     * @return a future of every rule that remains
     */
    public CompletableFuture<List<AutoWithdrawRule>> deleteAutoWithdraw(String currency) {
        return deleteAutoWithdraw(currency, RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/delete}.
     *
     * @param currency the currency whose rule to drop
     * @param options per-call options
     * @return a future of every rule that remains
     */
    public CompletableFuture<List<AutoWithdrawRule>> deleteAutoWithdraw(
            String currency, RequestOptions options) {
        AutoWithdrawDeleteRequest body = new AutoWithdrawDeleteRequest().currency(currency);
        return plainListAsync(
                Routes.POST_V1_AUTO_WITHDRAW_DELETE, body, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/api-allowlist/list} — source IPs allowed to use the keys. Payout key.
     *
     * @return a future of the allowlist, and whether it is being enforced
     */
    public CompletableFuture<ApiAllowlist> listApiAllowlist() {
        return listApiAllowlist(RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/list}.
     *
     * @param options per-call options
     * @return a future of the allowlist, and whether it is being enforced
     */
    public CompletableFuture<ApiAllowlist> listApiAllowlist(RequestOptions options) {
        return callAsync(Routes.POST_V1_API_ALLOWLIST_LIST, null, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/add}. Payout key.
     *
     * @param cidr the range to allow
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> addApiAllowlist(String cidr) {
        return addApiAllowlist(cidr, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/add}.
     *
     * @param cidr the range to allow
     * @param options per-call options
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> addApiAllowlist(String cidr, RequestOptions options) {
        ApiAllowlistAddRequest body = new ApiAllowlistAddRequest().cidr(cidr);
        return callAsync(Routes.POST_V1_API_ALLOWLIST_ADD, body, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/remove}. Payout key.
     *
     * @param cidr the range to drop
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> removeApiAllowlist(String cidr) {
        return removeApiAllowlist(cidr, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/remove}.
     *
     * @param cidr the range to drop
     * @param options per-call options
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> removeApiAllowlist(String cidr, RequestOptions options) {
        ApiAllowlistRemoveRequest body = new ApiAllowlistRemoveRequest().cidr(cidr);
        return callAsync(Routes.POST_V1_API_ALLOWLIST_REMOVE, body, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/enable} — enforcement on or off; the list is kept either way.
     *
     * @param enabled true to enforce the list
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> enableApiAllowlist(boolean enabled) {
        return enableApiAllowlist(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/enable}. Payout key.
     *
     * @param enabled true to enforce the list
     * @param options per-call options
     * @return a future of the allowlist after the change
     */
    public CompletableFuture<ApiAllowlist> enableApiAllowlist(
            boolean enabled, RequestOptions options) {
        ApiAllowlistEnableRequest body = new ApiAllowlistEnableRequest().enabled(enabled);
        return callAsync(Routes.POST_V1_API_ALLOWLIST_ENABLE, body, options, ApiAllowlist.class);
    }
}
