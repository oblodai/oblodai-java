package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.SplitConfigSetRequest;
import com.oblodai.contract.requests.SplitRecipientOptinRequest;
import com.oblodai.contract.requests.SplitRuleDeleteRequest;
import com.oblodai.contract.requests.SplitRuleListRequest;
import com.oblodai.contract.requests.SplitRuleRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.OkResult;
import com.oblodai.models.SplitConfig;
import com.oblodai.models.SplitOptIn;
import com.oblodai.models.SplitRule;

/**
 * Revenue splits: a percentage of every payment forwarded to a partner. Payout key.
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
     * @return the created rule
     */
    public SplitRule createRule(SplitRuleRequest request) {
        return createRule(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule}.
     *
     * @param request the share, and who receives it
     * @param options per-call options
     * @return the created rule
     */
    public SplitRule createRule(SplitRuleRequest request, RequestOptions options) {
        return call(Routes.POST_V1_SPLIT_RULE, request, options, SplitRule.class);
    }

    /**
     * {@code POST /v1/split/rule/list} — the merchant's split rules.
     *
     * @return a lazy pager over the rules
     */
    public Pager<SplitRule> listRules() {
        return listRules(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param options per-call options
     * @return a lazy pager over the rules
     */
    public Pager<SplitRule> listRules(RequestOptions options) {
        return listRules(new SplitRuleListRequest(), options);
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param params page bounds
     * @return a lazy pager over the rules
     */
    public Pager<SplitRule> listRules(SplitRuleListRequest params) {
        return listRules(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/list}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the rules
     */
    public Pager<SplitRule> listRules(SplitRuleListRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_SPLIT_RULE_LIST, params, options, SplitRule.class);
    }

    /**
     * {@code POST /v1/split/rule/delete} — stops forwarding to that partner. Payments already
     * split keep their shares.
     *
     * @param ruleId the rule's id
     * @return whether the rule was removed
     */
    public OkResult deleteRule(String ruleId) {
        return deleteRule(ruleId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/rule/delete}.
     *
     * @param ruleId the rule's id
     * @param options per-call options
     * @return whether the rule was removed
     */
    public OkResult deleteRule(String ruleId, RequestOptions options) {
        return call(
                Routes.POST_V1_SPLIT_RULE_DELETE,
                new SplitRuleDeleteRequest().ruleId(ruleId),
                options,
                OkResult.class);
    }

    /**
     * {@code POST /v1/split/config/get} — the merchant's split settings.
     *
     * @return the current configuration
     */
    public SplitConfig getConfig() {
        return getConfig(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/config/get}.
     *
     * @param options per-call options
     * @return the current configuration
     */
    public SplitConfig getConfig(RequestOptions options) {
        return call(Routes.POST_V1_SPLIT_CONFIG_GET, null, options, SplitConfig.class);
    }

    /**
     * {@code POST /v1/split/config/set} — how long a split share is held back before it is
     * forwarded, so that a refund can still reclaim it.
     *
     * @param request the hold-back window
     * @return the stored configuration
     */
    public SplitConfig setConfig(SplitConfigSetRequest request) {
        return setConfig(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/config/set}.
     *
     * @param request the hold-back window
     * @param options per-call options
     * @return the stored configuration
     */
    public SplitConfig setConfig(SplitConfigSetRequest request, RequestOptions options) {
        return call(Routes.POST_V1_SPLIT_CONFIG_SET, request, options, SplitConfig.class);
    }

    /**
     * {@code POST /v1/split/recipient/optin/get} — whether this merchant accepts being named as
     * the recipient of someone else's split.
     *
     * @return the current opt-in state
     */
    public SplitOptIn getOptIn() {
        return getOptIn(RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/recipient/optin/get}.
     *
     * @param options per-call options
     * @return the current opt-in state
     */
    public SplitOptIn getOptIn(RequestOptions options) {
        return call(Routes.POST_V1_SPLIT_RECIPIENT_OPTIN_GET, null, options, SplitOptIn.class);
    }

    /**
     * {@code POST /v1/split/recipient/optin} — opts in or out of receiving splits.
     *
     * @param enabled true to accept being a split recipient
     * @return the stored opt-in state
     */
    public SplitOptIn setOptIn(boolean enabled) {
        return setOptIn(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/split/recipient/optin}.
     *
     * @param enabled true to accept being a split recipient
     * @param options per-call options
     * @return the stored opt-in state
     */
    public SplitOptIn setOptIn(boolean enabled, RequestOptions options) {
        return call(
                Routes.POST_V1_SPLIT_RECIPIENT_OPTIN,
                new SplitRecipientOptinRequest().enabled(enabled),
                options,
                SplitOptIn.class);
    }
}
