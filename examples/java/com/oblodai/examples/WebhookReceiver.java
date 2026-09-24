package com.oblodai.examples;

import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.generated.models.PaymentWebhook;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookEvent;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A webhook receiver, in the shape every framework reduces to: read the RAW bytes, verify, then act.
 *
 * <ol>
 *   <li>Verify over the raw request bytes; a re-serialized body no longer matches the signature.
 *   <li>Deduplicate on the event id ({@code X-Webhook-Event-Id}): the same for every retry and
 *       every resend of one state.
 *   <li>Drop stale events: keep the last {@code sequence} applied per object.
 * </ol>
 *
 * <pre>
 * OBLODAI_WEBHOOK_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.WebhookReceiver
 * </pre>
 */
public final class WebhookReceiver {

    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastSequence = new ConcurrentHashMap<>();
    private final WebhookVerifier.Options options;

    /**
     * @param secret the endpoint secret
     * @param previousSecret the retiring secret during a rotation, or null
     */
    public WebhookReceiver(String secret, String previousSecret) {
        WebhookVerifier.Options o = WebhookVerifier.options(secret);
        this.options = previousSecret == null ? o : o.previousSecret(previousSecret);
    }

    /**
     * @param args optional: the port to listen on
     * @throws IOException when the port cannot be bound
     */
    public static void main(String[] args) throws IOException {
        WebhookReceiver receiver =
                new WebhookReceiver(
                        System.getenv("OBLODAI_WEBHOOK_SECRET"), System.getenv("OBLODAI_WEBHOOK_SECRET_PREVIOUS"));
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(
                "/oblodai/webhook",
                exchange -> {
                    try (exchange) {
                        byte[] raw = exchange.getRequestBody().readAllBytes();
                        int status = receiver.handle(raw, WebhookHeaders.ofMulti(exchange.getRequestHeaders()));
                        exchange.sendResponseHeaders(status, -1);
                    }
                });
        server.start();
        System.out.println("listening on http://localhost:" + port + "/oblodai/webhook");
    }

    /**
     * @param rawBody the exact request bytes
     * @param headers the request headers
     * @return the HTTP status to answer with
     */
    public int handle(byte[] rawBody, WebhookHeaders headers) {
        WebhookDeliveryInfo delivery;
        try {
            delivery = WebhookVerifier.verifyDelivery(rawBody, headers, options);
        } catch (SignatureException rejected) {
            return 401; // not from the gateway, or too old to trust: never process it
        } catch (WebhookPayloadException unreadable) {
            return 400; // authentic, but not an event this receiver can read
        }
        String key = delivery.eventId() != null ? delivery.eventId() : delivery.id();
        if (key != null && !seen.add(key)) {
            return 200; // already applied
        }
        if (delivery.isTest()) {
            return 200; // a rehearsal: signed like a live delivery, but no money moved
        }
        WebhookEvent event = delivery.event();
        if (WebhookVerifier.isStale(event, lastSequence.get(event.uuid()))) {
            return 200;
        }
        if (event.sequence() != null) {
            lastSequence.put(event.uuid(), event.sequence());
        }
        switch (event.type()) {
            case "payment" -> {
                PaymentWebhook payment = event.asPayment();
                System.out.println("invoice " + payment.orderId() + " is " + payment.status());
            }
            case "payout" -> System.out.println("payout " + event.uuid() + " is " + event.asPayout().status());
            default -> System.out.println("a " + event.type() + " event; acknowledged");
        }
        return 200; // answer 2xx quickly; the gateway retries anything else
    }
}
