// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/fee-config/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutFeeConfigSetRequest {

    /** true — the recipient pays the network fee (receives less); false — the merchant bears the fee. Required. Example: {@code true}. */
    @JsonProperty("fee_on_recipient")
    private Boolean feeOnRecipient;

    /** Sets {@code fee_on_recipient}. */
    public PayoutFeeConfigSetRequest feeOnRecipient(Boolean value) {
        this.feeOnRecipient = value;
        return this;
    }

    /** Current {@code fee_on_recipient}. */
    public Boolean feeOnRecipient() {
        return feeOnRecipient;
    }

}
