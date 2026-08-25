// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/sandbox/webhooks/replay}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SandboxWebhooksReplayRequest {

    /** Delivery id from GET /v1/sandbox/webhooks. Required. */
    @JsonProperty("delivery_id")
    private String deliveryId;

    /** Sets {@code delivery_id}. */
    public SandboxWebhooksReplayRequest deliveryId(String value) {
        this.deliveryId = value;
        return this;
    }

    /** Current {@code delivery_id}. */
    public String deliveryId() {
        return deliveryId;
    }

}
