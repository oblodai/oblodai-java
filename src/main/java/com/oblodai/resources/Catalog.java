package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.ExchangeRateListRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.Currencies;
import com.oblodai.models.ExchangeRate;

/** Public reference data. These routes are unsigned — no credentials are needed. */
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
     * @return the catalogue
     */
    public Currencies currencies() {
        return currencies(RequestOptions.none());
    }

    /**
     * {@code GET /v1/currencies}.
     *
     * @param options per-call options
     * @return the catalogue
     */
    public Currencies currencies(RequestOptions options) {
        return call(Routes.GET_V1_CURRENCIES, null, options, Currencies.class);
    }

    /**
     * {@code POST /v1/exchange-rate/list} — the rates in force, optionally narrowed by
     * {@code currency_from} and {@code currency_to}.
     *
     * @return a lazy pager over the rates
     */
    public Pager<ExchangeRate> exchangeRates() {
        return exchangeRates(new ExchangeRateListRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/exchange-rate/list}.
     *
     * @param params which pair to narrow to, and page bounds
     * @return a lazy pager over the rates
     */
    public Pager<ExchangeRate> exchangeRates(ExchangeRateListRequest params) {
        return exchangeRates(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/exchange-rate/list}.
     *
     * @param params which pair to narrow to, and page bounds
     * @param options per-call options
     * @return a lazy pager over the rates
     */
    public Pager<ExchangeRate> exchangeRates(
            ExchangeRateListRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_EXCHANGE_RATE_LIST, params, options, ExchangeRate.class);
    }
}
