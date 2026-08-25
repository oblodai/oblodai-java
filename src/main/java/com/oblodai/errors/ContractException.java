package com.oblodai.errors;

/**
 * The response could not be interpreted as the documented envelope: a success status with no {@code
 * {state, result}} body, a list route that did not answer with {@code {items, paginate}}, or an
 * idempotent replay the gateway could not reproduce. Not retryable — the shapes will not change by
 * asking again.
 */
public class ContractException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** The single code this exception carries. */
    public static final String BAD_ENVELOPE = "sdk.bad_envelope";

    /**
     * @param message what was expected and what arrived
     * @param httpStatus status of the answer that could not be read
     * @param raw the body (or its decoded form), for deliberate inspection
     */
    public ContractException(String message, int httpStatus, Object raw) {
        super(BAD_ENVELOPE, message, httpStatus, false, null, null, null, false, raw, null);
    }
}
