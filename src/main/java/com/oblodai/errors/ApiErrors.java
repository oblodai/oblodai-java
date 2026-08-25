package com.oblodai.errors;

import java.util.Set;

/** Builds the right {@link ApiException} subclass from an error envelope and the HTTP status. */
public final class ApiErrors {

    /** Statuses a response without an envelope may carry transiently (load balancer, proxy). */
    private static final Set<Integer> TRANSIENT_STATUSES = Set.of(408, 425, 429, 500, 502, 503, 504);

    private ApiErrors() {}

    /**
     * @param httpStatus status of the answer
     * @param detail the decoded {@code error} object, or a synthesized one
     * @param raw the body, kept off the default logging path
     * @param synthetic true when the body carried no gateway envelope
     * @param retryAfterHeader parsed {@code Retry-After} header, in seconds
     * @return the exception to throw
     */
    public static ApiException from(
            int httpStatus,
            ErrorDetail detail,
            Object raw,
            boolean synthetic,
            Integer retryAfterHeader) {
        String code = detail.code() == null || detail.code().isEmpty() ? "internal" : detail.code();
        String message =
                detail.message() == null || detail.message().isEmpty()
                        ? "request failed with HTTP "
                                + httpStatus
                                + " ("
                                + (detail.code() == null ? "no envelope" : detail.code())
                                + ")"
                        : detail.message();
        boolean retryable;
        if (synthetic) {
            // No gateway envelope: only the statuses an intermediary emits transiently may repeat.
            retryable = TRANSIENT_STATUSES.contains(httpStatus);
        } else if (detail.retryable() != null) {
            retryable = detail.retryable();
        } else {
            retryable = httpStatus == 429 || httpStatus == 503;
        }
        Integer retryAfter = detail.retryAfter() != null ? detail.retryAfter() : retryAfterHeader;
        String requestId = detail.requestId();
        String field = detail.field();

        if ("idempotency.key_reused".equals(code)) {
            return new IdempotencyConflictException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
        }
        return switch (httpStatus) {
            case 400 -> new ValidationException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 401 -> new AuthenticationException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 403 -> new PermissionException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 404 -> new NotFoundException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 409 -> new ConflictException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 429 -> new RateLimitException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            case 503 -> new UnavailableException(
                    code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
            default -> httpStatus >= 500
                    ? new InternalException(
                            code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw)
                    : new ApiException(
                            code, message, httpStatus, retryable, retryAfter, requestId, field, synthetic, raw);
        };
    }
}
