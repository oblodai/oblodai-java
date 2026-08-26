// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/split/rule/delete}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SplitRuleDeleteRequest {

    /** Rule identifier from POST /v1/split/rule or the list. Required. Example: {@code 9f4c1a2b-77de-4a55-9c1f-0e2b3d4a5f60}. */
    @JsonProperty("rule_id")
    private String ruleId;

    /** Sets {@code rule_id}. */
    public SplitRuleDeleteRequest ruleId(String value) {
        this.ruleId = value;
        return this;
    }

    /** Current {@code rule_id}. */
    public String ruleId() {
        return ruleId;
    }

}
