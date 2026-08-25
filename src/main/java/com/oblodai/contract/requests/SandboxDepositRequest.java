// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/sandbox/deposit}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SandboxDepositRequest {

    /** Amount in the invoice currency; empty — pay exactly what is due, anything else is a way to produce an under/overpayment. Example: {@code 10}. */
    @JsonProperty("amount")
    private String amount;

    /** How many confirmations the deposit arrived with; 0 — fully confirmed; fewer than required — a way to test the pending→confirmed transition (repeat the same txid with a higher number). Example: {@code 0}. */
    @JsonProperty("confirmations")
    private Integer confirmations;

    /** UUID of the test invoice being "paid". Required. */
    @JsonProperty("invoice_id")
    private String invoiceId;

    /** Repeating the same txid tests your idempotency; empty — a new txid. */
    @JsonProperty("txid")
    private String txid;

    /** Sets {@code amount}. */
    public SandboxDepositRequest amount(String value) {
        this.amount = value;
        return this;
    }

    /** Current {@code amount}. */
    public String amount() {
        return amount;
    }

    /** Sets {@code confirmations}. */
    public SandboxDepositRequest confirmations(Integer value) {
        this.confirmations = value;
        return this;
    }

    /** Current {@code confirmations}. */
    public Integer confirmations() {
        return confirmations;
    }

    /** Sets {@code invoice_id}. */
    public SandboxDepositRequest invoiceId(String value) {
        this.invoiceId = value;
        return this;
    }

    /** Current {@code invoice_id}. */
    public String invoiceId() {
        return invoiceId;
    }

    /** Sets {@code txid}. */
    public SandboxDepositRequest txid(String value) {
        this.txid = value;
        return this;
    }

    /** Current {@code txid}. */
    public String txid() {
        return txid;
    }

}
