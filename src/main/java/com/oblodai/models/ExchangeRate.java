package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/exchange-rate/list} item: 1 {@code from} = {@code course} {@code to}.
 *
 * @param from the base currency — the one unit being quoted
 * @param to the quote currency the base is priced in
 * @param course the rate, a decimal string; never a float
 */
public record ExchangeRate(
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("course") String course) {}
