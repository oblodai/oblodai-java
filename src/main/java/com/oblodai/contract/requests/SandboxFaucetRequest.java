// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/sandbox/faucet}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SandboxFaucetRequest {

    /** Amount of test money, as a string; capped at 1000000 per call. Required. Example: {@code 1000}. */
    @JsonProperty("amount")
    private String amount;

    /** Top-up asset (USDT, BTC, …). Required. Example: {@code USDT}. */
    @JsonProperty("asset")
    private String asset;

    /** Safe-retry key; empty — every call creates a new top-up. */
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    /** Sets {@code amount}. */
    public SandboxFaucetRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code asset}. */
    public SandboxFaucetRequest asset(String value) {
        this.asset = value;
        return this;
    }

    /** Current {@code asset}. */
    public String asset() {
        return asset;
    }

    /** Sets {@code idempotency_key}. */
    public SandboxFaucetRequest idempotencyKey(String value) {
        this.idempotencyKey = value;
        return this;
    }

    /** Current {@code idempotency_key}. */
    public String idempotencyKey() {
        return idempotencyKey;
    }

}
