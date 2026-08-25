package com.oblodai.webhooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Json;
import com.oblodai.core.Signing;
import com.oblodai.errors.SignatureException;
import com.oblodai.models.WebhookEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Webhook verification. Usable on its own: no client, no API key, no network.
 *
 * <p>Deliveries are signed as:
 *
 * <pre>
 *   X-Webhook-Timestamp:      unix seconds
 *   X-Webhook-Signature:      hex(HMAC-SHA256(secret, "&lt;ts&gt;." + rawBody))
 *   X-Webhook-Signature-Prev: the same with the previous secret — only during a rotation overlap
 *   X-Webhook-Event:          invoice.&lt;status&gt; | payout.&lt;status&gt; | wallet.paid
 *   X-Webhook-Id:             stable per delivery, identical across retries — deduplicate on it
 *   X-Webhook-Event-Time:     unix seconds when the state change committed
 * </pre>
 *
 * <p>Always verify over the <b>raw request bytes</b>. A re-serialized parse will not match: JSON
 * round-trips are not byte-stable, and the signature covers bytes.
 *
 * <pre>{@code
 * WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
 *         rawBody, WebhookHeaders.of(request.getHeaders()), WebhookVerifier.options(secret));
 * if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
 *     markOrderPaid(payment.orderId());
 * }
 * }</pre>
 */
public final class WebhookVerifier {

    /** Unix seconds the delivery attempt was signed at. */
    public static final String HEADER_TIMESTAMP = "X-Webhook-Timestamp";

    /** Signature made with the endpoint's current secret. */
    public static final String HEADER_SIGNATURE = "X-Webhook-Signature";

    /** Signature made with the previous secret, during a rotation overlap. */
    public static final String HEADER_SIGNATURE_PREV = "X-Webhook-Signature-Prev";

    /** The event type of the delivery. */
    public static final String HEADER_EVENT = "X-Webhook-Event";

    /** Delivery id, stable across retries. */
    public static final String HEADER_ID = "X-Webhook-Id";

    /** Unix seconds the state change committed at. */
    public static final String HEADER_EVENT_TIME = "X-Webhook-Event-Time";

    /** Default freshness window, in seconds, on either side of the delivery timestamp. */
    public static final long DEFAULT_TOLERANCE_SECONDS = 300;

    private static final ObjectMapper MAPPER = Json.newMapper();

    private WebhookVerifier() {}

    /** Verification settings: the secret, an optional outgoing secret, and the freshness window. */
    public static final class Options {
        private final String secret;
        private final String previousSecret;
        private final long toleranceSeconds;
        private final LongSupplier clock;

        private Options(String secret, String previousSecret, long toleranceSeconds, LongSupplier clock) {
            this.secret = secret;
            this.previousSecret = previousSecret;
            this.toleranceSeconds = toleranceSeconds;
            this.clock = clock;
        }

        /**
         * During a rotation, keep the outgoing secret here. Deliveries queued before the rotation
         * stay signed with it for their whole retry life (about 26 hours), so keep it at least that
         * long after rotating.
         *
         * @param previousSecret the secret being retired
         * @return a copy carrying it
         */
        public Options previousSecret(String previousSecret) {
            return new Options(secret, previousSecret, toleranceSeconds, clock);
        }

        /**
         * How far the delivery timestamp may be from now. {@link Duration#ZERO} disables the check —
         * only sensible when replaying a recorded delivery in a test.
         *
         * @param tolerance the window
         * @return a copy carrying it
         */
        public Options tolerance(Duration tolerance) {
            return new Options(secret, previousSecret, tolerance.toSeconds(), clock);
        }

        /**
         * Injects the clock, in unix seconds, so a test can verify a recorded delivery.
         *
         * @param clock source of the current unix time in seconds
         * @return a copy carrying it
         */
        public Options clock(LongSupplier clock) {
            return new Options(secret, previousSecret, toleranceSeconds, clock);
        }
    }

    /**
     * Verification settings for one endpoint secret.
     *
     * @param secret the endpoint secret from {@code webhooks().register(...)} or {@code
     *     rotateSecret()}
     * @return the settings, with a ±300 s freshness window
     */
    public static Options options(String secret) {
        return new Options(
                secret, null, DEFAULT_TOLERANCE_SECONDS, () -> System.currentTimeMillis() / 1000L);
    }

    /**
     * Verifies a delivery and returns its event.
     *
     * @param rawBody the exact request bytes
     * @param headers the request headers
     * @param options the endpoint secret and freshness window
     * @return the verified event
     * @throws SignatureException when the signature, the timestamp or the headers do not hold up
     */
    public static WebhookEvent verify(byte[] rawBody, WebhookHeaders headers, Options options) {
        return verifyDelivery(rawBody, headers, options).event();
    }

    /**
     * Verifies a delivery given as text. Prefer the {@code byte[]} form: the signature covers bytes,
     * and a framework that decoded the body with the wrong charset would change them.
     *
     * @param rawBody the exact request body
     * @param headers the request headers
     * @param options the endpoint secret and freshness window
     * @return the verified event
     */
    public static WebhookEvent verify(String rawBody, WebhookHeaders headers, Options options) {
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), headers, options);
    }

    /**
     * Verifies a delivery and returns the event together with its delivery headers.
     *
     * @param rawBody the exact request bytes
     * @param headers the request headers
     * @param options the endpoint secret and freshness window
     * @return the event, the delivery id, the event type and the times
     * @throws SignatureException when the signature, the timestamp or the headers do not hold up
     */
    public static WebhookDeliveryInfo verifyDelivery(
            byte[] rawBody, WebhookHeaders headers, Options options) {
        String timestampHeader = headers.first(HEADER_TIMESTAMP);
        String signature = headers.first(HEADER_SIGNATURE);
        if (timestampHeader == null || signature == null) {
            throw new SignatureException(
                    SignatureException.MISSING_HEADER,
                    "missing " + HEADER_TIMESTAMP + " or " + HEADER_SIGNATURE);
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE, "timestamp header is not an integer");
        }

        if (options.toleranceSeconds > 0) {
            long now = options.clock.getAsLong();
            if (Math.abs(now - timestamp) > options.toleranceSeconds) {
                throw new SignatureException(
                        SignatureException.STALE_TIMESTAMP,
                        "delivery timestamp "
                                + timestamp
                                + " is outside the ±"
                                + options.toleranceSeconds
                                + "s window");
            }
        }

        String previousSignature = headers.first(HEADER_SIGNATURE_PREV);
        // A merchant who has not swapped the stored secret yet verifies the Prev header with it; one
        // who already swapped but kept the old copy verifies the main header with the new secret.
        // Both hold during the overlap, so both are accepted.
        List<String[]> candidates = new ArrayList<>();
        candidates.add(new String[] {signature, options.secret});
        if (previousSignature != null) candidates.add(new String[] {previousSignature, options.secret});
        if (options.previousSecret != null) {
            candidates.add(new String[] {signature, options.previousSecret});
            if (previousSignature != null) {
                candidates.add(new String[] {previousSignature, options.previousSecret});
            }
        }
        boolean matched = false;
        for (String[] candidate : candidates) {
            String expected = Signing.signWebhook(candidate[1], timestamp, rawBody);
            if (Signing.constantTimeEquals(
                    candidate[0].toLowerCase(java.util.Locale.ROOT), expected)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE, "signature does not match the body");
        }

        String eventTimeHeader = headers.first(HEADER_EVENT_TIME);
        Long eventTime = null;
        if (eventTimeHeader != null && eventTimeHeader.trim().matches("\\d+")) {
            eventTime = Long.valueOf(eventTimeHeader.trim());
        }
        return new WebhookDeliveryInfo(
                parse(rawBody), headers.first(HEADER_ID), headers.first(HEADER_EVENT), eventTime, timestamp);
    }

    /**
     * Parses a delivery body into a typed event. Only call this on bytes you have verified — it
     * checks the shape, not the origin.
     *
     * @param rawBody the delivery body
     * @return the event, one of the three concrete kinds
     * @throws SignatureException when the body is not a webhook event
     */
    public static WebhookEvent parse(byte[] rawBody) {
        JsonNode body;
        try {
            body = MAPPER.readTree(rawBody);
        } catch (Exception e) {
            throw new SignatureException(SignatureException.BAD_SIGNATURE, "body is not JSON");
        }
        if (body == null || !body.isObject() || !body.path("type").isTextual() || !body.path("uuid").isTextual()) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE,
                    "body lacks the type/uuid fields every event carries");
        }
        String type = body.get("type").asText();
        if (!type.equals("payment") && !type.equals("payout") && !type.equals("wallet")) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE, "unknown event type \"" + type + "\"");
        }
        try {
            return MAPPER.treeToValue(body, WebhookEvent.class);
        } catch (Exception e) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE, "event body could not be decoded: " + e.getMessage());
        }
    }

    /**
     * Parses a delivery body given as text.
     *
     * @param rawBody the delivery body
     * @return the event
     */
    public static WebhookEvent parse(String rawBody) {
        return parse(rawBody.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Deliveries can arrive out of order (a retried {@code paid} after a {@code refund}). Keep the
     * last {@code sequence} you processed per object and skip anything not newer.
     *
     * @param event the event just verified
     * @param lastProcessedSequence the highest sequence you have already applied, or null
     * @return true when this event is not newer than what you have already applied
     */
    public static boolean isStale(WebhookEvent event, Long lastProcessedSequence) {
        if (lastProcessedSequence == null || event.sequence() == null) return false;
        return event.sequence() <= lastProcessedSequence;
    }
}
