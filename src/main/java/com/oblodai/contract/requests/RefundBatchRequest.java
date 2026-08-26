// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/refund/batch}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RefundBatchRequest {

    /** What to do when an item fails: continue (default) — process the rest; stop — halt processing after the first error. Example: {@code continue}. */
    @JsonProperty("on_error")
    private String onError;

    /** Array of 1 to 5000 items — the same fields as POST /v1/payment/refund; every item requires reference (idempotency key) and either uuid or order_id of the payment. Required. */
    @JsonProperty("refunds")
    private List<Refund> refunds;

    /** Sets {@code on_error}. */
    public RefundBatchRequest onError(String value) {
        this.onError = value;
        return this;
    }

    /** Sets {@code on_error} from the generated vocabulary. */
    public RefundBatchRequest onError(BatchOnError value) {
        this.onError = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code on_error}. */
    public String onError() {
        return onError;
    }

    /** Sets {@code refunds}. */
    public RefundBatchRequest refunds(List<Refund> value) {
        this.refunds = value;
        return this;
    }

    /** Current {@code refunds}. */
    public List<Refund> refunds() {
        return refunds;
    }

    public static final class Refund {

        /** Refund destination address. Defaults to the payment's payer_address; required only for Bitcoin/UTXO. */
        @JsonProperty("address")
        private String address;

        /** Partial amount. Defaults to the full amount received. Example: {@code 10}. */
        @JsonProperty("amount")
        private String amount;

        /** Network. Example: {@code tron}. */
        @JsonProperty("network")
        private String network;

        /** Your order reference for the payment. Either uuid or order_id is required. Example: {@code order-1}. */
        @JsonProperty("order_id")
        private String orderId;

        /** Optional refund idempotency key: distinguishes two different refunds with the same (payment, address, amount); a repeat with the same value is deduplicated. This is not order_id. Required. */
        @JsonProperty("reference")
        private String reference;

        /** Payment id. Either uuid or order_id is required. */
        @JsonProperty("uuid")
        private String uuid;

        /** Sets {@code address}. */
        public Refund address(String value) {
            this.address = value;
            return this;
        }

        /** Current {@code address}. */
        public String address() {
            return address;
        }

        /** Sets {@code amount}. */
        public Refund amount(String value) {
            this.amount = value;
            return this;
        }

        /** Current {@code amount}. */
        public String amount() {
            return amount;
        }

        /** Sets {@code network}. */
        public Refund network(String value) {
            this.network = value;
            return this;
        }

        /** Sets {@code network} from the generated vocabulary. */
        public Refund network(Network value) {
            this.network = value == null ? null : value.wire();
            return this;
        }

        /** Current {@code network}. */
        public String network() {
            return network;
        }

        /** Sets {@code order_id}. */
        public Refund orderId(String value) {
            this.orderId = value;
            return this;
        }

        /** Current {@code order_id}. */
        public String orderId() {
            return orderId;
        }

        /** Sets {@code reference}. */
        public Refund reference(String value) {
            this.reference = value;
            return this;
        }

        /** Current {@code reference}. */
        public String reference() {
            return reference;
        }

        /** Sets {@code uuid}. */
        public Refund uuid(String value) {
            this.uuid = value;
            return this;
        }

        /** Current {@code uuid}. */
        public String uuid() {
            return uuid;
        }

    }
}
