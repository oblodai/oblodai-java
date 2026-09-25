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
 * SigningProtocol}: a rename in the contract reaches them with the next generation.
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
        assertEquals(SigningProtocol.SKEW_SECONDS, WebhookVerifier.DEFAULT_TOLERANCE_SECONDS);
    }

    /** The canonical string follows the generated order and separator, not a copy of them. */
    @Test
    void canonicalStringFollowsTheGeneratedOrder() {
        String key = "k-1";
        String body = "{\"a\":1}";
        List<String> parts = new java.util.ArrayList<>();
        for (String part : SigningProtocol.REQUEST_CANONICAL) {
            parts.add(
                    switch (part) {
                        case "ts" -> "1700000000";
                        case "METHOD" -> "POST";
                        case "request_uri" -> "/v1/x?y=1";
                        case "idempotency_key" -> key;
                        case "body" -> body;
                        default -> throw new AssertionError(part);
                    });
        }
        assertEquals(
                String.join(SigningProtocol.REQUEST_CANONICAL_SEPARATOR, parts),
                Signing.canonicalString(1700000000L, "post", "/v1/x?y=1", key, body));
    }
}
