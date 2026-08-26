// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/fee-config/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentFeeConfigSetRequest {

    /** Share of OUR commission paid by the buyer: 0 — the merchant pays (current behaviour), 100 — the buyer pays and the invoice is issued with a markup. Applies to invoices created AFTER the change. Required. Example: {@code 100}. */
    @JsonProperty("payer_pays_percent")
    private Integer payerPaysPercent;

    /** Sets {@code payer_pays_percent}. */
    public PaymentFeeConfigSetRequest payerPaysPercent(Integer value) {
        this.payerPaysPercent = value;
        return this;
    }

    /** Current {@code payer_pays_percent}. */
    public Integer payerPaysPercent() {
        return payerPaysPercent;
    }

}
