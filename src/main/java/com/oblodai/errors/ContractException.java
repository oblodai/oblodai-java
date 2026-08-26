package com.oblodai.errors;

/**
 * The response could not be interpreted as the documented envelope: a success status with no {@code
 * {state, result}} body, a list route that did not answer with {@code {items, paginate}}, or an
 * idempotent replay the gateway could not reproduce — or a webhook delivery that verified but whose
 * body is not an event ({@link #WEBHOOK_BAD_PAYLOAD}). Not retryable — the shapes will not change by
 * asking again.
 */
public class ContractException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** An API answer that is not the documented envelope. */
    public static final String BAD_ENVELOPE = "sdk.bad_envelope";

    /** The answer was larger than the SDK reads into memory. */
    public static final String RESPONSE_TOO_LARGE = "sdk.response_too_large";

    /**
     * A webhook delivery whose signature verified but whose body is not an event. Deliberately in
     * this family and not in {@code webhook.bad_signature}'s: the delivery is authentic, and a
     * receiver that answers 401 to signature failures must not answer 401 to this.
     */
    public static final String WEBHOOK_BAD_PAYLOAD = "webhook.bad_payload";

    /**
     * @param message what was expected and what arrived
     * @param httpStatus status of the answer that could not be read
     * @param raw the body (or its decoded form), for deliberate inspection
     */
    public ContractException(String message, int httpStatus, Object raw) {
        this(BAD_ENVELOPE, message, httpStatus, raw);
    }

    /**
     * @param code {@link #BAD_ENVELOPE}, {@link #RESPONSE_TOO_LARGE} or {@link #WEBHOOK_BAD_PAYLOAD}
     * @param message what was expected and what arrived
     * @param httpStatus status of the answer that could not be read, or 0
     * @param raw the body (or its decoded form), for deliberate inspection
     */
    public ContractException(String code, String message, int httpStatus, Object raw) {
        super(code, message, httpStatus, false, null, null, null, false, raw, null);
    }
}
