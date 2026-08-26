// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/pay/{id}/select}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayIdSelectRequest {

    /** Selected payment currency. Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Selected network. Required. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Sets {@code currency}. */
    public PayIdSelectRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code network}. */
    public PayIdSelectRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public PayIdSelectRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

}
