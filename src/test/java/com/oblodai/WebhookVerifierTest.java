package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Signing;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.generated.models.PaymentWebhook;
import com.oblodai.webhooks.WebhookEvent;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Webhook verification: signatures, the rotation overlap, freshness and the body. The gateway's own
 * vectors (x-oblodai-signing) run in the conformance suite.
 */
class WebhookVerifierTest {

    private static final long TS = 1_755_600_000L;
    private static final String BODY =
            "{\"type\":\"payment\",\"uuid\":\"u1\",\"order_id\":\"o\",\"status\":\"paid\","
                    + "\"is_final\":true,\"sequence\":7,\"event_at\":\"2026-01-01T00:00:00Z\"}";

    private static WebhookHeaders headers(String signature, String previousSignature) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(SigningProtocol.HEADER_WEBHOOK_TIMESTAMP.toLowerCase(java.util.Locale.ROOT), String.valueOf(TS));
        headers.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE.toLowerCase(java.util.Locale.ROOT), signature);
        if (previousSignature != null) headers.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE_PREV.toLowerCase(java.util.Locale.ROOT), previousSignature);
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
        assertEquals("u1", event.uuid());
        assertEquals("o", event.orderId());
        assertEquals(7L, event.sequence());
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
                                        WebhookHeaders.of(Map.of(SigningProtocol.HEADER_WEBHOOK_SIGNATURE.toLowerCase(java.util.Locale.ROOT), "aa")),
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
                                        BODY, good, WebhookVerifier.options("whsec").clock(() -> TS + 2L * SigningProtocol.SKEW_SECONDS)));
        assertEquals(SignatureException.STALE_TIMESTAMP, stale.code());

        assertEquals(
                "u1",
                WebhookVerifier.verify(
                                BODY,
                                good,
                                WebhookVerifier.options("whsec")
                                        .clock(() -> TS + 2L * SigningProtocol.SKEW_SECONDS)
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
    void flagsRehearsalDeliveriesFromTheSignedBodyOnly() {
        WebhookHeaders live = headers(Signing.signWebhook("whsec", TS, BODY), null);
        assertFalse(
                WebhookVerifier.verifyDelivery(
                                BODY.getBytes(StandardCharsets.UTF_8),
                                live,
                                WebhookVerifier.options("whsec").clock(() -> TS))
                        .isTest(),
                "a live delivery is not a rehearsal");

        // The body carries it: recognised with or without the advisory header.
        String testBody = BODY.replace("\"sequence\":7", "\"sequence\":7,\"test\":true");
        WebhookDeliveryInfo fromBody =
                WebhookVerifier.verifyDelivery(
                        testBody.getBytes(StandardCharsets.UTF_8),
                        headers(Signing.signWebhook("whsec", TS, testBody), null),
                        WebhookVerifier.options("whsec").clock(() -> TS));
        assertTrue(fromBody.isTest());
        assertTrue(WebhookVerifier.isTestEvent(fromBody.event()));
        assertTrue(fromBody.event().test());

        // Ruling R1: only the unsigned header carries it — a replayer adding it to a live delivery
        // must not turn it into an ignored rehearsal.
        Map<String, String> withHeader = new LinkedHashMap<>();
        withHeader.put(SigningProtocol.HEADER_WEBHOOK_TIMESTAMP.toLowerCase(java.util.Locale.ROOT), String.valueOf(TS));
        withHeader.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE.toLowerCase(java.util.Locale.ROOT), Signing.signWebhook("whsec", TS, BODY));
        withHeader.put(SigningProtocol.HEADER_WEBHOOK_TEST, "true");
        WebhookDeliveryInfo fromHeader =
                WebhookVerifier.verifyDelivery(
                        BODY.getBytes(StandardCharsets.UTF_8),
                        WebhookHeaders.of(withHeader),
                        WebhookVerifier.options("whsec").clock(() -> TS));
        assertFalse(fromHeader.isTest(), "the unsigned header is not trusted");
        assertTrue(fromHeader.unverifiedTestHeader());
        assertFalse(WebhookVerifier.isTestEvent(fromHeader.event()));
    }

    /** Ruling R1: the dedupe key comes from the signed body; rewritten id headers do not change it. */
    @Test
    void theDedupeKeyComesFromTheSignedBodyNotTheHeaders() {
        String signature = Signing.signWebhook("whsec", TS, BODY);
        Map<String, String> first = new LinkedHashMap<>();
        first.put(SigningProtocol.HEADER_WEBHOOK_TIMESTAMP, String.valueOf(TS));
        first.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE, signature);
        first.put(SigningProtocol.HEADER_WEBHOOK_EVENT_ID, "e-1");
        first.put(SigningProtocol.HEADER_WEBHOOK_ID, "d-1");
        Map<String, String> replay = new LinkedHashMap<>(first);
        replay.put(SigningProtocol.HEADER_WEBHOOK_EVENT_ID, "e-forged");
        replay.put(SigningProtocol.HEADER_WEBHOOK_ID, "d-forged");
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        WebhookDeliveryInfo a =
                WebhookVerifier.verifyDelivery(
                        body, WebhookHeaders.of(first), WebhookVerifier.options("whsec").clock(() -> TS));
        WebhookDeliveryInfo b =
                WebhookVerifier.verifyDelivery(
                        body, WebhookHeaders.of(replay), WebhookVerifier.options("whsec").clock(() -> TS));
        assertEquals(a.eventKey(), b.eventKey());
        assertEquals("e-forged", b.unverifiedEventId());
        assertEquals(a.event().eventKey(), a.eventKey());
        assertTrue(a.eventKey().endsWith(":7"), a.eventKey());

        // A core that signs the state id: the body's event_id is the key, whatever the headers say.
        String signedState = BODY.replace("\"sequence\":7", "\"sequence\":7,\"event_id\":\"st-42\"");
        Map<String, String> withState = new LinkedHashMap<>(replay);
        withState.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE, Signing.signWebhook("whsec", TS, signedState));
        WebhookDeliveryInfo c =
                WebhookVerifier.verifyDelivery(
                        signedState.getBytes(StandardCharsets.UTF_8),
                        WebhookHeaders.of(withState),
                        WebhookVerifier.options("whsec").clock(() -> TS));
        assertEquals("st-42", c.eventKey());
    }

    /** A full payment body as a core sends it; {@code %s} is where an event_id may go. */
    private static final String FULL_PAYMENT =
            "{\"type\":\"payment\",\"uuid\":\"u1\",\"order_id\":\"o\",\"status\":\"paid\","
                    + "\"is_final\":true,\"amount\":\"25\",\"currency\":\"USDT\",\"network\":\"tron\","
                    + "\"payer_amount\":\"25\",\"payer_currency\":\"USDT\",\"payment_amount\":\"25\","
                    + "\"payer_address\":\"T\",\"payer_address_is_refundable\":true,\"additional_data\":\"\","
                    + "\"txid\":\"tx\",\"event_at\":\"2026-01-01T00:00:00Z\"%s}";

    private static WebhookDeliveryInfo deliver(String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(SigningProtocol.HEADER_WEBHOOK_TIMESTAMP, String.valueOf(TS));
        headers.put(SigningProtocol.HEADER_WEBHOOK_SIGNATURE, Signing.signWebhook("whsec", TS, body));
        return WebhookVerifier.verifyDelivery(
                body.getBytes(StandardCharsets.UTF_8),
                WebhookHeaders.of(headers),
                WebhookVerifier.options("whsec").clock(() -> TS));
    }

    @Test
    void aResendOfOneStateDedupesOnEventIdThoughItsSequenceGrows() {
        String field = ",\"" + SigningProtocol.WEBHOOK_EVENT_ID_FIELD + "\":\"st-42\"";
        WebhookDeliveryInfo first = deliver(FULL_PAYMENT.formatted(",\"sequence\":7" + field));
        WebhookDeliveryInfo resend = deliver(FULL_PAYMENT.formatted(",\"sequence\":9" + field));
        assertEquals(7L, first.event().sequence());
        assertEquals(9L, resend.event().sequence());
        assertEquals("st-42", first.eventKey());
        assertEquals(first.eventKey(), resend.eventKey(), "a resend is the same state");
        assertEquals("st-42", first.event().as(PaymentWebhook.class).eventId());
    }

    @Test
    void aPresentButEmptyOrNonStringEventIdIsABadPayload() {
        for (String bad : List.of("\"\"", "null", "42", "{}")) {
            for (String kind : List.of("payment", "brand_new_kind")) {
                String body =
                        FULL_PAYMENT
                                .formatted(",\"sequence\":7,\"" + SigningProtocol.WEBHOOK_EVENT_ID_FIELD + "\":" + bad)
                                .replace("\"payment\"", "\"" + kind + "\"");
                WebhookPayloadException e =
                        assertThrows(WebhookPayloadException.class, () -> deliver(body), kind + " " + bad);
                assertEquals("webhook.bad_payload", e.code(), kind + " " + bad);
            }
        }
    }

    @Test
    void aDeliveryFromAnOlderCoreWithoutEventIdFallsBackToTypeIdSequence() {
        WebhookDeliveryInfo delivery = deliver(FULL_PAYMENT.formatted(",\"sequence\":7"));
        assertEquals("payment:u1:7", delivery.eventKey());
        assertNull(delivery.event().as(PaymentWebhook.class).eventId(), "event_id is optional");
    }

    @Test
    void parsesTheDiscriminatedUnionAndDetectsStaleSequences() {
        WebhookEvent event = WebhookVerifier.parse(BODY);
        assertEquals("payment", event.type());
        assertTrue(WebhookVerifier.isStale(event, 7L));
        assertFalse(WebhookVerifier.isStale(event, 6L));
        assertFalse(WebhookVerifier.isStale(event, null));

        // A kind this snapshot has not heard of is delivered, not thrown: a gateway that grows a
        // new event type must not break a deployed receiver.
        WebhookEvent alien = WebhookVerifier.parse("{\"type\":\"alien\",\"uuid\":\"x\"}");
        assertEquals("alien", alien.type());
        assertEquals("x", alien.uuid());
        assertEquals(null, alien.objectId()); // which field identifies an unknown kind is not guessed
        assertFalse(WebhookVerifier.isStale(alien, 7L));

        // A body that is not an event at all is a contract failure, not a signature failure.
        assertEquals(
                "webhook.bad_payload",
                assertThrows(ContractException.class, () -> WebhookVerifier.parse("[1,2,3]")).code());
    }

    /** objectId() reads the field the contract names for the kind: a conversion's is id, not uuid. */
    @Test
    void objectIdIsTheFieldTheContractNamesForTheKind() {
        for (com.oblodai.generated.Facts.WebhookKind kind : com.oblodai.generated.Facts.WEBHOOK_KINDS.values()) {
            assertNotNull(kind.idField(), kind.kind());
            WebhookEvent event =
                    WebhookVerifier.parse("{\"type\":\"" + kind.kind() + "\",\"" + kind.idField() + "\":\"obj-1\"}");
            assertEquals("obj-1", event.objectId(), kind.kind());
        }
        WebhookEvent conversion =
                WebhookVerifier.parse("{\"type\":\"conversion\",\"id\":\"c1\",\"sequence\":2}");
        assertEquals("c1", conversion.objectId());
        assertEquals(null, conversion.uuid());
    }
}
