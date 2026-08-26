// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/auto-withdraw/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AutoWithdrawSetRequest {

    /** Destination address (the merchant's external wallet). Required. Example: {@code TQrY8bkbpXKPt2LZbU8jqfnpFbUSF15sbx}. */
    @JsonProperty("address")
    private String address;

    /** Asset to withdraw automatically. Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Threshold: the sweep runs once the available balance of the asset reaches this amount; empty uses the network minimum. Example: {@code 100}. */
    @JsonProperty("min_amount")
    private String minAmount;

    /** Network of the destination address. Required. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Sets {@code address}. */
    public AutoWithdrawSetRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code currency}. */
    public AutoWithdrawSetRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code min_amount}. */
    public AutoWithdrawSetRequest minAmount(String value) {
        this.minAmount = value;
        return this;
    }

    /** Current {@code min_amount}. */
    public String minAmount() {
        return minAmount;
    }

    /** Sets {@code network}. */
    public AutoWithdrawSetRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public AutoWithdrawSetRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

}
