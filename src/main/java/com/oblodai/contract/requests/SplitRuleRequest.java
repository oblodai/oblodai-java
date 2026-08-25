// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/split/rule}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SplitRuleRequest {

    /** External crypto address of the partner; the share leaves as a real on-chain transaction — irreversible. Exactly one recipient option: either address+network or merchant_id. */
    @JsonProperty("address")
    private String address;

    /** Id of the partner merchant inside Oblodai; the share moves through internal accounting and is clawed back on a refund. Example: {@code b4c1f0e2-5a77-4d31-9f08-2c6e7a1b3d94}. */
    @JsonProperty("merchant_id")
    private String merchantId;

    /** Address network. Required together with address. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Comment for yourself (shown in the rule list). */
    @JsonProperty("note")
    private String note;

    /** Share of every payment, as a string: "10" = 10 %, "2.5" = 2.5 %. Greater than 0 and at most 100, step 0.01 %; the sum of all rules cannot exceed 100 %. Required. Example: {@code 10}. */
    @JsonProperty("percent")
    private String percent;

    /** Sets {@code address}. */
    public SplitRuleRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code merchant_id}. */
    public SplitRuleRequest merchantId(String value) {
        this.merchantId = value;
        return this;
    }

    /** Current {@code merchant_id}. */
    public String merchantId() {
        return merchantId;
    }

    /** Sets {@code network}. */
    public SplitRuleRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public SplitRuleRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code note}. */
    public SplitRuleRequest note(String value) {
        this.note = value;
        return this;
    }

    /** Current {@code note}. */
    public String note() {
        return note;
    }

    /** Sets {@code percent}. */
    public SplitRuleRequest percent(String value) {
        this.percent = value;
        return this;
    }

    /** Current {@code percent}. */
    public String percent() {
        return percent;
    }

}
