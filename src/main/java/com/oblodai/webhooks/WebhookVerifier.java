package com.oblodai.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Json;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.models.UnknownEvent;
import com.oblodai.models.WebhookEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
 *   X-Webhook-Test:           true on a rehearsal delivery — no money moved
 * </pre>
 *
 * <p>Always verify over the <b>raw request bytes</b>. A re-serialized parse will not match: JSON
 * round-trips are not byte-stable, and the signature covers bytes.
 *
 * <p>The order of the checks is deliberate: headers, then the MAC, then freshness, then the body.
 * The freshness window is only consulted once the MAC has proved the delivery is ours, so an
 * unauthenticated caller cannot use the timestamp check to learn anything. A body that verified but
 * cannot be read is a contract failure ({@code webhook.bad_payload}), not a signature failure — a
 * receiver that answers 401 to {@code webhook.*} signature errors must not reject an authentic
 * delivery it merely failed to decode.
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

    /** {@code true} on a rehearsal delivery; the body then carries {@code test: true} as well. */
    public static final String HEADER_TEST = "X-Webhook-Test";

    /** Default freshness window, in seconds, on either side of the delivery timestamp. */
    public static final long DEFAULT_TOLERANCE_SECONDS = 300;

    /** A delivery that verified but whose body could not be read as an event. */
    public static final String BAD_PAYLOAD = ContractException.WEBHOOK_BAD_PAYLOAD;

    private static final ObjectMapper MAPPER = Json.newMapper();

    private WebhookVerifier() {}

    /** Verification settings: the secret, an optional outgoing secret, and the freshness window. */
    public static final class Options {
        private final String secret;
        private final String previousSecret;
        private final long toleranceMillis;
        private final LongSupplier clock;

        private Options(String secret, String previousSecret, long toleranceMillis, LongSupplier clock) {
            this.secret = secret;
            this.previousSecret = previousSecret;
            this.toleranceMillis = toleranceMillis;
            this.clock = clock;
        }

        /**
         * During a rotation, keep the outgoing secret here. Deliveries queued before the rotation
         * stay signed with it for their whole retry life (about 26 hours), so keep it at least that
         * long after rotating.
         *
         * @param previousSecret the secret being retired; must not be empty when supplied
         * @return a copy carrying it
         * @throws ConfigException when the value is present but empty
         */
        public Options previousSecret(String previousSecret) {
            if (previousSecret != null && previousSecret.isEmpty()) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG,
                        "previousSecret was supplied but is empty; pass null when there is no rotation"
                                + " in progress",
                        "previousSecret");
            }
            return new Options(secret, previousSecret, toleranceMillis, clock);
        }

        /**
         * How far the delivery timestamp may be from now. {@link Duration#ZERO} disables the check —
         * only sensible when replaying a recorded delivery in a test. Sub-second windows are kept as
         * written; a negative one is refused rather than silently disabling the check.
         *
         * @param tolerance the window
         * @return a copy carrying it
         * @throws ConfigException when the duration is null or negative
         */
        public Options tolerance(Duration tolerance) {
            if (tolerance == null) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG, "tolerance must not be null", "tolerance");
            }
            if (tolerance.isNegative()) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG,
                        "tolerance must not be negative (got "
                                + tolerance
                                + "); pass Duration.ZERO to disable the freshness check deliberately",
                        "tolerance");
            }
            return new Options(secret, previousSecret, tolerance.toMillis(), clock);
        }

        /**
         * Injects the clock, in unix seconds, so a test can verify a recorded delivery.
         *
         * @param clock source of the current unix time in seconds
         * @return a copy carrying it
         */
        public Options clock(LongSupplier clock) {
            if (clock == null) {
                throw new ConfigException(ConfigException.BAD_CONFIG, "clock must not be null", "clock");
            }
            return new Options(secret, previousSecret, toleranceMillis, clock);
        }

        /** The freshness window in milliseconds; 0 means the check is disabled. */
        public long toleranceMillis() {
            return toleranceMillis;
        }
    }

    /**
     * Verification settings for one endpoint secret.
     *
     * @param secret the endpoint secret from {@code webhooks().register(...)} or {@code
     *     rotateSecret()}
     * @return the settings, with a ±300 s freshness window
     * @throws ConfigException when the secret is null or blank — an empty key would "verify"
     *     whatever a stranger signed with an empty key
     */
    public static Options options(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "a webhook secret is required: pass the value webhooks().register(...) or"
                            + " rotateSecret() returned",
                    "secret");
        }
        return new Options(
                secret,
                null,
                DEFAULT_TOLERANCE_SECONDS * 1000L,
                () -> System.currentTimeMillis() / 1000L);
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
     * @throws WebhookPayloadException when the delivery is authentic but its body is not an event
     */
    public static WebhookDeliveryInfo verifyDelivery(
            byte[] rawBody, WebhookHeaders headers, Options options) {
        if (options == null) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "verification options are required: WebhookVerifier.options(secret)",
                    "options");
        }
        if (headers == null) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG, "the delivery headers are required", "headers");
        }
        byte[] body = rawBody == null ? new byte[0] : rawBody;

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

        // 1. The MAC, before anything the timestamp says: the freshness window must never be an
        //    oracle an unauthenticated caller can probe.
        String previousSignature = headers.first(HEADER_SIGNATURE_PREV);
        if (!WebhookPayloads.matches(
                body, timestamp, signature, options.secret, options.previousSecret, previousSignature)) {
            throw new SignatureException(
                    SignatureException.BAD_SIGNATURE, "signature does not match the body");
        }

        // 2. Freshness, now that the delivery is known to be ours.
        if (options.toleranceMillis > 0) {
            long nowMillis = options.clock.getAsLong() * 1000L;
            if (Math.abs(nowMillis - timestamp * 1000L) > options.toleranceMillis) {
                throw new SignatureException(
                        SignatureException.STALE_TIMESTAMP,
                        "delivery timestamp "
                                + timestamp
                                + " is outside the ±"
                                + options.toleranceMillis
                                + "ms window");
            }
        }

        // 3. The body itself.
        WebhookEvent event = parse(body);
        String eventTimeHeader = headers.first(HEADER_EVENT_TIME);
        Long eventTime = null;
        if (eventTimeHeader != null && eventTimeHeader.trim().matches("\\d+")) {
            eventTime = Long.valueOf(eventTimeHeader.trim());
        }
        boolean isTest = isTrue(headers.first(HEADER_TEST)) || isTestEvent(event);
        return new WebhookDeliveryInfo(
                event,
                headers.first(HEADER_ID),
                headers.first(HEADER_EVENT),
                eventTime,
                timestamp,
                isTest);
    }

    private static boolean isTrue(String header) {
        return header != null && header.trim().equalsIgnoreCase("true");
    }

    /**
     * Parses a delivery body into a typed event. Only call this on bytes you have verified — it
     * checks the shape, not the origin.
     *
     * @param rawBody the delivery body
     * @return the event; a {@code type} this snapshot does not know decodes to
     *     {@link com.oblodai.models.UnknownEvent} rather than failing
     * @throws WebhookPayloadException ({@code webhook.bad_payload}) when the body is not an event
     *     object
     */
    public static WebhookEvent parse(byte[] rawBody) {
        return WebhookPayloads.parse(rawBody);
    }

    /**
     * Parses a delivery body given as text.
     *
     * @param rawBody the delivery body
     * @return the event
     */
    public static WebhookEvent parse(String rawBody) {
        return parse(rawBody == null ? new byte[0] : rawBody.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether the event is one of the kinds this SDK snapshot models. A {@code false} means the
     * gateway has grown a new event type: the delivery is still verified and readable through
     * {@link UnknownEvent}, and a receiver should ignore it rather than fail.
     *
     * @param event the event just verified, or null
     * @return true for a payment, payout or wallet event
     */
    public static boolean isKnownEvent(WebhookEvent event) {
        return event != null && !(event instanceof UnknownEvent);
    }

    /**
     * Rehearsal deliveries ({@code webhooks().test(...)}, sandbox) are signed exactly like live ones
     * and carry {@code test: true}. Never act on one as if money moved.
     *
     * @param event the event just verified, or null
     * @return true when the event is a rehearsal, not a real state change
     */
    public static boolean isTestEvent(WebhookEvent event) {
        return event != null && Boolean.TRUE.equals(event.test());
    }

    /**
     * Deliveries can arrive out of order (a retried {@code paid} after a {@code refund}). Keep the
     * last {@code sequence} you processed per object and skip anything not newer.
     *
     * @param event the event just verified, or null
     * @param lastProcessedSequence the highest sequence you have already applied, or null
     * @return true when this event is not newer than what you have already applied; false when
     *     either side is unknown — never throws
     */
    public static boolean isStale(WebhookEvent event, Long lastProcessedSequence) {
        if (event == null || lastProcessedSequence == null || event.sequence() == null) return false;
        return event.sequence() <= lastProcessedSequence;
    }
}
