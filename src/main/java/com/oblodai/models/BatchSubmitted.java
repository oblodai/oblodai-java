package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/payment/batch}, {@code /v1/refund/batch}, {@code /v1/payout/batch} and
 * {@code /v1/transfer/batch} acknowledgement — the batch was queued; poll
 * {@code /v1/batch/info} for its outcome.
 *
 * @param batchId the batch's identifier; pass it to {@code /v1/batch/info}
 * @param kind what the batch does ({@code payment}, {@code payout}, {@code refund},
 *     {@code transfer}, {@code payout_link}, …)
 * @param status the batch's lifecycle state ({@code queued}, {@code processing}, {@code done},
 *     {@code stopped}, …)
 * @param count how many elements were accepted into the batch
 */
public record BatchSubmitted(
        @JsonProperty("batch_id") String batchId,
        @JsonProperty("kind") String kind,
        @JsonProperty("status") String status,
        @JsonProperty("count") Integer count) {}
