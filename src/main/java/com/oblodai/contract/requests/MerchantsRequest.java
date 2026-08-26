// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/merchants}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MerchantsRequest {

    /** Owner email; must be unique across merchants. Required. Example: {@code owner@shop.example}. */
    @JsonProperty("email")
    private String email;

    /** Display name of the merchant. Example: {@code Acme}. */
    @JsonProperty("name")
    private String name;

    /** Sets {@code email}. */
    public MerchantsRequest email(String value) {
        this.email = value;
        return this;
    }

    /** Current {@code email}. */
    public String email() {
        return email;
    }

    /** Sets {@code name}. */
    public MerchantsRequest name(String value) {
        this.name = value;
        return this;
    }

    /** Current {@code name}. */
    public String name() {
        return name;
    }

}
