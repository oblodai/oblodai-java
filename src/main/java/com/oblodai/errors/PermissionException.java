package com.oblodai.errors;

/** 403 — the key is valid but not allowed to do this (wrong key kind, feature disabled). */
public class PermissionException extends ApiException {

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
    public PermissionException(
            String code,
            String message,
            int httpStatus,
            boolean retryable,
            Integer retryAfter,
            String requestId,
            String field,
            boolean synthetic,
            Object raw) {
        super(code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
    }
}
