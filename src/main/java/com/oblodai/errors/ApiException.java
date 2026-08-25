package com.oblodai.errors;

/** The gateway (or something in front of it) answered with an error status. */
public class ApiException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /**
     * @param code stable machine code, {@code family.reason}
     * @param message human-readable message from the gateway
     * @param httpStatus HTTP status of the answer
     * @param retryable the gateway's own verdict on whether a repeat can succeed
     * @param retryAfter seconds to wait before retrying, when hinted
     * @param requestId gateway-side request id
     * @param field the request field a validation failure refers to
     * @param synthetic true when no gateway envelope was present
     * @param raw decoded error body, or the raw text when it was not JSON
     */
    public ApiException(
            String code,
            String message,
            int httpStatus,
            boolean retryable,
            Integer retryAfter,
            String requestId,
            String field,
            boolean synthetic,
            Object raw) {
        super(code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw, null);
    }

    /**
     * @param code stable machine code, {@code family.reason}
     * @param message human-readable message from the gateway
     * @param httpStatus HTTP status of the answer
     * @param retryable the gateway's own verdict on whether a repeat can succeed
     * @param retryAfter seconds to wait before retrying, when hinted
     * @param requestId gateway-side request id
     * @param field the request field a validation failure refers to
     * @param synthetic true when no gateway envelope was present
     * @param raw decoded error body, or the raw text when it was not JSON
     * @param cause the underlying failure, when there is one
     */
    protected ApiException(
            String code,
            String message,
            int httpStatus,
            boolean retryable,
            Integer retryAfter,
            String requestId,
            String field,
            boolean synthetic,
            Object raw,
            Throwable cause) {
        super(code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw, cause);
    }
}
