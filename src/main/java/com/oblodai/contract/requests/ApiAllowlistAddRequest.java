// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/api-allowlist/add}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiAllowlistAddRequest {

    /** IP or subnet in CIDR notation (203.0.113.7 or 203.0.113.0/24). Required. Example: {@code 203.0.113.0/24}. */
    @JsonProperty("cidr")
    private String cidr;

    /** Sets {@code cidr}. */
    public ApiAllowlistAddRequest cidr(String value) {
        this.cidr = value;
        return this;
    }

    /** Current {@code cidr}. */
    public String cidr() {
        return cidr;
    }

}
