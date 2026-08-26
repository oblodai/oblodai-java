// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/exchange-rate/list}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ExchangeRateListRequest {

    /** Currency code. If set, only its rate is returned. If empty or the body is {}, rates for all currencies are returned. Example: {@code ETH}. */
    @JsonProperty("currency_from")
    private String currencyFrom;

    /** Quote currency: USDT by default; any pricing asset, including fiats with a direct feed (EUR, RUB, …). */
    @JsonProperty("currency_to")
    private String currencyTo;

    /** Page size, 1–100; default 25. */
    @JsonProperty("limit")
    private Integer limit;

    /** Offset from the start of the list; default 0. */
    @JsonProperty("offset")
    private Integer offset;

    /** Sets {@code currency_from}. */
    public ExchangeRateListRequest currencyFrom(String value) {
        this.currencyFrom = value;
        return this;
    }

    /** Current {@code currency_from}. */
    public String currencyFrom() {
        return currencyFrom;
    }

    /** Sets {@code currency_to}. */
    public ExchangeRateListRequest currencyTo(String value) {
        this.currencyTo = value;
        return this;
    }

    /** Current {@code currency_to}. */
    public String currencyTo() {
        return currencyTo;
    }

    /** Sets {@code limit}. */
    public ExchangeRateListRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code offset}. */
    public ExchangeRateListRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

}
