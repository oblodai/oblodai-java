// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/link/toggle}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentLinkToggleRequest {

    /** true — the link accepts payments; false — disabled (the page shows the link as inactive). Required. Example: {@code false}. */
    @JsonProperty("active")
    private Boolean active;

    /** Payment link identifier. Required. Example: {@code 5d3f2a71-9c84-4b0e-8d17-3e6a2c9f1b40}. */
    @JsonProperty("link_id")
    private String linkId;

    /** Sets {@code active}. */
    public PaymentLinkToggleRequest active(Boolean value) {
        this.active = value;
        return this;
    }

    /** Current {@code active}. */
    public Boolean active() {
        return active;
    }

    /** Sets {@code link_id}. */
    public PaymentLinkToggleRequest linkId(String value) {
        this.linkId = value;
        return this;
    }

    /** Current {@code link_id}. */
    public String linkId() {
        return linkId;
    }

}
