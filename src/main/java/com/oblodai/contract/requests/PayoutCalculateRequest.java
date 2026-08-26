// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/calculate}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutCalculateRequest {

    /** Payout amount as a decimal string. Required. Example: {@code 10}. */
    @JsonProperty("amount")
    private String amount;

    /** Payout asset (USDT, BTC, …). Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** true — the fee is debited from the balance on top of the amount (the recipient gets exactly amount); false — the fee is taken out of the payout. */
    @JsonProperty("is_subtract")
    private Boolean isSubtract;

    /** Payout network; required when the asset lives on several networks. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Sets {@code amount}. */
    public PayoutCalculateRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code currency}. */
    public PayoutCalculateRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code is_subtract}. */
    public PayoutCalculateRequest isSubtract(Boolean value) {
        this.isSubtract = value;
        return this;
    }

    /** Current {@code is_subtract}. */
    public Boolean isSubtract() {
        return isSubtract;
    }

    /** Sets {@code network}. */
    public PayoutCalculateRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public PayoutCalculateRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

}
