package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/documents/jobs} and {@code /jobs/info} — an asynchronous report build.
 *
 * @param jobId the job's identifier; pass it to {@code /v1/documents/jobs/info}
 * @param kind what is being built ({@code statement}, {@code ledger}, …)
 * @param format the output format ({@code csv}, {@code pdf}, …)
 * @param lang the language the report is rendered in
 * @param status the job's lifecycle state ({@code queued}, {@code processing}, {@code done},
 *     {@code failed}, …)
 * @param period the reporting window
 * @param readyWithin human hint while queued (for example {@code 15s})
 * @param file set once done; the built report and where to fetch it
 * @param error human-readable failure text, on failed jobs
 * @param createdAt when the job was submitted, RFC 3339 UTC
 * @param updatedAt when the job last changed, RFC 3339 UTC
 */
public record DocumentJob(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("kind") String kind,
        @JsonProperty("format") String format,
        @JsonProperty("lang") String lang,
        @JsonProperty("status") String status,
        @JsonProperty("period") Period period,
        @JsonProperty("ready_within") String readyWithin,
        @JsonProperty("file") File file,
        @JsonProperty("error") String error,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {

    /**
     * The window a report covers, as calendar dates.
     *
     * @param from the first day covered, inclusive
     * @param to the last day covered, inclusive
     */
    public record Period(@JsonProperty("from") String from, @JsonProperty("to") String to) {}

    /**
     * The built report. {@code download_url} is a signed link; alternatively fetch it through
     * {@code documents.jobFile}.
     *
     * @param downloadUrl signed link to the built report
     * @param expiresAt when that link stops working, RFC 3339 UTC
     * @param rows how many rows the report holds
     * @param sizeBytes the report's size in bytes
     */
    public record File(
            @JsonProperty("download_url") String downloadUrl,
            @JsonProperty("expires_at") String expiresAt,
            @JsonProperty("rows") Integer rows,
            @JsonProperty("size_bytes") Long sizeBytes) {}
}
