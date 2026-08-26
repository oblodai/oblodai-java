package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.SplitConfigSetRequest;
import com.oblodai.contract.requests.SplitRecipientOptinRequest;
import com.oblodai.contract.requests.SplitRuleDeleteRequest;
import com.oblodai.contract.requests.SplitRuleListRequest;
import com.oblodai.contract.requests.SplitRuleRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.OkResult;
import com.oblodai.models.SplitConfig;
import com.oblodai.models.SplitOptIn;
import com.oblodai.models.SplitRule;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Revenue splits: a percentage of every payment forwarded to a partner. Payout key.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Splits}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Splits extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Splits(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/split/rule} — sends a share either to an external address
     * ({@code address} plus {@code network}) or to another merchant on the platform
     * ({@code merchant_id}).
     *
     * @param request the share, and who receives it
     * @return a future of the created rule
     */
    public CompletableFuture<SplitRule> createRule(SplitRuleRequest request) {
        return createRule(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule}.
     *
     * @param request the share, and who receives it
     * @param options per-call options
     * @return a future of the created rule
     */
    public CompletableFuture<SplitRule> createRule(
            SplitRuleRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_SPLIT_RULE, request, options, SplitRule.class);
    }

    /**
     * {@code POST /v1/split/rule/list} — the merchant's split rules.
     *
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<SplitRule> listRules() {
        return listRules(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<SplitRule> listRules(RequestOptions options) {
        return listRules(new SplitRuleListRequest(), options);
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param params page bounds
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<SplitRule> listRules(SplitRuleListRequest params) {
        return listRules(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the rules
     */
    public AsyncPager<SplitRule> listRules(SplitRuleListRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_SPLIT_RULE_LIST, params, options, SplitRule.class);
    }

    /**
     * {@code POST /v1/split/rule/delete} — stops forwarding to that partner. Payments already
     * split keep their shares.
     *
     * @param ruleId the rule's id
     * @return a future of whether the rule was removed
     */
    public CompletableFuture<OkResult> deleteRule(String ruleId) {
        return deleteRule(ruleId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/delete}.
     *
     * @param ruleId the rule's id
     * @param options per-call options
     * @return a future of whether the rule was removed
     */
    public CompletableFuture<OkResult> deleteRule(String ruleId, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_SPLIT_RULE_DELETE,
                new SplitRuleDeleteRequest().ruleId(ruleId),
                options,
                OkResult.class);
    }

    /**
     * {@code POST /v1/split/config/get} — the merchant's split settings.
     *
     * @return a future of the current configuration
     */
    public CompletableFuture<SplitConfig> getConfig() {
        return getConfig(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/config/get}.
     *
     * @param options per-call options
     * @return a future of the current configuration
     */
    public CompletableFuture<SplitConfig> getConfig(RequestOptions options) {
        return callAsync(Routes.POST_V1_SPLIT_CONFIG_GET, null, options, SplitConfig.class);
    }

    /**
     * {@code POST /v1/split/config/set} — how long a split share is held back before it is
     * forwarded, so that a refund can still reclaim it.
     *
     * @param request the hold-back window
     * @return a future of the stored configuration
     */
    public CompletableFuture<SplitConfig> setConfig(SplitConfigSetRequest request) {
        return setConfig(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/config/set}.
     *
     * @param request the hold-back window
     * @param options per-call options
     * @return a future of the stored configuration
     */
    public CompletableFuture<SplitConfig> setConfig(
            SplitConfigSetRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_SPLIT_CONFIG_SET, request, options, SplitConfig.class);
    }

    /**
     * {@code POST /v1/split/recipient/optin/get} — whether this merchant accepts being named as
     * the recipient of someone else's split.
     *
     * @return a future of the current opt-in state
     */
    public CompletableFuture<SplitOptIn> getOptIn() {
        return getOptIn(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/recipient/optin/get}.
     *
     * @param options per-call options
     * @return a future of the current opt-in state
     */
    public CompletableFuture<SplitOptIn> getOptIn(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_SPLIT_RECIPIENT_OPTIN_GET, null, options, SplitOptIn.class);
    }

    /**
     * {@code POST /v1/split/recipient/optin} — opts in or out of receiving splits.
     *
     * @param enabled true to accept being a split recipient
     * @return a future of the stored opt-in state
     */
    public CompletableFuture<SplitOptIn> setOptIn(boolean enabled) {
        return setOptIn(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/recipient/optin}.
     *
     * @param enabled true to accept being a split recipient
     * @param options per-call options
     * @return a future of the stored opt-in state
     */
    public CompletableFuture<SplitOptIn> setOptIn(boolean enabled, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_SPLIT_RECIPIENT_OPTIN,
                new SplitRecipientOptinRequest().enabled(enabled),
                options,
                SplitOptIn.class);
    }
}
