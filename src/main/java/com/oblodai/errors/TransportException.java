package com.oblodai.errors;

/**
 * The request never produced an HTTP response: DNS, TCP, TLS, a per-attempt timeout, a cancellation
 * or the overall call deadline. Timeouts and network failures are retryable, but only on requests
 * that are safe to repeat — the gateway may have processed a write whose answer was lost.
 */
public class TransportException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** The attempt exceeded its own timeout. */
    public static final String TIMEOUT = "transport.timeout";

    /** DNS, TCP, TLS or a broken connection. */
    public static final String NETWORK = "transport.network";

    /** The caller cancelled the call. */
    public static final String ABORTED = "transport.aborted";

    /** A retry would have exceeded the overall deadline of the call. */
    public static final String DEADLINE = "transport.deadline";

    /**
     * @param code one of {@link #TIMEOUT}, {@link #NETWORK}, {@link #ABORTED}, {@link #DEADLINE}
     * @param message what happened
     * @param cause the underlying I/O failure, when there is one
     */
    public TransportException(String code, String message, Throwable cause) {
        super(
                code,
                message,
                0,
                TIMEOUT.equals(code) || NETWORK.equals(code),
                null,
                null,
                null,
                false,
                null,
                cause);
    }
}
