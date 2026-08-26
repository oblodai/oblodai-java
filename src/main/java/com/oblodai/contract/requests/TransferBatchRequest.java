// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/transfer/batch}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TransferBatchRequest {

    /** What to do when an item fails: continue (default) — process the rest; stop — halt processing after the first error. Example: {@code continue}. */
    @JsonProperty("on_error")
    private String onError;

    /** Array of 1 to 5000 items — the same fields as POST /v1/transfer/to-user; every item requires order_id (idempotency key) and to_user_id (user UUID). */
    @JsonProperty("transfers")
    private List<Transfer> transfers;

    /** Sets {@code on_error}. */
    public TransferBatchRequest onError(String value) {
        this.onError = value;
        return this;
    }

    /** Sets {@code on_error} from the generated vocabulary. */
    public TransferBatchRequest onError(BatchOnError value) {
        this.onError = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code on_error}. */
    public String onError() {
        return onError;
    }

    /** Sets {@code transfers}. */
    public TransferBatchRequest transfers(List<Transfer> value) {
        this.transfers = value;
        return this;
    }

    /** Current {@code transfers}. */
    public List<Transfer> transfers() {
        return transfers;
    }

    public static final class Transfer {

        /** Transfer amount in currency. Required. Example: {@code 50}. */
        @JsonProperty("amount")
        private String amount;

        /** Currency code (cryptocurrency). Required. Example: {@code USDT}. */
        @JsonProperty("currency")
        private String currency;

        /** Idempotency key: a repeat with the same order_id is a no-op; required in a transfer batch. Required. */
        @JsonProperty("order_id")
        private String orderId;

        /** Platform user id of the recipient (UUID, not username); a username is resolved to an id via the cabinet public profile /public/users/{username}. Required. */
        @JsonProperty("to_user_id")
        private String toUserId;

        /** Sets {@code amount}. */
        public Transfer amount(String value) {
            this.amount = value;
            return this;
        }

        /** Current {@code amount}. */
        public String amount() {
            return amount;
        }

        /** Sets {@code currency}. */
        public Transfer currency(String value) {
            this.currency = value;
            return this;
        }

        /** Current {@code currency}. */
        public String currency() {
            return currency;
        }

        /** Sets {@code order_id}. */
        public Transfer orderId(String value) {
            this.orderId = value;
            return this;
        }

        /** Current {@code order_id}. */
        public String orderId() {
            return orderId;
        }

        /** Sets {@code to_user_id}. */
        public Transfer toUserId(String value) {
            this.toUserId = value;
            return this;
        }

        /** Current {@code to_user_id}. */
        public String toUserId() {
            return toUserId;
        }

    }
}
