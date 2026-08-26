// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/send-email}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentSendEmailRequest {

    /** Where to send it. Defaults to the payer_email set on the payment. Example: {@code buyer@example.com}. */
    @JsonProperty("email")
    private String email;

    /** Your order reference. Example: {@code order-1}. */
    @JsonProperty("order_id")
    private String orderId;

    /** Payment id in Oblodai. Either uuid or order_id is required. */
    @JsonProperty("uuid")
    private String uuid;

    /** Sets {@code email}. */
    public PaymentSendEmailRequest email(String value) {
        this.email = value;
        return this;
    }

    /** Current {@code email}. */
    public String email() {
        return email;
    }

    /** Sets {@code order_id}. */
    public PaymentSendEmailRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code uuid}. */
    public PaymentSendEmailRequest uuid(String value) {
        this.uuid = value;
        return this;
    }

    /** Current {@code uuid}. */
    public String uuid() {
        return uuid;
    }

}
