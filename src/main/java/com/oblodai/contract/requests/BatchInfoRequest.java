// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/batch/info}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BatchInfoRequest {

    /** Batch id from the submit response. Required. Example: {@code 9f4c1a2b-77de-4a55-9c1f-0e2b3d4a5f60}. */
    @JsonProperty("batch_id")
    private String batchId;

    /** How many items to return in items (pagination). Example: {@code 100}. */
    @JsonProperty("limit")
    private Integer limit;

    /** Offset over items. Example: {@code 0}. */
    @JsonProperty("offset")
    private Integer offset;

    /** Sets {@code batch_id}. */
    public BatchInfoRequest batchId(String value) {
        this.batchId = value;
        return this;
    }

    /** Current {@code batch_id}. */
    public String batchId() {
        return batchId;
    }

    /** Sets {@code limit}. */
    public BatchInfoRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code offset}. */
    public BatchInfoRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

}
