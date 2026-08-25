package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/autorefund/*} — whether mispayments are sent back automatically.
 *
 * @param overpay whether overpayments are refunded automatically
 * @param underpay whether underpayments are refunded automatically
 * @param configured {@code get} only: whether the merchant ever set it
 */
public record AutoRefundConfig(
        @JsonProperty("overpay") Boolean overpay,
        @JsonProperty("underpay") Boolean underpay,
        @JsonProperty("configured") Boolean configured) {}
