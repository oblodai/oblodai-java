package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.VrcsRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.Balance;
import com.oblodai.models.ReferralInfo;
import com.oblodai.models.VrcsStatus;

/** Balances and account-level facts. */
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
     * @return the balances
     */
    public Balance balance() {
        return balance(RequestOptions.none());
    }

    /**
     * {@code POST /v1/balance}.
     *
     * @param options per-call options
     * @return the balances
     */
    public Balance balance(RequestOptions options) {
        return call(Routes.POST_V1_BALANCE, null, options, Balance.class);
    }

    /**
     * {@code POST /v1/referral/info} — the merchant's referral code, its link, and what it has
     * earned.
     *
     * @return the referral standing
     */
    public ReferralInfo referral() {
        return referral(RequestOptions.none());
    }

    /**
     * {@code POST /v1/referral/info}.
     *
     * @param options per-call options
     * @return the referral standing
     */
    public ReferralInfo referral(RequestOptions options) {
        return call(Routes.POST_V1_REFERRAL_INFO, null, options, ReferralInfo.class);
    }

    /**
     * {@code POST /v1/vrcs} — reads volatility-risk conversion, which converts volatile deposits
     * to USDT as they arrive. Sent without a body, this only reports the flag.
     *
     * @return whether conversion is on
     */
    public VrcsStatus vrcs() {
        return vrcs(RequestOptions.none());
    }

    /**
     * {@code POST /v1/vrcs} — reads the flag.
     *
     * @param options per-call options
     * @return whether conversion is on
     */
    public VrcsStatus vrcs(RequestOptions options) {
        return call(Routes.POST_V1_VRCS, null, options, VrcsStatus.class);
    }

    /**
     * {@code POST /v1/vrcs} — switches volatility-risk conversion on or off.
     *
     * @param enabled true to convert volatile deposits to USDT
     * @return the stored state
     */
    public VrcsStatus vrcs(boolean enabled) {
        return vrcs(enabled, RequestOptions.none());
    }

    /**
     * {@code POST /v1/vrcs} — switches conversion on or off.
     *
     * @param enabled true to convert volatile deposits to USDT
     * @param options per-call options
     * @return the stored state
     */
    public VrcsStatus vrcs(boolean enabled, RequestOptions options) {
        return call(
                Routes.POST_V1_VRCS,
                new VrcsRequest().enabled(enabled),
                options,
                VrcsStatus.class);
    }
}
