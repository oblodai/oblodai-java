package com.oblodai.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * The {@code error} object of the gateway's error envelope, exactly as it arrives on the wire.
 *
 * @param code stable machine code, {@code family.reason}
 * @param message human-readable message
 * @param field the request field at fault, on validation failures
 * @param retryable the gateway's own verdict on whether repeating can succeed
 * @param retryAfter seconds to wait before retrying
 * @param requestId gateway-side request id
 * @param details machine-readable facts about the refusal, keys documented by its code; empty when
 *     the gateway sent none
 */
public record ErrorDetail(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("field") String field,
        @JsonProperty("retryable") Boolean retryable,
        @JsonProperty("retry_after") Integer retryAfter,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("details") Map<String, String> details) {

    /** Normalizes {@code details}: never null, never modifiable. */
    public ErrorDetail {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    /**
     * The envelope without {@code details}, as before they existed.
     *
     * @param code stable machine code, {@code family.reason}
     * @param message human-readable message
     * @param field the request field at fault, on validation failures
     * @param retryable the gateway's own verdict on whether repeating can succeed
     * @param retryAfter seconds to wait before retrying
     * @param requestId gateway-side request id
     */
    public ErrorDetail(
            String code, String message, String field, Boolean retryable, Integer retryAfter, String requestId) {
        this(code, message, field, retryable, retryAfter, requestId, Map.of());
    }
}
