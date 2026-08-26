// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/refund-fee-config/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutRefundFeeConfigSetRequest {

    /** true — the customer receives net (the customer pays the fee); false — the merchant pays the fee and the customer receives gross. Required. Example: {@code true}. */
    @JsonProperty("fee_on_customer")
    private Boolean feeOnCustomer;

    /** Sets {@code fee_on_customer}. */
    public PayoutRefundFeeConfigSetRequest feeOnCustomer(Boolean value) {
        this.feeOnCustomer = value;
        return this;
    }

    /** Current {@code fee_on_customer}. */
    public Boolean feeOnCustomer() {
        return feeOnCustomer;
    }

}
