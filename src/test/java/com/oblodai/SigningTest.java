package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.oblodai.core.Signing;
import com.oblodai.support.Contract;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The signature recipe, against the vectors the gateway's own test suite exports. If one of these
 * fails, every signed call this SDK makes is a 401 — which is exactly how the 1.x line broke.
 */
class SigningTest {

    @TestFactory
    List<DynamicTest> requestVectors() {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode vector : Contract.contract().path("signing_vectors")) {
            tests.add(
                    DynamicTest.dynamicTest(
                            vector.path("name").asText(),
                            () -> {
                                String idempotencyKey = vector.path("idempotency_key").asText("");
                                String body = vector.path("body").asText("");
                                assertEquals(
                                        vector.path("canonical").asText(),
                                        Signing.canonicalString(
                                                vector.path("ts").asLong(),
                                                vector.path("method").asText(),
                                                vector.path("request_uri").asText(),
                                                idempotencyKey,
                                                body),
                                        "canonical string");
                                assertEquals(
                                        vector.path("signature").asText(),
                                        Signing.signRequest(
                                                vector.path("secret").asText(),
                                                vector.path("ts").asLong(),
                                                vector.path("method").asText(),
                                                vector.path("request_uri").asText(),
                                                idempotencyKey,
                                                body),
                                        "signature");
                            }));
        }
        return tests;
    }

    @TestFactory
    List<DynamicTest> webhookVectors() {
        List<DynamicTest> tests = new ArrayList<>();
        int index = 0;
        for (JsonNode vector : Contract.contract().path("webhook_vectors")) {
            int at = index++;
            tests.add(
                    DynamicTest.dynamicTest(
                            "webhook vector " + at,
                            () ->
                                    assertEquals(
                                            vector.path("signature").asText(),
                                            Signing.signWebhook(
                                                    vector.path("secret").asText(),
                                                    vector.path("ts").asLong(),
                                                    vector.path("payload").asText()))));
        }
        return tests;
    }

    @Test
    void idempotencySlotIsEmptyNotAbsent() {
        String withNull = Signing.signRequest("s", 1, "POST", "/v1/x", null, "{}");
        String withEmpty = Signing.signRequest("s", 1, "POST", "/v1/x", "", "{}");
        assertEquals(withNull, withEmpty);
        assertEquals("1\nPOST\n/v1/x\n\n{}", Signing.canonicalString(1, "POST", "/v1/x", null, "{}"));
    }

    @Test
    void signsTheBodyBytesSoTextAndBytesAgree() {
        String body = "{\"additional_data\":\"тест\"}";
        assertEquals(
                Signing.signRequest("s", 5, "POST", "/v1/payment", null, body),
                Signing.signRequest(
                        "s", 5, "POST", "/v1/payment", null, body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void lowercasesNothingButUppercasesTheMethod() {
        assertEquals(
                Signing.signRequest("s", 5, "POST", "/v1/x", null, ""),
                Signing.signRequest("s", 5, "post", "/v1/x", null, ""));
    }
}
