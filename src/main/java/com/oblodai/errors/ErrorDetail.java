package com.oblodai.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code error} object of the gateway's error envelope, exactly as it arrives on the wire.
 *
 * @param code stable machine code, {@code family.reason}
 * @param message human-readable message
 * @param field the request field at fault, on validation failures
 * @param retryable the gateway's own verdict on whether repeating can succeed
 * @param retryAfter seconds to wait before retrying
 * @param requestId gateway-side request id
 */
public record ErrorDetail(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("field") String field,
        @JsonProperty("retryable") Boolean retryable,
        @JsonProperty("retry_after") Integer retryAfter,
        @JsonProperty("request_id") String requestId) {}
