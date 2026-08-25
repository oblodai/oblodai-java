// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/history}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutHistoryRequest {

    /** payout — ordinary payouts, refund — refunds; empty returns both. Example: {@code payout}. One of "payout" | "refund". */
    @JsonProperty("kind")
    private String kind;

    /** Page size, 1–100; out of range falls back to 25. Example: {@code 25}. */
    @JsonProperty("limit")
    private Integer limit;

    /** Offset from the start of the list (newest first). Example: {@code 0}. */
    @JsonProperty("offset")
    private Integer offset;

    /** Filter by status (an exact value from the status vocabulary); empty returns all. Example: {@code paid}. One of PayoutStatus. */
    @JsonProperty("status")
    private String status;

    /** Sets {@code kind}. */
    public PayoutHistoryRequest kind(String value) {
        this.kind = value;
        return this;
    }

    /** Current {@code kind}. */
    public String kind() {
        return kind;
    }

    /** Sets {@code limit}. */
    public PayoutHistoryRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code offset}. */
    public PayoutHistoryRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

    /** Sets {@code status}. */
    public PayoutHistoryRequest status(String value) {
        this.status = value;
        return this;
    }

    /** Current {@code status}. */
    public String status() {
        return status;
    }

}
