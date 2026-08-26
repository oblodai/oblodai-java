package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.ExchangeRateListRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.Currencies;
import com.oblodai.models.ExchangeRate;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Public reference data. These routes are unsigned — no credentials are needed.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Catalog}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Catalog extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Catalog(Transport transport) {
        super(transport);
    }

    /**
     * {@code GET /v1/currencies} — every asset the gateway knows, the networks it runs on, and
     * which of them are available right now.
     *
     * @return a future of the catalogue
     */
    public CompletableFuture<Currencies> currencies() {
        return currencies(RequestOptions.none());
    }

    /**
     * {@code GET /v1/currencies}.
     *
     * @param options per-call options
     * @return a future of the catalogue
     */
    public CompletableFuture<Currencies> currencies(RequestOptions options) {
        return callAsync(Routes.GET_V1_CURRENCIES, null, options, Currencies.class);
    }

    /**
     * {@code POST /v1/exchange-rate/list} — the rates in force, optionally narrowed by
     * {@code currency_from} and {@code currency_to}.
     *
     * @return a lazy non-blocking pager over the rates
     */
    public AsyncPager<ExchangeRate> exchangeRates() {
        return exchangeRates(RequestOptions.none());
    }

    /**
     * {@code POST /v1/exchange-rate/list}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the rates
     */
    public AsyncPager<ExchangeRate> exchangeRates(RequestOptions options) {
        return exchangeRates(new ExchangeRateListRequest(), options);
    }

    /**
     * {@code POST /v1/exchange-rate/list}.
     *
     * @param params which pair to narrow to, and page bounds
     * @return a lazy non-blocking pager over the rates
     */
    public AsyncPager<ExchangeRate> exchangeRates(ExchangeRateListRequest params) {
        return exchangeRates(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/exchange-rate/list}.
     *
     * @param params which pair to narrow to, and page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the rates
     */
    public AsyncPager<ExchangeRate> exchangeRates(
            ExchangeRateListRequest params, RequestOptions options) {
        return pagerAsync(Routes.POST_V1_EXCHANGE_RATE_LIST, params, options, ExchangeRate.class);
    }
}
