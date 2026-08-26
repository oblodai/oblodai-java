package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PayoutCalculateRequest;
import com.oblodai.contract.requests.PayoutServicesRequest;
import com.oblodai.contract.requests.PayoutValidateRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.PayoutCalculation;
import com.oblodai.models.PayoutValidation;
import com.oblodai.models.ServiceMethod;

/**
 * The before-you-send routes of {@link Payouts}: {@code POST /v1/payout/validate},
 * {@code POST /v1/payout/calculate} and {@code POST /v1/payout/services} — the dry run, the
 * price and the currency and network pairs payouts can go out on. Nothing here moves money.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Payouts} and exists only to keep source files small. Reach every method here through
 * {@code payouts()}.
 */
public abstract sealed class PayoutsPreflightRoutes extends PayoutsFeeRoutes permits Payouts {

    /**
     * @param transport the engine to call through
     */
    protected PayoutsPreflightRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payout/validate} — dry run: every check {@link Payouts#create} makes, with nothing
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
     * {@code POST /v1/payout/services} — the currencies and networks payouts can be sent on.
     *
     * @return a lazy pager over the available methods
     */
    public Pager<ServiceMethod> services() {
        return services(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payout/services}.
     *
     * @param options per-call options
     * @return a lazy pager over the available methods
     */
    public Pager<ServiceMethod> services(RequestOptions options) {
        return services(new PayoutServicesRequest(), options);
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
}
