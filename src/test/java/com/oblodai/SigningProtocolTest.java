package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oblodai.core.Idempotency;
import com.oblodai.core.Signing;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.webhooks.WebhookVerifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The 1.x/2.0 public names of the signing protocol are aliases of the generated {@link
 * SigningProtocol}: a rename in the contract reaches them with the next generation. That the runtime
 * builds its strings by the generated order, not one of its own, is core/SigningOrderTest.
 */
class SigningProtocolTest {

    @Test
    void requestNamesAreTheGeneratedOnes() {
        assertEquals(
                SigningProtocol.REQUEST_HEADERS,
                List.of(
                        Signing.HEADER_PUBLIC_ID,
                        Signing.HEADER_SIGNATURE,
                        Signing.HEADER_TIMESTAMP,
                        Signing.HEADER_IDEMPOTENCY_KEY));
        assertEquals(SigningProtocol.SKEW_SECONDS, Signing.SIGNATURE_SKEW_SECONDS);
        assertEquals(SigningProtocol.MAX_IDEMPOTENCY_KEY_LENGTH, Idempotency.MAX_KEY_LENGTH);
    }

    @Test
    void webhookNamesAreTheGeneratedOnes() {
        assertEquals(
                SigningProtocol.WEBHOOK_HEADERS,
                List.of(
                        WebhookVerifier.HEADER_TIMESTAMP,
                        WebhookVerifier.HEADER_SIGNATURE,
                        WebhookVerifier.HEADER_SIGNATURE_PREV,
                        WebhookVerifier.HEADER_EVENT,
                        WebhookVerifier.HEADER_ID,
                        WebhookVerifier.HEADER_EVENT_ID,
                        WebhookVerifier.HEADER_EVENT_TIME));
        assertEquals(SigningProtocol.HEADER_WEBHOOK_TEST, WebhookVerifier.HEADER_TEST);
        assertEquals(SigningProtocol.SKEW_SECONDS, WebhookVerifier.DEFAULT_TOLERANCE_SECONDS);
    }
}
