// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/approve}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutApproveRequest {

    /** Payout id. Required. */
    @JsonProperty("uuid")
    private String uuid;

    /** Sets {@code uuid}. */
    public PayoutApproveRequest uuid(String value) {
        this.uuid = value;
        return this;
    }

    /** Current {@code uuid}. */
    public String uuid() {
        return uuid;
    }

}
