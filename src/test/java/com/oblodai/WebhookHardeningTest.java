package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Signing;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.SignatureException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.WebhookKinds;
import com.oblodai.generated.models.ConversionWebhook;
import com.oblodai.webhooks.WebhookEvent;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The rules a webhook verifier must not get wrong: never verify with an empty key, never let the
 * freshness window answer before the MAC does, never throw on an event kind this snapshot has not
 * heard of, and never report an authentic-but-unreadable body as a signature failure.
 */
class WebhookHardeningTest {

    private static final String SECRET = "whsec_live";
    private static final long NOW = 1_800_000_000L;
    private static final String BODY =
            "{\"type\":\"payment\",\"uuid\":\"u1\",\"order_id\":\"o1\",\"status\":\"paid\","
                    + "\"is_final\":true,\"sequence\":7,\"event_at\":\"2026-01-01T00:00:00Z\"}";

    private static WebhookVerifier.Options options() {
        return WebhookVerifier.options(SECRET).clock(() -> NOW);
    }

    private static WebhookHeaders headers(long timestamp, String signature) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("X-Webhook-Timestamp", Long.toString(timestamp));
        map.put("X-Webhook-Signature", signature);
        map.put("X-Webhook-Id", "wd_1");
        return WebhookHeaders.of(map);
    }

    private static String sign(String secret, long timestamp, String body) {
        return Signing.signWebhook(secret, timestamp, body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void anEmptySecretIsRefusedBeforeAnyCryptoHappens() {
        assertEquals(
                ConfigException.BAD_CONFIG,
                assertThrows(ConfigException.class, () -> WebhookVerifier.options(null)).code());
        assertThrows(ConfigException.class, () -> WebhookVerifier.options(""));
        assertThrows(ConfigException.class, () -> WebhookVerifier.options("   "));
        assertThrows(ConfigException.class, () -> options().previousSecret(""));
    }

    @Test
    void missingOptionsOrHeadersAreAConfigFailureNotANullPointer() {
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        assertThrows(
                ConfigException.class,
                () -> WebhookVerifier.verifyDelivery(body, headers(NOW, "aa"), null));
        assertThrows(
                ConfigException.class, () -> WebhookVerifier.verifyDelivery(body, null, options()));
    }

    @Test
    void aNegativeToleranceIsRefusedAndSubSecondWindowsAreKept() {
        assertThrows(ConfigException.class, () -> options().tolerance(Duration.ofSeconds(-1)));
        assertThrows(ConfigException.class, () -> options().tolerance(null));

        // 900 ms used to truncate to 0 seconds, which silently DISABLED the freshness check.
        WebhookVerifier.Options tight = options().tolerance(Duration.ofMillis(900));
        assertEquals(900, tight.toleranceMillis());
        long stale = NOW - 5;
        SignatureException failure =
                assertThrows(
                        SignatureException.class,
                        () ->
                                WebhookVerifier.verify(
                                        BODY, headers(stale, sign(SECRET, stale, BODY)), tight));
        assertEquals(SignatureException.STALE_TIMESTAMP, failure.code());

        WebhookVerifier.Options disabled = options().tolerance(Duration.ZERO);
        assertEquals(
                "u1", WebhookVerifier.verify(BODY, headers(stale, sign(SECRET, stale, BODY)), disabled).uuid());
    }

    @Test
    void theMacIsCheckedBeforeTheFreshnessWindow() {
        long ancient = NOW - 10_000;
        SignatureException failure =
                assertThrows(
                        SignatureException.class,
                        () -> WebhookVerifier.verify(BODY, headers(ancient, "00".repeat(32)), options()));
        assertEquals(
                SignatureException.BAD_SIGNATURE,
                failure.code(),
                "an unauthenticated caller must not learn the delivery timestamp was in range");
    }

    @Test
    void aSignatureHeaderIsTrimmedAndCaseInsensitiveButNotHexPrefixed() {
        String signature = sign(SECRET, NOW, BODY);
        assertEquals(
                "u1",
                WebhookVerifier.verify(BODY, headers(NOW, "  " + signature + "\t"), options()).uuid());
        assertEquals(
                "u1",
                WebhookVerifier.verify(BODY, headers(NOW, signature.toUpperCase(Locale.ROOT)), options())
                        .uuid());
        assertThrows(
                SignatureException.class,
                () -> WebhookVerifier.verify(BODY, headers(NOW, "0x" + signature), options()));
    }

    @Test
    void anAuthenticBodyThatCannotBeReadIsAContractFailureNotASignatureFailure() {
        String notJson = "<html>gateway error</html>";
        ContractException failure =
                assertThrows(
                        ContractException.class,
                        () ->
                                WebhookVerifier.verify(
                                        notJson, headers(NOW, sign(SECRET, NOW, notJson)), options()));
        assertEquals(ContractException.WEBHOOK_BAD_PAYLOAD, failure.code());
        assertEquals(
                "webhook.bad_payload",
                failure.code(),
                "a receiver that answers 401 to webhook.bad_signature must not answer 401 to this");
        assertFalse(
                SignatureException.class.isAssignableFrom(failure.getClass()),
                "it is not in the signature family");
        assertTrue(
                failure instanceof com.oblodai.errors.WebhookPayloadException,
                "and it has its own class to catch");

        // A body of the right kind with a field of the wrong type verifies (the delivery is
        // authentic); reading it as the typed event is where it fails, in the same family.
        String wrongTypes = "{\"type\":\"payment\",\"uuid\":\"u1\",\"sequence\":\"seven\"}";
        WebhookEvent event =
                WebhookVerifier.verify(wrongTypes, headers(NOW, sign(SECRET, NOW, wrongTypes)), options());
        assertNull(event.sequence());
        assertEquals(
                ContractException.WEBHOOK_BAD_PAYLOAD,
                assertThrows(ContractException.class, event::asPayment).code());
    }

    @Test
    void anEventKindThisSnapshotDoesNotKnowIsDeliveredWithItsRawType() {
        String body =
                "{\"type\":\"settlement\",\"uuid\":\"s1\",\"order_id\":\"o9\",\"sequence\":12,"
                        + "\"test\":true,\"amount\":\"25\"}";
        WebhookEvent event =
                WebhookVerifier.verify(body, headers(NOW, sign(SECRET, NOW, body)), options());

        WebhookEvent unknown = event;
        assertEquals("settlement", unknown.type());
        assertEquals("s1", unknown.uuid());
        assertEquals(12L, unknown.sequence());
        assertEquals("25", unknown.fields().get("amount"), "unmodelled fields are still readable");
        assertTrue(unknown.toString().contains("settlement"));
        assertFalse(WebhookVerifier.isKnownEvent(unknown), "a receiver can tell it is a new kind");
        assertTrue(WebhookVerifier.isTestEvent(unknown), "the helpers work on it");
        assertTrue(WebhookVerifier.isStale(unknown, 12L));
        assertFalse(WebhookVerifier.isStale(unknown, 11L));
    }

    @Test
    void everyKindOfTheContractIsKnownAndTyped() {
        assertEquals(Facts.KNOWN_WEBHOOK_KINDS, WebhookEvent.KNOWN_KINDS, "the runtime keeps no list of its own");
        assertTrue(WebhookEvent.KNOWN_KINDS.contains("conversion"));
        // A conversion event is keyed by id, not uuid: the contract's body has no uuid field.
        String body =
                "{\"type\":\"conversion\",\"id\":\"c1\",\"mode\":\"auto\",\"from\":\"USDT\",\"to\":\"TRX\","
                        + "\"sent\":\"10\",\"fee_percent\":\"1\",\"status\":\"completed\",\"reason\":\"\","
                        + "\"is_final\":true,\"document_url\":\"\",\"created_at\":\"2026-09-25T00:00:00Z\","
                        + "\"completed_at\":\"2026-09-25T00:00:00Z\",\"sequence\":3,"
                        + "\"event_at\":\"2026-09-25T00:00:00Z\"}";
        WebhookEvent event = WebhookVerifier.verify(body, headers(NOW, sign(SECRET, NOW, body)), options());
        assertTrue(WebhookVerifier.isKnownEvent(event));
        assertNull(event.uuid());
        assertTrue(event.typed() instanceof ConversionWebhook, "the model comes from the contract's table");
        assertEquals("c1", event.asConversion().id());
        assertThrows(IllegalStateException.class, event::asPayment);

        String unknown = "{\"type\":\"settlement\",\"uuid\":\"s1\"}";
        WebhookEvent later = WebhookVerifier.verify(unknown, headers(NOW, sign(SECRET, NOW, unknown)), options());
        assertNull(later.typed(), "a kind this SDK does not know has no model");
    }

    @Test
    void isStaleNeverThrowsOnAMissingSequenceOrAMissingEvent() {
        String body = "{\"type\":\"wallet\",\"uuid\":\"w1\",\"status\":\"paid\"}";
        WebhookEvent noSequence =
                WebhookVerifier.verify(body, headers(NOW, sign(SECRET, NOW, body)), options());
        assertTrue(WebhookVerifier.isKnownEvent(noSequence));
        assertFalse(WebhookVerifier.isKnownEvent(null));
        assertFalse(WebhookVerifier.isStale(noSequence, 5L));
        assertFalse(WebhookVerifier.isStale(noSequence, null));
        assertFalse(WebhookVerifier.isStale(null, 5L));
        assertFalse(WebhookVerifier.isTestEvent(null));
    }

    @Test
    void aRotationAcceptsEitherSecretAndStillRefusesAThirdOne() {
        WebhookVerifier.Options rotating = options().previousSecret("whsec_old");
        assertEquals(
                "u1", WebhookVerifier.verify(BODY, headers(NOW, sign("whsec_old", NOW, BODY)), rotating).uuid());
        assertEquals(
                "u1", WebhookVerifier.verify(BODY, headers(NOW, sign(SECRET, NOW, BODY)), rotating).uuid());
        assertThrows(
                SignatureException.class,
                () -> WebhookVerifier.verify(BODY, headers(NOW, sign("whsec_other", NOW, BODY)), rotating));
    }

    @Test
    void everyKindHasAGeneratedAccessorAndTheRuntimeNamesNoModel() throws IOException {
        assertTrue(WebhookKinds.class.isAssignableFrom(WebhookEvent.class), "the accessors come from the generator");
        for (Facts.WebhookKind kind : Facts.WEBHOOK_KINDS.values()) {
            boolean found = false;
            for (Method m : WebhookKinds.class.getDeclaredMethods()) {
                found |= m.isDefault() && m.getParameterCount() == 0 && m.getReturnType() == kind.model();
            }
            assertTrue(found, "no generated accessor for the " + kind.kind() + " kind");
            for (Method m : WebhookEvent.class.getDeclaredMethods()) {
                assertTrue(
                        m.getReturnType() != kind.model(),
                        "WebhookEvent." + m.getName() + " repeats the " + kind.kind() + " kind by hand");
            }
        }
        String src = Files.readString(Path.of("src/main/java/com/oblodai/webhooks/WebhookEvent.java"));
        assertFalse(src.contains("com.oblodai.generated.models."), "WebhookEvent names no model of a kind");
        for (String kind : Facts.KNOWN_WEBHOOK_KINDS) {
            assertFalse(src.contains("{@code " + kind + "}"), "WebhookEvent lists the kind " + kind + " by hand");
        }
        // The public names stay, and each goes through as(): a wallet event is not a payment.
        WebhookEvent wallet = new WebhookEvent(Map.of("type", "wallet"));
        assertThrows(IllegalStateException.class, wallet::asPayment);
        assertThrows(IllegalStateException.class, wallet::asConversion);
    }
}
