package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.BatchOnError;
import java.util.List;

/**
 * {@code /v1/batch/info} — the state of an asynchronous batch and every element in it.
 *
 * @param batchId the batch's identifier
 * @param kind what the batch does ({@code payment}, {@code payout}, {@code refund},
 *     {@code transfer}, {@code payout_link}, …)
 * @param status the batch's lifecycle state ({@code queued}, {@code processing}, {@code done},
 *     {@code stopped}, …)
 * @param onError what the batch was told to do when an element fails
 * @param total how many elements the batch holds
 * @param succeeded how many elements succeeded so far
 * @param failed how many elements failed so far
 * @param items the elements themselves, with their individual outcomes
 * @param createdAt when the batch was submitted, RFC 3339 UTC
 * @param updatedAt when the batch last changed, RFC 3339 UTC
 */
public record BatchInfo(
        @JsonProperty("batch_id") String batchId,
        @JsonProperty("kind") String kind,
        @JsonProperty("status") String status,
        @JsonProperty("on_error") BatchOnError onError,
        @JsonProperty("total") Integer total,
        @JsonProperty("succeeded") Integer succeeded,
        @JsonProperty("failed") Integer failed,
        @JsonProperty("items") List<BatchInfoItem> items,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {}
