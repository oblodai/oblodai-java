package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.VrcsRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.Balance;
import com.oblodai.models.ReferralInfo;
import com.oblodai.models.VrcsStatus;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Balances and account-level facts.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Account}: the same methods,
 * returning {@link CompletableFuture}.
 */
public final class Account extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Account(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/balance} — the available balance per currency.
     *
     * @return a future of the balances
     */
    public CompletableFuture<Balance> balance() {
        return balance(RequestOptions.none());
    }

    /**
     * {@code POST /v1/balance}.
     *
     * @param options per-call options
     * @return a future of the balances
     */
    public CompletableFuture<Balance> balance(RequestOptions options) {
        return callAsync(Routes.POST_V1_BALANCE, null, options, Balance.class);
    }

    /**
     * {@code POST /v1/referral/info} — the merchant's referral code, its link, and what it has
     * earned.
     *
     * @return a future of the referral standing
     */
    public CompletableFuture<ReferralInfo> referral() {
        return referral(RequestOptions.none());
    }

    /**
     * {@code POST /v1/referral/info}.
     *
     * @param options per-call options
     * @return a future of the referral standing
     */
    public CompletableFuture<ReferralInfo> referral(RequestOptions options) {
        return callAsync(Routes.POST_V1_REFERRAL_INFO, null, options, ReferralInfo.class);
    }

    /**
     * {@code POST /v1/vrcs} — reads volatility-risk conversion, which converts volatile deposits
     * to USDT as they arrive. Sent without a body, this only reports the flag.
     *
     * @return a future of whether conversion is on
     */
    public CompletableFuture<VrcsStatus> vrcs() {
        return vrcs(RequestOptions.none());
    }

    /**
     * {@code POST /v1/vrcs} — reads the flag.
     *
     * @param options per-call options
     * @return a future of whether conversion is on
     */
    public CompletableFuture<VrcsStatus> vrcs(RequestOptions options) {
        return callAsync(Routes.POST_V1_VRCS, null, options, VrcsStatus.class);
    }

    /**
     * {@code POST /v1/vrcs} — switches volatility-risk conversion on or off.
     *
     * @param enabled true to convert volatile deposits to USDT
     * @return a future of the stored state
     */
    public CompletableFuture<VrcsStatus> vrcs(boolean enabled) {
        return vrcs(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/vrcs} — switches conversion on or off.
     *
     * @param enabled true to convert volatile deposits to USDT
     * @param options per-call options
     * @return a future of the stored state
     */
    public CompletableFuture<VrcsStatus> vrcs(boolean enabled, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_VRCS,
                new VrcsRequest().enabled(enabled),
                options,
                VrcsStatus.class);
    }
}
