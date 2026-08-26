// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/claim/{token}}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ClaimTokenRequest {

    /** Recipient address in the payout network. Required. */
    @JsonProperty("address")
    private String address;

    /** Memo/tag — only for networks where it is required. */
    @JsonProperty("memo")
    private String memo;

    /** Claim code — if the sender set one on the link. After 10 incorrect attempts the link is locked. */
    @JsonProperty("passcode")
    private String passcode;

    /** Sets {@code address}. */
    public ClaimTokenRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code memo}. */
    public ClaimTokenRequest memo(String value) {
        this.memo = value;
        return this;
    }

    /** Current {@code memo}. */
    public String memo() {
        return memo;
    }

    /** Sets {@code passcode}. */
    public ClaimTokenRequest passcode(String value) {
        this.passcode = value;
        return this;
    }

    /** Current {@code passcode}. */
    public String passcode() {
        return passcode;
    }

}
