package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.ApiAllowlistAddRequest;
import com.oblodai.contract.requests.ApiAllowlistEnableRequest;
import com.oblodai.contract.requests.ApiAllowlistRemoveRequest;
import com.oblodai.contract.requests.AutoWithdrawDeleteRequest;
import com.oblodai.contract.requests.AutoWithdrawSetRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.ApiAllowlist;
import com.oblodai.models.AutoWithdrawRule;
import java.util.List;

/**
 * The sweep and allow-list routes of {@link Settings}: {@code POST /v1/auto-withdraw/list},
 * {@code /set} and {@code /delete} — the sweep rules — together with
 * {@code POST /v1/api-allowlist/list}, {@code /add}, {@code /remove} and {@code /enable} — the
 * source IPs the key may be used from.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Settings} and exists only to keep source files small. Reach every method here through
 * {@code settings()}.
 */
public abstract sealed class SettingsSweepAndAllowlistRoutes extends Resource permits Settings {

    /**
     * @param transport the engine to call through
     */
    protected SettingsSweepAndAllowlistRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/auto-withdraw/list} — the sweep rules in force.
     *
     * @return every rule, as a plain list the gateway caps rather than paginates
     */
    public List<AutoWithdrawRule> listAutoWithdraw() {
        return listAutoWithdraw(RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/list}.
     *
     * @param options per-call options
     * @return every rule
     */
    public List<AutoWithdrawRule> listAutoWithdraw(RequestOptions options) {
        return plainList(Routes.POST_V1_AUTO_WITHDRAW_LIST, null, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/auto-withdraw/set} — sweeps a currency once it passes {@code min_amount}.
     *
     * @param request the currency, the destination and the threshold
     * @return every rule after the change
     */
    public List<AutoWithdrawRule> setAutoWithdraw(AutoWithdrawSetRequest request) {
        return setAutoWithdraw(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/set}.
     *
     * @param request the currency, the destination and the threshold
     * @param options per-call options
     * @return every rule after the change
     */
    public List<AutoWithdrawRule> setAutoWithdraw(
            AutoWithdrawSetRequest request, RequestOptions options) {
        return plainList(
                Routes.POST_V1_AUTO_WITHDRAW_SET, request, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/auto-withdraw/delete} — stops sweeping that currency.
     *
     * @param currency the currency whose rule to drop
     * @return every rule that remains
     */
    public List<AutoWithdrawRule> deleteAutoWithdraw(String currency) {
        return deleteAutoWithdraw(currency, RequestOptions.none());
    }

    /**
     * {@code POST /v1/auto-withdraw/delete}.
     *
     * @param currency the currency whose rule to drop
     * @param options per-call options
     * @return every rule that remains
     */
    public List<AutoWithdrawRule> deleteAutoWithdraw(String currency, RequestOptions options) {
        AutoWithdrawDeleteRequest body = new AutoWithdrawDeleteRequest().currency(currency);
        return plainList(
                Routes.POST_V1_AUTO_WITHDRAW_DELETE, body, options, AutoWithdrawRule.class);
    }

    /**
     * {@code POST /v1/api-allowlist/list} — source IPs allowed to use the keys.
     *
     * @return the allowlist, and whether it is being enforced
     */
    public ApiAllowlist listApiAllowlist() {
        return listApiAllowlist(RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/list}.
     *
     * @param options per-call options
     * @return the allowlist, and whether it is being enforced
     */
    public ApiAllowlist listApiAllowlist(RequestOptions options) {
        return call(Routes.POST_V1_API_ALLOWLIST_LIST, null, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/add}.
     *
     * @param cidr the range to allow
     * @return the allowlist after the change
     */
    public ApiAllowlist addApiAllowlist(String cidr) {
        return addApiAllowlist(cidr, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/add}.
     *
     * @param cidr the range to allow
     * @param options per-call options
     * @return the allowlist after the change
     */
    public ApiAllowlist addApiAllowlist(String cidr, RequestOptions options) {
        ApiAllowlistAddRequest body = new ApiAllowlistAddRequest().cidr(cidr);
        return call(Routes.POST_V1_API_ALLOWLIST_ADD, body, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/remove}.
     *
     * @param cidr the range to drop
     * @return the allowlist after the change
     */
    public ApiAllowlist removeApiAllowlist(String cidr) {
        return removeApiAllowlist(cidr, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/remove}.
     *
     * @param cidr the range to drop
     * @param options per-call options
     * @return the allowlist after the change
     */
    public ApiAllowlist removeApiAllowlist(String cidr, RequestOptions options) {
        ApiAllowlistRemoveRequest body = new ApiAllowlistRemoveRequest().cidr(cidr);
        return call(Routes.POST_V1_API_ALLOWLIST_REMOVE, body, options, ApiAllowlist.class);
    }

    /**
     * {@code POST /v1/api-allowlist/enable} — enforcement on or off; the list is kept either way.
     *
     * @param enabled true to enforce the list
     * @return the allowlist after the change
     */
    public ApiAllowlist enableApiAllowlist(boolean enabled) {
        return enableApiAllowlist(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/api-allowlist/enable}.
     *
     * @param enabled true to enforce the list
     * @param options per-call options
     * @return the allowlist after the change
     */
    public ApiAllowlist enableApiAllowlist(boolean enabled, RequestOptions options) {
        ApiAllowlistEnableRequest body = new ApiAllowlistEnableRequest().enabled(enabled);
        return call(Routes.POST_V1_API_ALLOWLIST_ENABLE, body, options, ApiAllowlist.class);
    }
}
