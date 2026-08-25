// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/accuracy/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentAccuracySetRequest {

    /** Tolerance in percent, 1–5. Required when enabled: true; ignored when enabled: false (reset to 0). Capped at 5 %. Example: {@code 2}. */
    @JsonProperty("accuracy_percent")
    private Integer accuracyPercent;

    /** Enable/disable the tolerance. Required. Example: {@code true}. */
    @JsonProperty("enabled")
    private Boolean enabled;

    /** Sets {@code accuracy_percent}. */
    public PaymentAccuracySetRequest accuracyPercent(Integer value) {
        this.accuracyPercent = value;
        return this;
    }

    /** Current {@code accuracy_percent}. */
    public Integer accuracyPercent() {
        return accuracyPercent;
    }

    /** Sets {@code enabled}. */
    public PaymentAccuracySetRequest enabled(Boolean value) {
        this.enabled = value;
        return this;
    }

    /** Current {@code enabled}. */
    public Boolean enabled() {
        return enabled;
    }

}
