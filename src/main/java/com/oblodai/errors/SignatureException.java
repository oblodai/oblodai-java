package com.oblodai.errors;

/**
 * Webhook verification failed: the signature does not match the raw body, the delivery timestamp is
 * outside the tolerated window, or the signature headers are missing. Answer such a delivery with
 * 400 and do not process it — a valid-looking body with a bad signature is not from the gateway.
 */
public class SignatureException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** The signature does not match the body (or the body is not a webhook event). */
    public static final String BAD_SIGNATURE = "webhook.bad_signature";

    /** The delivery timestamp is outside the tolerated window. */
    public static final String STALE_TIMESTAMP = "webhook.stale_timestamp";

    /** A required signature header is missing. */
    public static final String MISSING_HEADER = "webhook.missing_header";

    /**
     * @param code one of the {@code webhook.*} constants on this class
     * @param message what failed
     */
    public SignatureException(String code, String message) {
        super(code, message, 0, false, null, null, null, false, null, null);
    }
}
