// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/split/config/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SplitConfigSetRequest {

    /** How many seconds to defer split settlement; range 0–7776000 (up to 90 days). 0 — send the shares immediately: you take on the risk that a refund becomes impossible. Required. Example: {@code 172800}. */
    @JsonProperty("refund_hold_seconds")
    private Integer refundHoldSeconds;

    /** Sets {@code refund_hold_seconds}. */
    public SplitConfigSetRequest refundHoldSeconds(Integer value) {
        this.refundHoldSeconds = value;
        return this;
    }

    /** Current {@code refund_hold_seconds}. */
    public Integer refundHoldSeconds() {
        return refundHoldSeconds;
    }

}
