// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/wallet/block}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WalletBlockRequest {

    /** Static wallet address. Required. Example: {@code TXk9...c3Fd}. */
    @JsonProperty("address")
    private String address;

    /** true — block (the default when the field is omitted); false — unblock. */
    @JsonProperty("is_force_block")
    private Boolean isForceBlock;

    /** Sets {@code address}. */
    public WalletBlockRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code is_force_block}. */
    public WalletBlockRequest isForceBlock(Boolean value) {
        this.isForceBlock = value;
        return this;
    }

    /** Current {@code is_force_block}. */
    public Boolean isForceBlock() {
        return isForceBlock;
    }

}
