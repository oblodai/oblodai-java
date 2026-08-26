// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/wallet/blocked-address-refund}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WalletBlockedAddressRefundRequest {

    /** Refund destination address. Required. */
    @JsonProperty("address")
    private String address;

    /** Destination tag/memo (XRP destination tag, XLM memo id, TON comment). Required for a classic address on a tag/memo network if the tag is not embedded in the X-/M-address. */
    @JsonProperty("memo")
    private String memo;

    /** Static wallet id (from the /v1/wallet response). Required. */
    @JsonProperty("uuid")
    private String uuid;

    /** Sets {@code address}. */
    public WalletBlockedAddressRefundRequest address(String value) {
        this.address = value;
        return this;
    }

    /** Current {@code address}. */
    public String address() {
        return address;
    }

    /** Sets {@code memo}. */
    public WalletBlockedAddressRefundRequest memo(String value) {
        this.memo = value;
        return this;
    }

    /** Current {@code memo}. */
    public String memo() {
        return memo;
    }

    /** Sets {@code uuid}. */
    public WalletBlockedAddressRefundRequest uuid(String value) {
        this.uuid = value;
        return this;
    }

    /** Current {@code uuid}. */
    public String uuid() {
        return uuid;
    }

}
