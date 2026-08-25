package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Element of every batch listing ({@code /v1/payout/mass}, {@code /v1/payout/link/batch},
 * {@code /v1/batch/info}).
 *
 * <p>Every element reports its own outcome: {@code ok} tells success from failure, and the failing
 * ones carry {@code message}, {@code error_code} and the {@code http_status} the single-item call
 * would have answered with.
 *
 * @param <T> the payload the gateway returns for a successful element (a payout, a payout link, …)
 * @param idx zero-based position of this element in the submitted batch
 * @param ok whether this element succeeded
 * @param orderId the merchant reference of this element, echoed back when one was sent
 * @param result the created object, present only when {@code ok} is true
 * @param message human-readable failure text, present only when {@code ok} is false
 * @param errorCode machine-readable failure code, present only when {@code ok} is false
 * @param httpStatus the HTTP status the equivalent single-item call would have answered with
 */
public record BatchElement<T>(
        @JsonProperty("idx") Integer idx,
        @JsonProperty("ok") Boolean ok,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("result") T result,
        @JsonProperty("message") String message,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("http_status") Integer httpStatus) {}
