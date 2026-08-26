// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/discount/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentDiscountSetRequest {

    /** Currency. Empty = global default for all coins. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Percentage, from -99 to 99. Positive is a discount, negative is a markup. Required. Example: {@code 3}. */
    @JsonProperty("discount_percent")
    private Integer discountPercent;

    /** Network. Empty = any network of this currency. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Sets {@code currency}. */
    public PaymentDiscountSetRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code discount_percent}. */
    public PaymentDiscountSetRequest discountPercent(Integer value) {
        this.discountPercent = value;
        return this;
    }

    /** Current {@code discount_percent}. */
    public Integer discountPercent() {
        return discountPercent;
    }

    /** Sets {@code network}. */
    public PaymentDiscountSetRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public PaymentDiscountSetRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

}
