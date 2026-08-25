// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/transfer/to-personal}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TransferToPersonalRequest {

    /** Transfer amount in currency. Required. Example: {@code 50}. */
    @JsonProperty("amount")
    private String amount;

    /** Currency code (cryptocurrency). Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Idempotency key: a repeat with the same order_id is a no-op. Always send it, otherwise retrying the request after a network timeout creates a second transfer. Example: {@code transfer-1}. */
    @JsonProperty("order_id")
    private String orderId;

    /** Sets {@code amount}. */
    public TransferToPersonalRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code currency}. */
    public TransferToPersonalRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code order_id}. */
    public TransferToPersonalRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

}
