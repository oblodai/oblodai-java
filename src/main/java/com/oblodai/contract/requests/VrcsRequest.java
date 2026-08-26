// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/vrcs}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class VrcsRequest {

    /** true — enable auto-conversion of volatile deposits to USDT, false — disable; omit to read the current state. Example: {@code true}. */
    @JsonProperty("enabled")
    private Boolean enabled;

    /** Sets {@code enabled}. */
    public VrcsRequest enabled(Boolean value) {
        this.enabled = value;
        return this;
    }

    /** Current {@code enabled}. */
    public Boolean enabled() {
        return enabled;
    }

}
