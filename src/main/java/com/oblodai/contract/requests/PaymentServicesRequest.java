// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/services}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentServicesRequest {

    /** Page size, 1–100; out of range falls back to 25. Example: {@code 25}. */
    @JsonProperty("limit")
    private Integer limit;

    /** Offset from the start of the list (newest first). Example: {@code 0}. */
    @JsonProperty("offset")
    private Integer offset;

    /** Sets {@code limit}. */
    public PaymentServicesRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code offset}. */
    public PaymentServicesRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

}
