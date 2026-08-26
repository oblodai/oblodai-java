// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/link/cancel}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutLinkCancelRequest {

    /** Payout link id (link_id from the creation response). Required. */
    @JsonProperty("link_id")
    private String linkId;

    /** Sets {@code link_id}. */
    public PayoutLinkCancelRequest linkId(String value) {
        this.linkId = value;
        return this;
    }

    /** Current {@code link_id}. */
    public String linkId() {
        return linkId;
    }

}
