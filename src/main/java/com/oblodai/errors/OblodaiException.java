package com.oblodai.errors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every failure the SDK raises. One family mirrors the gateway's error envelope:
 *
 * <pre>{ "error": { "code", "message", "field"?, "retryable", "retry_after"?, "request_id"? } }</pre>
 *
 * <p>{@link #retryable()} is authoritative when the gateway wrote the envelope: it is the gateway's
 * own classification of the failure, and the SDK has already retried what it was safe to retry. A
 * response without an envelope (a proxy 502, an HTML 503) is {@link #synthetic()} — the gateway
 * never saw the request, or never answered it — and is retried only when repeating is safe.
 *
 * <p>Subclasses exist for {@code catch} ergonomics; the discriminator is always {@link #code()}.
 * This is an unchecked exception: a payment API call fails for business reasons far more often than
 * for reasons a caller can handle locally, and checked exceptions would only be rethrown.
 *
 * <p>{@link #toString()} and {@link #details()} never include the raw response body, so a logger
 * cannot spill an invoice payload or a cheque passcode into a log file. The body is available on
 * {@link #raw()} for deliberate inspection.
 */
public class OblodaiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final int httpStatus;
    private final boolean retryable;
    private final Integer retryAfter;
    private final String requestId;
    private final String field;
    private final boolean synthetic;
    private final transient Object raw;

    /**
     * @param code stable machine code, {@code family.reason}
     * @param message human-readable message from the gateway, or the SDK's own
     * @param httpStatus HTTP status, or 0 when no response was received
     * @param retryable whether repeating the identical request can succeed later
     * @param retryAfter seconds to wait before retrying, when the gateway hinted one
     * @param requestId gateway-side request id — quote it to support
     * @param field the request field a validation failure refers to
     * @param synthetic true when no gateway envelope was present (a proxy answered)
     * @param raw decoded error body, or the raw text when the body was not JSON
     * @param cause underlying exception, when there is one
     */
    public OblodaiException(
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
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
        this.requestId = requestId;
        this.field = field;
        this.synthetic = synthetic;
        this.raw = raw;
    }

    /** Stable machine code ({@code family.reason}), e.g. {@code payout.insufficient_funds}. */
    public String code() {
        return code;
    }

    /** Code family ({@code payout} in {@code payout.insufficient_funds}). */
    public String family() {
        int dot = code == null ? -1 : code.indexOf('.');
        return dot < 0 ? code : code.substring(0, dot);
    }

    /** HTTP status, or 0 when no response was received. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Whether repeating the identical request can succeed later. */
    public boolean retryable() {
        return retryable;
    }

    /** Seconds to wait before retrying, when the gateway (or a {@code Retry-After}) gave a hint. */
    public Integer retryAfter() {
        return retryAfter;
    }

    /** Gateway-side request id; quote it when contacting support. */
    public String requestId() {
        return requestId;
    }

    /** The request field the error refers to, for validation failures. */
    public String field() {
        return field;
    }

    /** No gateway envelope: the answer came from something in front of the gateway. */
    public boolean synthetic() {
        return synthetic;
    }

    /** The decoded error body, or the raw text when it was not JSON. Never logged by default. */
    public Object raw() {
        return raw;
    }

    /** Structured-logger friendly view: keeps the message, drops the raw body. */
    public Map<String, Object> details() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", getClass().getSimpleName());
        out.put("code", code);
        out.put("message", getMessage());
        out.put("httpStatus", httpStatus);
        out.put("retryable", retryable);
        if (retryAfter != null) out.put("retryAfter", retryAfter);
        if (requestId != null) out.put("requestId", requestId);
        if (field != null) out.put("field", field);
        if (synthetic) out.put("synthetic", true);
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append(": ").append(code);
        if (getMessage() != null && !getMessage().isEmpty()) sb.append(" — ").append(getMessage());
        if (httpStatus != 0) sb.append(" (HTTP ").append(httpStatus).append(')');
        if (requestId != null) sb.append(" [request ").append(requestId).append(']');
        return sb.toString();
    }
}
