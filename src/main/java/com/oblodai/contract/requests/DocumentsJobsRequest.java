// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/documents/jobs}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DocumentsJobsRequest {

    /** File format: pdf (default) or csv. CSV is generated without layout — cheaper for large statements and loads into Excel/1C. Example: {@code csv}. */
    @JsonProperty("format")
    private String format;

    /** Start of the period, YYYY-MM-DD (defaults to the first day of the current month). Example: {@code 2025-01-01}. */
    @JsonProperty("from")
    private String from;

    /** Report type: statement (operations), fees (commissions) or ledger (balance movements). Required. Example: {@code statement}. */
    @JsonProperty("kind")
    private String kind;

    /** Document language (default en). Example: {@code ru}. */
    @JsonProperty("lang")
    private String lang;

    /** End of the period, inclusive, YYYY-MM-DD (defaults to today). The period may span up to two years. Example: {@code 2026-08-19}. */
    @JsonProperty("to")
    private String to;

    /** Sets {@code format}. */
    public DocumentsJobsRequest format(String value) {
        this.format = value;
        return this;
    }

    /** Current {@code format}. */
    public String format() {
        return format;
    }

    /** Sets {@code from}. */
    public DocumentsJobsRequest from(String value) {
        this.from = value;
        return this;
    }

    /** Current {@code from}. */
    public String from() {
        return from;
    }

    /** Sets {@code kind}. */
    public DocumentsJobsRequest kind(String value) {
        this.kind = value;
        return this;
    }

    /** Current {@code kind}. */
    public String kind() {
        return kind;
    }

    /** Sets {@code lang}. */
    public DocumentsJobsRequest lang(String value) {
        this.lang = value;
        return this;
    }

    /** Current {@code lang}. */
    public String lang() {
        return lang;
    }

    /** Sets {@code to}. */
    public DocumentsJobsRequest to(String value) {
        this.to = value;
        return this;
    }

    /** Current {@code to}. */
    public String to() {
        return to;
    }

}
