// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/wallet}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WalletRequest {

    /** Symbol of the receiving currency (USDT, BTC, ETH, …). Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Receiving network (tron, ethereum, bitcoin, …). Required. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Your customer/order identifier. Pins a dedicated permanent address to the customer. Example: {@code client-42}. */
    @JsonProperty("order_id")
    private String orderId;

    /** Sets {@code currency}. */
    public WalletRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code network}. */
    public WalletRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public WalletRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code order_id}. */
    public WalletRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

}
