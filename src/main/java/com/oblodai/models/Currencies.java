package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code GET /v1/currencies} — the whole catalog: what can be settled, and what can be priced in.
 *
 * @param currencies the settlement assets and their networks
 * @param pricingCurrencies the currencies an invoice may be priced in
 */
public record Currencies(
        @JsonProperty("currencies") List<CurrencyInfo> currencies,
        @JsonProperty("pricing_currencies") List<PricingCurrency> pricingCurrencies) {}
