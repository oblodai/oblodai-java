// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/link/info}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentLinkInfoRequest {

    /** Page size for the link's payments, 1–100; out of range falls back to 25. Example: {@code 25}. */
    @JsonProperty("limit")
    private Integer limit;

    /** Payment link identifier. Required. Example: {@code 5d3f2a71-9c84-4b0e-8d17-3e6a2c9f1b40}. */
    @JsonProperty("link_id")
    private String linkId;

    /** Offset within the link's payments. Example: {@code 0}. */
    @JsonProperty("offset")
    private Integer offset;

    /** Sets {@code limit}. */
    public PaymentLinkInfoRequest limit(Integer value) {
        this.limit = value;
        return this;
    }

    /** Current {@code limit}. */
    public Integer limit() {
        return limit;
    }

    /** Sets {@code link_id}. */
    public PaymentLinkInfoRequest linkId(String value) {
        this.linkId = value;
        return this;
    }

    /** Current {@code link_id}. */
    public String linkId() {
        return linkId;
    }

    /** Sets {@code offset}. */
    public PaymentLinkInfoRequest offset(Integer value) {
        this.offset = value;
        return this;
    }

    /** Current {@code offset}. */
    public Integer offset() {
        return offset;
    }

}
