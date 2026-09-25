package com.oblodai.core;

import com.oblodai.generated.SigningProtocol;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Request signing — the exact recipe the gateway verifies, as the contract's {@code
 * x-oblodai-signing} declares it ({@link SigningProtocol}):
 *
 * <pre>
 *   canonical = the parts of SigningProtocol.REQUEST_CANONICAL_ORDER joined by REQUEST_CANONICAL_SEPARATOR
 *   signature = hex(HMAC-SHA256(secret, canonical))
 * </pre>
 *
 * <ul>
 *   <li>{@code ts} is unix seconds; the gateway accepts {@value SigningProtocol#SKEW_SECONDS} s of
 *       skew either way.
 *   <li>{@code METHOD} is upper case.
 *   <li>{@code request_uri} is path plus raw query ({@code /v1/x?limit=1}), never the origin.
 *   <li>{@code idempotency_key} is the empty string when no {@value
 *       SigningProtocol#HEADER_IDEMPOTENCY_KEY} header is sent — empty, not absent: the
 *       separator is always there.
 *   <li>{@code body} is the byte-exact request body; GETs sign an empty body.
 * </ul>
 *
 * <p>Pure: no clock, no I/O. The header names, the order of the parts and the separators are the
 * generated {@link SigningProtocol}; the constants here are aliases kept for 1.x callers. The
 * conformance tests replay the core's vectors from the contract.
 */
public final class Signing {

    /** Public id of the signing key: {@link SigningProtocol#HEADER_PUBLIC_ID}. */
    public static final String HEADER_PUBLIC_ID = SigningProtocol.HEADER_PUBLIC_ID;

    /** Hex HMAC of the canonical string: {@link SigningProtocol#HEADER_SIGNATURE}. */
    public static final String HEADER_SIGNATURE = SigningProtocol.HEADER_SIGNATURE;

    /** Unix seconds the signature was made at: {@link SigningProtocol#HEADER_TIMESTAMP}. */
    public static final String HEADER_TIMESTAMP = SigningProtocol.HEADER_TIMESTAMP;

    /**
     * Deduplication key of a write the gateway caches: {@link
     * SigningProtocol#HEADER_IDEMPOTENCY_KEY}.
     */
    public static final String HEADER_IDEMPOTENCY_KEY = SigningProtocol.HEADER_IDEMPOTENCY_KEY;

    /**
     * Admin token of a self-hosted gateway; only merchant provisioning uses it. Not part of the
     * signing protocol, so the contract's {@code x-oblodai-signing} does not carry it.
     */
    public static final String HEADER_ADMIN_TOKEN = "X-Admin-Token";

    /** Clock skew the gateway tolerates, in seconds: {@link SigningProtocol#SKEW_SECONDS}. */
    public static final int SIGNATURE_SKEW_SECONDS = SigningProtocol.SKEW_SECONDS;

    private Signing() {}

    /** The string that gets signed. Exposed so a mismatch can be diffed against the gateway's log. */
    public static String canonicalString(
            long ts, String method, String requestUri, String idempotencyKey, byte[] body) {
        return canonicalString(
                SigningProtocol.REQUEST_CANONICAL_ORDER,
                SigningProtocol.REQUEST_CANONICAL_SEPARATOR,
                ts, method, requestUri, idempotencyKey, body);
    }

    /** The canonical string by the given order and separator; the public one passes the generated. */
    static String canonicalString(
            List<String> order,
            String separator,
            long ts,
            String method,
            String requestUri,
            String idempotencyKey,
            byte[] body) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        join(order, separator, part -> requestPart(part, ts, method, requestUri, idempotencyKey, body), out::writeBytes);
        return out.toString(StandardCharsets.UTF_8);
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
        join(
                SigningProtocol.REQUEST_CANONICAL_ORDER,
                SigningProtocol.REQUEST_CANONICAL_SEPARATOR,
                part -> requestPart(part, ts, method, requestUri, idempotencyKey, body),
                mac::update);
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
     * <pre>
     *   canonical = the parts of SigningProtocol.WEBHOOK_CANONICAL_ORDER joined by WEBHOOK_CANONICAL_SEPARATOR
     *   signature = hex(HMAC-SHA256(secret, canonical))
     * </pre>
     *
     * <p>The payload is signed verbatim, so a verifier must use the raw request bytes, never a
     * re-encoded parse of them.
     */
    public static String signWebhook(String secret, long ts, byte[] payload) {
        return signWebhook(
                SigningProtocol.WEBHOOK_CANONICAL_ORDER, SigningProtocol.WEBHOOK_CANONICAL_SEPARATOR, secret, ts, payload);
    }

    /** The webhook signature by the given order and separator; the public one passes the generated. */
    static String signWebhook(List<String> order, String separator, String secret, long ts, byte[] payload) {
        Mac mac = mac(secret);
        join(
                order,
                separator,
                part ->
                        switch (part) {
                            case "ts" -> bytes(Long.toString(ts));
                            case "payload" -> payload == null ? new byte[0] : payload;
                            default -> throw unknownPart("webhook", part);
                        },
                mac::update);
        return hex(mac.doFinal());
    }

    /** Convenience overload for a payload that is already a string. */
    public static String signWebhook(String secret, long ts, String payload) {
        return signWebhook(secret, ts, bytes(payload == null ? "" : payload));
    }

    /** One part of a request's canonical string, by its name in {@code x-oblodai-signing}. */
    private static byte[] requestPart(
            String part, long ts, String method, String requestUri, String idempotencyKey, byte[] body) {
        return switch (part) {
            case "ts" -> bytes(Long.toString(ts));
            case "METHOD" -> bytes(method.toUpperCase(java.util.Locale.ROOT));
            case "request_uri" -> bytes(requestUri);
            case "idempotency_key" -> bytes(idempotencyKey == null ? "" : idempotencyKey);
            case "body" -> body == null ? new byte[0] : body;
            default -> throw unknownPart("request", part);
        };
    }

    /** Feeds the parts in {@code order}, {@code separator} between them, to {@code sink}. */
    private static void join(
            java.util.List<String> order,
            String separator,
            java.util.function.Function<String, byte[]> part,
            java.util.function.Consumer<byte[]> sink) {
        byte[] sep = bytes(separator);
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sink.accept(sep);
            sink.accept(part.apply(order.get(i)));
        }
    }

    /** The generator refuses a canonical part it does not know, so this means a stale runtime. */
    private static IllegalStateException unknownPart(String side, String part) {
        return new IllegalStateException(
                "the " + side + " canonical string has a part \"" + part + "\" this runtime cannot fill");
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
