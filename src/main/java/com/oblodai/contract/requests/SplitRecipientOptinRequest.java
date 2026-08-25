// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/split/recipient/optin}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SplitRecipientOptinRequest {

    /** Allow other merchants to route split shares to your balance. true — enable receiving, false — disable (new rules targeting you stop being created; existing ones keep executing). Required. Example: {@code true}. */
    @JsonProperty("enabled")
    private Boolean enabled;

    /** Sets {@code enabled}. */
    public SplitRecipientOptinRequest enabled(Boolean value) {
        this.enabled = value;
        return this;
    }

    /** Current {@code enabled}. */
    public Boolean enabled() {
        return enabled;
    }

}
