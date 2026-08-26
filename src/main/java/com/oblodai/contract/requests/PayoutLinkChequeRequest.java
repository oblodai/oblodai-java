// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/link/cheque}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutLinkChequeRequest {

    /** Claim secret from the payout link creation response. Stored only as a hash and never reissued — the cheque can be printed only while you still hold the token. Required. */
    @JsonProperty("claim_token")
    private String claimToken;

    /** Document language — one of the 41 supported codes (en by default); the full list is in the document.unknown_lang error. Example: {@code ru}. */
    @JsonProperty("lang")
    private String lang;

    /** Sets {@code claim_token}. */
    public PayoutLinkChequeRequest claimToken(String value) {
        this.claimToken = value;
        return this;
    }

    /** Current {@code claim_token}. */
    public String claimToken() {
        return claimToken;
    }

    /** Sets {@code lang}. */
    public PayoutLinkChequeRequest lang(String value) {
        this.lang = value;
        return this;
    }

    /** Current {@code lang}. */
    public String lang() {
        return lang;
    }

}
