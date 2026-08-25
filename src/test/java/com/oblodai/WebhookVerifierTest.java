package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.oblodai.core.Signing;
import com.oblodai.errors.SignatureException;
import com.oblodai.models.PaymentEvent;
import com.oblodai.models.WebhookEvent;
import com.oblodai.support.Contract;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Webhook verification against real deliveries. The samples were signed by the gateway's own
 * dispatcher with the endpoint secret in force at that moment — the one the recorded
 * {@code rotate-secret} call returned — and carry the exact bytes that were delivered.
 */
class WebhookVerifierTest {

    private static final String SECRET =
            Contract.result("POST /v1/webhooks/rotate-secret").path("secret").asText();

    private static WebhookHeaders headersOf(JsonNode sample) {
        Map<String, String> headers = new LinkedHashMap<>();
        sample.path("headers").fields().forEachRemaining(e -> headers.put(e.getKey(), e.getValue().asText()));
        return WebhookHeaders.of(headers);
    }

    @TestFactory
    List<DynamicTest> verifiesEveryRecordedDelivery() {
        List<DynamicTest> tests = new ArrayList<>();
        List<JsonNode> samples = Contract.webhookSamples();
        assertFalse(samples.isEmpty(), "no webhook samples recorded");
        int index = 0;
        for (JsonNode sample : samples) {
            int at = index++;
            tests.add(
                    DynamicTest.dynamicTest(
                            at + " " + sample.path("headers").path("X-Webhook-Event").asText(),
                            () -> {
                                byte[] raw = sample.path("raw").asText().getBytes(StandardCharsets.UTF_8);
                                long ts =
                                        Long.parseLong(
                                                sample.path("headers").path("X-Webhook-Timestamp").asText());
                                WebhookDeliveryInfo delivery =
                                        WebhookVerifier.verifyDelivery(
                                                raw,
                                                headersOf(sample),
                                                WebhookVerifier.options(SECRET).clock(() -> ts));

                                assertEquals(sample.path("body").path("uuid").asText(), delivery.event().uuid());
                                assertEquals(
                                        sample.path("headers").path("X-Webhook-Id").asText(), delivery.id());
                                assertEquals(
                                        sample.path("headers").path("X-Webhook-Event").asText(),
                                        delivery.eventType());
                                assertEquals(sample.path("body").path("type").asText(), delivery.event().type());
                                assertNotNull(delivery.event().sequence());
                                assertTrue(
                                        delivery.eventType().matches("^(invoice|payout|wallet)\\..+"),
                                        "event type vocabulary");

                                // The same bytes under any other secret must not verify.
                                assertThrows(
                                        SignatureException.class,
                                        () ->
                                                WebhookVerifier.verify(
                                                        raw,
                                                        headersOf(sample),
                                                        WebhookVerifier.options("some-other-secret")
                                                                .previousSecret("another")
                                                                .clock(() -> ts)));
                            }));
        }
        return tests;
    }

    private static final long TS = 1_755_600_000L;
    private static final String BODY =
            "{\"type\":\"payment\",\"uuid\":\"u1\",\"order_id\":\"o\",\"status\":\"paid\","
                    + "\"is_final\":true,\"sequence\":7,\"event_at\":\"2026-01-01T00:00:00Z\"}";

    private static WebhookHeaders headers(String signature, String previousSignature) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-webhook-timestamp", String.valueOf(TS));
        headers.put("x-webhook-signature", signature);
        if (previousSignature != null) headers.put("x-webhook-signature-prev", previousSignature);
        return WebhookHeaders.of(headers);
    }

    @Test
    void acceptsAValidSignatureWithCaseInsensitiveHeaders() {
        WebhookEvent event =
                WebhookVerifier.verify(
                        BODY,
                        headers(Signing.signWebhook("whsec", TS, BODY), null),
                        WebhookVerifier.options("whsec").clock(() -> TS));
        assertEquals("payment", event.type());
        assertTrue(event instanceof PaymentEvent);
    }

    @Test
    void rejectsAWrongSecretATamperedBodyAndAMissingHeader() {
        WebhookHeaders good = headers(Signing.signWebhook("whsec", TS, BODY), null);
        assertThrows(
                SignatureException.class,
                () -> WebhookVerifier.verify(BODY, good, WebhookVerifier.options("other").clock(() -> TS)));

        String tampered = BODY.replace("\"paid\"", "\"paid_over\"");
        SignatureException bad =
                assertThrows(
                        SignatureException.class,
                        () ->
                                WebhookVerifier.verify(
                                        tampered, good, WebhookVerifier.options("whsec").clock(() -> TS)));
        assertEquals(SignatureException.BAD_SIGNATURE, bad.code());

        SignatureException missing =
                assertThrows(
                        SignatureException.class,
                        () ->
                                WebhookVerifier.verify(
                                        BODY,
                                        WebhookHeaders.of(Map.of("x-webhook-signature", "aa")),
                                        WebhookVerifier.options("whsec")));
        assertEquals(SignatureException.MISSING_HEADER, missing.code());
    }

    @Test
    void rejectsStaleDeliveriesUnlessToleranceIsDisabled() {
        WebhookHeaders good = headers(Signing.signWebhook("whsec", TS, BODY), null);
        SignatureException stale =
                assertThrows(
                        SignatureException.class,
                        () ->
                                WebhookVerifier.verify(
                                        BODY, good, WebhookVerifier.options("whsec").clock(() -> TS + 600)));
        assertEquals(SignatureException.STALE_TIMESTAMP, stale.code());

        assertEquals(
                "u1",
                WebhookVerifier.verify(
                                BODY,
                                good,
                                WebhookVerifier.options("whsec")
                                        .clock(() -> TS + 600)
                                        .tolerance(Duration.ZERO))
                        .uuid());
    }

    @Test
    void verifiesDuringARotationFromEitherSide() {
        WebhookHeaders rotated =
                headers(Signing.signWebhook("new", TS, BODY), Signing.signWebhook("old", TS, BODY));
        // The merchant has not swapped the stored secret yet: the Prev header verifies with it.
        assertEquals(
                "u1",
                WebhookVerifier.verify(BODY, rotated, WebhookVerifier.options("old").clock(() -> TS)).uuid());
        // The merchant already swapped: the main header verifies with the new secret.
        assertEquals(
                "u1",
                WebhookVerifier.verify(BODY, rotated, WebhookVerifier.options("new").clock(() -> TS)).uuid());
        // The merchant keeps both, and the stored "current" is neither of the delivery's own.
        assertEquals(
                "u1",
                WebhookVerifier.verify(
                                BODY,
                                rotated,
                                WebhookVerifier.options("unrelated").previousSecret("old").clock(() -> TS))
                        .uuid());
    }

    @Test
    void parsesTheDiscriminatedUnionAndDetectsStaleSequences() {
        WebhookEvent event = WebhookVerifier.parse(BODY);
        assertEquals("payment", event.type());
        assertTrue(WebhookVerifier.isStale(event, 7L));
        assertFalse(WebhookVerifier.isStale(event, 6L));
        assertFalse(WebhookVerifier.isStale(event, null));

        SignatureException alien =
                assertThrows(
                        SignatureException.class,
                        () -> WebhookVerifier.parse("{\"type\":\"alien\",\"uuid\":\"x\"}"));
        assertTrue(alien.getMessage().contains("unknown event type"));
    }
}
