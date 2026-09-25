package com.oblodai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Signing builds its strings by the order and separator it is handed — the generated ones in the
 * public methods — and not by an order of its own: a different order and separator give a
 * different, predictable string.
 */
class SigningOrderTest {

    @Test
    void requestCanonicalFollowsTheGivenOrderAndSeparator() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        assertEquals(
                "{}|k|/v1/x|POST|7",
                Signing.canonicalString(
                        List.of("body", "idempotency_key", "request_uri", "METHOD", "ts"), "|", 7, "post", "/v1/x", "k", body));
    }

    @Test
    void webhookSignatureFollowsTheGivenOrderAndSeparator() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("s".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder want = new StringBuilder();
        for (byte b : mac.doFinal("body~9".getBytes(StandardCharsets.UTF_8))) {
            want.append(String.format("%02x", b));
        }
        assertEquals(
                want.toString(),
                Signing.signWebhook(List.of("payload", "ts"), "~", "s", 9, "body".getBytes(StandardCharsets.UTF_8)));
    }
}
