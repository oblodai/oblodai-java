package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/test-webhook/*} and {@code /v1/payment/testing-webhook} — what the receiver answered
 * to a synthetic delivery.
 *
 * @param ok whether the receiver accepted the delivery
 * @param signed whether the delivery carried a signature
 * @param statusCode the HTTP status the receiver answered with; absent when the receiver could not
 *     be reached (see {@code error})
 * @param error why the receiver could not be reached, when it could not
 * @param url where the delivery was posted; {@code /v1/payment/testing-webhook} only
 * @param durationMs how long the round trip took, in milliseconds;
 *     {@code /v1/payment/testing-webhook} only
 */
public record WebhookTestResult(
        @JsonProperty("ok") Boolean ok,
        @JsonProperty("signed") Boolean signed,
        @JsonProperty("status_code") Integer statusCode,
        @JsonProperty("error") String error,
        @JsonProperty("url") String url,
        @JsonProperty("duration_ms") Long durationMs) {}
