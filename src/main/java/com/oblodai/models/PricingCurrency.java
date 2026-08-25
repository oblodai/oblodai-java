package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A currency an invoice may be priced in — every fiat the gateway quotes, plus the coins.
 *
 * @param currency the currency code (for example {@code USD})
 * @param decimals how many decimal places this currency is rendered with (JPY and KRW have none)
 * @param fiat true for fiat currencies, false for coins
 */
public record PricingCurrency(
        @JsonProperty("currency") String currency,
        @JsonProperty("decimals") Integer decimals,
        @JsonProperty("fiat") Boolean fiat) {}
