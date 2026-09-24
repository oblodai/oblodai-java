package com.oblodai.errors;

/**
 * A long-running operation was still not finished when {@code waitFor} gave up. Nothing failed on
 * the gateway: poll again later, or wait longer.
 */
public class JobTimeoutException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** The error code. */
    public static final String CODE = "sdk.job_timeout";

    /**
     * @param message which job, in which status, after how long
     */
    public JobTimeoutException(String message) {
        super(CODE, message, 0, true, null, null, null, false, null, null);
    }
}
