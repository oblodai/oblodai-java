package com.oblodai.webhooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Json;
import com.oblodai.core.Signing;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.models.WebhookEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The two things {@link WebhookVerifier} does to the delivery itself: compare the MACs, and read the
 * body. Split out so the verifier reads as the order of its checks — headers, MAC, freshness, body —
 * rather than as the mechanics of each.
 */
final class WebhookPayloads {

    private static final ObjectMapper MAPPER = Json.newMapper();

    private WebhookPayloads() {}

    /**
     * Constant-time comparison of the delivered signatures against the current secret first and the
     * retiring one second. A merchant who has not swapped the stored secret yet verifies the Prev
     * header with it; one who already swapped but kept the old copy verifies the main header with
     * the new secret. Both hold during the overlap, so both are accepted.
     */
    static boolean matches(
            byte[] body,
            long timestamp,
            String signature,
            String secret,
            String previousSecret,
            String previousSignature) {
        String current = normalizeSignature(signature);
        String previous = normalizeSignature(previousSignature);
        List<String[]> candidates = new ArrayList<>();
        candidates.add(new String[] {current, secret});
        if (previous != null) candidates.add(new String[] {previous, secret});
        if (previousSecret != null) {
            candidates.add(new String[] {current, previousSecret});
            if (previous != null) candidates.add(new String[] {previous, previousSecret});
        }
        boolean matched = false;
        for (String[] candidate : candidates) {
            if (candidate[0] == null) continue;
            String expected = Signing.signWebhook(candidate[1], timestamp, body);
            // No early exit: every candidate is compared so the work does not depend on which one
            // matched.
            matched |= Signing.constantTimeEquals(candidate[0], expected);
        }
        return matched;
    }

    /**
     * A delivered signature as it must look to be compared: surrounding whitespace trimmed, hex in
     * either case accepted, an {@code 0x} prefix refused. Anything that is not plain hex is not a
     * signature this SDK will compare — it returns null and the candidate is skipped.
     */
    static String normalizeSignature(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.regionMatches(true, 0, "0x", 0, 2)) return null;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.matches("[0-9a-f]+") ? lower : null;
    }

    static WebhookEvent parse(byte[] rawBody) {
        JsonNode body;
        try {
            body = MAPPER.readTree(rawBody == null ? new byte[0] : rawBody);
        } catch (Exception e) {
            throw badPayload("the delivery body is not JSON");
        }
        if (body == null || !body.isObject()) {
            throw badPayload("the delivery body is not a JSON object");
        }
        if (!body.path("type").isTextual() || !body.path("uuid").isTextual()) {
            throw badPayload("the delivery body lacks the type/uuid fields every event carries");
        }
        try {
            return MAPPER.treeToValue(body, WebhookEvent.class);
        } catch (Exception e) {
            throw badPayload("the delivery body could not be decoded as an event");
        }
    }


    static WebhookPayloadException badPayload(String what) {
        return new WebhookPayloadException(
                what
                        + " — the signature verified, so this delivery is authentic: answer 400 and"
                        + " investigate, do not answer 401");
    }
}
