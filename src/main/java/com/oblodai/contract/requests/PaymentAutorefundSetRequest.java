// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/autorefund/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentAutorefundSetRequest {

    /** Refund the excess on overpayment (paid_over). Required. Example: {@code true}. */
    @JsonProperty("overpay")
    private Boolean overpay;

    /** Refund the funds on an expired underpayment (wrong_amount). Required. Example: {@code true}. */
    @JsonProperty("underpay")
    private Boolean underpay;

    /** Sets {@code overpay}. */
    public PaymentAutorefundSetRequest overpay(Boolean value) {
        this.overpay = value;
        return this;
    }

    /** Current {@code overpay}. */
    public Boolean overpay() {
        return overpay;
    }

    /** Sets {@code underpay}. */
    public PaymentAutorefundSetRequest underpay(Boolean value) {
        this.underpay = value;
        return this;
    }

    /** Current {@code underpay}. */
    public Boolean underpay() {
        return underpay;
    }

}
