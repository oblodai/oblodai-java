// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/link/list}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutLinkListRequest {

    /** How many links to return per page. Example: {@code 50}. */
    @JsonProperty("limit")
    private Integer limit;

    /** Offset from the start of the list (paging). Example: {@code 0}. */
    @JsonProperty("offset")
    private Integer offset;

    /** Sets {@code limit}. */
    public PayoutLinkListRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code offset}. */
    public PayoutLinkListRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

}
