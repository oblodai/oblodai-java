package com.oblodai.errors;

/**
 * A webhook delivery whose signature verified but whose body is not an event: not JSON, not an
 * object, or a field of a type no event carries.
 *
 * <p>Deliberately a {@link ContractException} and not a {@link SignatureException}: the delivery is
 * authentic, and a receiver that answers 401 to signature failures must not answer 401 to this one —
 * the gateway would keep retrying a delivery the receiver is rejecting for the wrong reason. Answer
 * 400, log the body, and investigate.
 */
public class WebhookPayloadException extends ContractException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what could not be read
     */
    public WebhookPayloadException(String message) {
        super(WEBHOOK_BAD_PAYLOAD, message, 0, null);
    }
}
