// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/resolve}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentResolveRequest {

    /** accept — accept the partial payment, refund — return it to the payer. Required. Example: {@code accept}. One of "accept" | "refund". */
    @JsonProperty("action")
    private String action;

    /** refund only: refund address. Defaults to the payment's recorded payer_address; if it is empty (Bitcoin/UTXO) the address is required, otherwise refund.no_address. */
    @JsonProperty("address")
    private String address;

    /** refund only: refund network, defaults to the payment network. */
    @JsonProperty("network")
    private String network;

    /** Your payment identifier. Example: {@code ord-1001}. */
    @JsonProperty("order_id")
    private String orderId;

    /** refund only: your refund deduplication key. */
    @JsonProperty("reference")
    private String reference;

    /** Payment UUID. Either uuid or order_id is required. */
    @JsonProperty("uuid")
    private String uuid;

    /** Sets {@code action}. */
    public PaymentResolveRequest action(String value) {
        this.action = value;
        return this;
    }

    /** Current {@code action}. */
    public String action() {
        return action;
    }

    /** Sets {@code address}. */
    public PaymentResolveRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code network}. */
    public PaymentResolveRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public PaymentResolveRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code order_id}. */
    public PaymentResolveRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code reference}. */
    public PaymentResolveRequest reference(String value) {
        this.reference = value;
        return this;
    }

    /** Current {@code reference}. */
    public String reference() {
        return reference;
    }

    /** Sets {@code uuid}. */
    public PaymentResolveRequest uuid(String value) {
        this.uuid = value;
        return this;
    }

    /** Current {@code uuid}. */
    public String uuid() {
        return uuid;
    }

}
