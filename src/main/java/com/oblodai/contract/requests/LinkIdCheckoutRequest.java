// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/link/{id}/checkout}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LinkIdCheckoutRequest {

    /** Amount entered by the buyer, in the link's price currency; required for open and range, ignored for fixed. Example: {@code 10.00}. */
    @JsonProperty("amount")
    private String amount;

    /** Settlement currency — the coin the buyer pays with; needed only if the link did not pin pinned_currency. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Settlement network; needed only if the link did not pin pinned_network. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Shop order number from the embedded widget (data-oblodai-order-id); carried over to the invoice and to the webhook for matching with the order; not an idempotency key. */
    @JsonProperty("order_id")
    private String orderId;

    /** Buyer email — the cheque is sent there automatically after payment. Example: {@code buyer@example.com}. */
    @JsonProperty("payer_email")
    private String payerEmail;

    /** Sets {@code amount}. */
    public LinkIdCheckoutRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code currency}. */
    public LinkIdCheckoutRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code network}. */
    public LinkIdCheckoutRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public LinkIdCheckoutRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code order_id}. */
    public LinkIdCheckoutRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code payer_email}. */
    public LinkIdCheckoutRequest payerEmail(String value) {
        this.payerEmail = value;
        return this;
    }

    /** Current {@code payer_email}. */
    public String payerEmail() {
        return payerEmail;
    }

}
