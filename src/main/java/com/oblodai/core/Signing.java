package com.oblodai.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Request signing — the exact recipe the gateway verifies:
 *
 * <pre>
 *   canonical = ts "\n" METHOD "\n" requestUri "\n" idempotencyKey "\n" body
 *   signature = hex(HMAC-SHA256(secret, canonical))
 * </pre>
 *
 * <ul>
 *   <li>{@code ts} is unix seconds; the gateway accepts ±300 s of skew.
 *   <li>{@code requestUri} is path plus raw query ({@code /v1/x?limit=1}), never the origin.
 *   <li>The idempotency slot is the empty string when no {@code Idempotency-Key} header is sent —
 *       empty, not absent: the newline is always there.
 *   <li>{@code body} is the byte-exact request body; GETs sign an empty body.
 * </ul>
 *
 * <p>Pure: no clock, no I/O. The unit tests replay the vectors the gateway's own test suite exports
 * in {@code contract/contract.json}.
 */
public final class Signing {

    /** Public id of the signing key. */
    public static final String HEADER_PUBLIC_ID = "X-Public-Id";

    /** Hex HMAC of the canonical string. */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /** Unix seconds the signature was made at. */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /** Deduplication key of a write the gateway caches. */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Admin token of a self-hosted gateway; only merchant provisioning uses it. */
    public static final String HEADER_ADMIN_TOKEN = "X-Admin-Token";

    /** Clock skew the gateway tolerates, in seconds. */
    public static final int SIGNATURE_SKEW_SECONDS = 300;

    private Signing() {}

    /** The string that gets signed. Exposed so a mismatch can be diffed against the gateway's log. */
    public static String canonicalString(
            long ts, String method, String requestUri, String idempotencyKey, byte[] body) {
        return ts
                + "\n"
                + method.toUpperCase(java.util.Locale.ROOT)
                + "\n"
                + requestUri
                + "\n"
                + (idempotencyKey == null ? "" : idempotencyKey)
                + "\n"
                + new String(body == null ? new byte[0] : body, StandardCharsets.UTF_8);
    }

    /** Convenience overload for a body that is already a string. */
    public static String canonicalString(
            long ts, String method, String requestUri, String idempotencyKey, String body) {
        return canonicalString(
                ts, method, requestUri, idempotencyKey, bytes(body == null ? "" : body));
    }

    /** Hex HMAC-SHA256 of the canonical string, signed over the exact body bytes. */
    public static String signRequest(
            String secret, long ts, String method, String requestUri, String idempotencyKey, byte[] body) {
        Mac mac = mac(secret);
        String prefix =
                ts
                        + "\n"
                        + method.toUpperCase(java.util.Locale.ROOT)
                        + "\n"
                        + requestUri
                        + "\n"
                        + (idempotencyKey == null ? "" : idempotencyKey)
                        + "\n";
        mac.update(bytes(prefix));
        if (body != null) mac.update(body);
        return hex(mac.doFinal());
    }

    /** Convenience overload for a body that is already a string. */
    public static String signRequest(
            String secret, long ts, String method, String requestUri, String idempotencyKey, String body) {
        return signRequest(secret, ts, method, requestUri, idempotencyKey, bytes(body == null ? "" : body));
    }

    /**
     * Webhook signature, as the gateway's dispatcher makes it:
     *
     * <pre>signature = hex(HMAC-SHA256(secret, "&lt;unix ts&gt;." + payload))</pre>
     *
     * <p>The payload is signed verbatim, so a verifier must use the raw request bytes, never a
     * re-encoded parse of them.
     */
    public static String signWebhook(String secret, long ts, byte[] payload) {
        Mac mac = mac(secret);
        mac.update(bytes(ts + "."));
        if (payload != null) mac.update(payload);
        return hex(mac.doFinal());
    }

    /** Convenience overload for a payload that is already a string. */
    public static String signWebhook(String secret, long ts, String payload) {
        return signWebhook(secret, ts, bytes(payload == null ? "" : payload));
    }

    /** Constant-time comparison of two hex signatures. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(bytes(a), bytes(b));
    }

    private static Mac mac(String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(bytes(secret), "HmacSHA256"));
            return mac;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable in this JVM", e);
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String hex(byte[] raw) {
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}
