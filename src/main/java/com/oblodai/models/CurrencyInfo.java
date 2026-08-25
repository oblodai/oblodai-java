package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One settlement asset of the catalog, with every network it is available on.
 *
 * @param currency the asset code (for example {@code USDT})
 * @param decimals the asset's own scale — how many decimal places its amounts are rendered with
 * @param networks the networks this asset lives on
 */
public record CurrencyInfo(
        @JsonProperty("currency") String currency,
        @JsonProperty("decimals") Integer decimals,
        @JsonProperty("networks") List<CurrencyNetwork> networks) {}
