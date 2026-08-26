// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/transfer/to-user}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TransferToUserRequest {

    /** Transfer amount in currency. Required. Example: {@code 50}. */
    @JsonProperty("amount")
    private String amount;

    /** Currency code (cryptocurrency). Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Idempotency key: a repeat with the same order_id is a no-op; required in a transfer batch. */
    @JsonProperty("order_id")
    private String orderId;

    /** Platform user id of the recipient (UUID, not username); a username is resolved to an id via the cabinet public profile /public/users/{username}. Required. */
    @JsonProperty("to_user_id")
    private String toUserId;

    /** Sets {@code amount}. */
    public TransferToUserRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code currency}. */
    public TransferToUserRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code order_id}. */
    public TransferToUserRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code to_user_id}. */
    public TransferToUserRequest toUserId(String value) {
        this.toUserId = value;
        return this;
    }

    /** Current {@code to_user_id}. */
    public String toUserId() {
        return toUserId;
    }

}
