// GENERATED FILE — do not edit. Source: contract/contract.json (core 7b8eb828b9ec).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/documents/jobs/info}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DocumentsJobsInfoRequest {

    /** Job id from the creation response. Required. */
    @JsonProperty("job_id")
    private String jobId;

    /** Sets {@code job_id}. */
    public DocumentsJobsInfoRequest jobId(String value) {
        this.jobId = value;
        return this;
    }

    /** Current {@code job_id}. */
    public String jobId() {
        return jobId;
    }

}
