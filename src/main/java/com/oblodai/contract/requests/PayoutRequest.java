// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutRequest {

    /** Recipient address. Required. */
    @JsonProperty("address")
    private String address;

    /** Payout amount, in currency. Required. Example: {@code 25}. */
    @JsonProperty("amount")
    private String amount;

    /** Currency code (for example USDT). Required. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Fund the payout by converting the balance. USDT → currency only. Example: {@code USDT}. */
    @JsonProperty("from_currency")
    private String fromCurrency;

    /** Who pays the network fee: true — amount+fee is debited from the balance and the recipient receives amount; false — the recipient receives amount-fee; not provided — the project fee-config. */
    @JsonProperty("is_subtract")
    private Boolean isSubtract;

    /** Destination tag/memo (TON Jetton). Maximum 120 characters. */
    @JsonProperty("memo")
    private String memo;

    /** Network (tron, ethereum, …). Required for coins with several networks. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Your payout number; idempotency key. Required. Example: {@code payout-1}. */
    @JsonProperty("order_id")
    private String orderId;

    /** Origin label: api (default) or manual. */
    @JsonProperty("source")
    private String source;

    /** Custom webhook URL for this payout (passes the SSRF check). Requires a registered endpoint (POST /v1/webhooks): delivery is signed with its secret. */
    @JsonProperty("url_callback")
    private String urlCallback;

    /** Sets {@code address}. */
    public PayoutRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code amount}. */
    public PayoutRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code currency}. */
    public PayoutRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code from_currency}. */
    public PayoutRequest fromCurrency(String value) {
        this.fromCurrency = value;
        return this;
    }

    /** Current {@code from_currency}. */
    public String fromCurrency() {
        return fromCurrency;
    }

    /** Sets {@code is_subtract}. */
    public PayoutRequest isSubtract(Boolean value) {
        this.isSubtract = value;
        return this;
    }

    /** Current {@code is_subtract}. */
    public Boolean isSubtract() {
        return isSubtract;
    }

    /** Sets {@code memo}. */
    public PayoutRequest memo(String value) {
        this.memo = value;
        return this;
    }

    /** Current {@code memo}. */
    public String memo() {
        return memo;
    }

    /** Sets {@code network}. */
    public PayoutRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public PayoutRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code order_id}. */
    public PayoutRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code source}. */
    public PayoutRequest source(String value) {
        this.source = value;
        return this;
    }

    /** Current {@code source}. */
    public String source() {
        return source;
    }

    /** Sets {@code url_callback}. */
    public PayoutRequest urlCallback(String value) {
        this.urlCallback = value;
        return this;
    }

    /** Current {@code url_callback}. */
    public String urlCallback() {
        return urlCallback;
    }

}
