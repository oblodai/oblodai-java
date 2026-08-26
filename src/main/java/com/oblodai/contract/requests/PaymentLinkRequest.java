// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/link}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentLinkRequest {

    /** Amount — for fixed mode; required in this mode. Example: {@code 25.00}. */
    @JsonProperty("amount_fixed")
    private String amountFixed;

    /** Amount mode: fixed | open | range. Required. Example: {@code open}. */
    @JsonProperty("amount_mode")
    private String amountMode;

    /** Price currency — fiat (USD, EUR, RUB, …) or a coin; see pricing_currencies from GET /v1/currencies. Required. Example: {@code USD}. */
    @JsonProperty("currency")
    private String currency;

    /** Description on the payment page. */
    @JsonProperty("description")
    private String description;

    /** Link lifetime in seconds from creation; 0 (default) — the link never expires. */
    @JsonProperty("expires_in_seconds")
    private Integer expiresInSeconds;

    /** Upper bound — for range; required in this mode. Example: {@code 1000.00}. */
    @JsonProperty("max_amount")
    private String maxAmount;

    /** Lower bound: an optional floor for open, a required minimum for range. Example: {@code 1.00}. */
    @JsonProperty("min_amount")
    private String minAmount;

    /** Settlement currency (coin) pinned to the link; empty — the buyer chooses the coin. Example: {@code USDT}. */
    @JsonProperty("pinned_currency")
    private String pinnedCurrency;

    /** Settlement network pinned to the link; empty — the buyer chooses the network. Example: {@code tron}. */
    @JsonProperty("pinned_network")
    private String pinnedNetwork;

    /** Title on the payment page. */
    @JsonProperty("title")
    private String title;

    /** Sets {@code amount_fixed}. */
    public PaymentLinkRequest amountFixed(String value) {
        this.amountFixed = value;
        return this;
    }

    /** Current {@code amount_fixed}. */
    public String amountFixed() {
        return amountFixed;
    }

    /** Sets {@code amount_mode}. */
    public PaymentLinkRequest amountMode(String value) {
        this.amountMode = value;
        return this;
    }

    /** Sets {@code amount_mode} from the generated vocabulary. */
    public PaymentLinkRequest amountMode(AmountMode value) {
        this.amountMode = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code amount_mode}. */
    public String amountMode() {
        return amountMode;
    }

    /** Sets {@code currency}. */
    public PaymentLinkRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code description}. */
    public PaymentLinkRequest description(String value) {
        this.description = value;
        return this;
    }

    /** Current {@code description}. */
    public String description() {
        return description;
    }

    /** Sets {@code expires_in_seconds}. */
    public PaymentLinkRequest expiresInSeconds(Integer value) {
        this.expiresInSeconds = value;
        return this;
    }

    /** Current {@code expires_in_seconds}. */
    public Integer expiresInSeconds() {
        return expiresInSeconds;
    }

    /** Sets {@code max_amount}. */
    public PaymentLinkRequest maxAmount(String value) {
        this.maxAmount = value;
        return this;
    }

    /** Current {@code max_amount}. */
    public String maxAmount() {
        return maxAmount;
    }

    /** Sets {@code min_amount}. */
    public PaymentLinkRequest minAmount(String value) {
        this.minAmount = value;
        return this;
    }

    /** Current {@code min_amount}. */
    public String minAmount() {
        return minAmount;
    }

    /** Sets {@code pinned_currency}. */
    public PaymentLinkRequest pinnedCurrency(String value) {
        this.pinnedCurrency = value;
        return this;
    }

    /** Current {@code pinned_currency}. */
    public String pinnedCurrency() {
        return pinnedCurrency;
    }

    /** Sets {@code pinned_network}. */
    public PaymentLinkRequest pinnedNetwork(String value) {
        this.pinnedNetwork = value;
        return this;
    }

    /** Sets {@code pinned_network} from the generated vocabulary. */
    public PaymentLinkRequest pinnedNetwork(Network value) {
        this.pinnedNetwork = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code pinned_network}. */
    public String pinnedNetwork() {
        return pinnedNetwork;
    }

    /** Sets {@code title}. */
    public PaymentLinkRequest title(String value) {
        this.title = value;
        return this;
    }

    /** Current {@code title}. */
    public String title() {
        return title;
    }

}
