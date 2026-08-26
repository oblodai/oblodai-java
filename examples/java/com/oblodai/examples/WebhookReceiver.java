package com.oblodai.examples;

import com.oblodai.errors.SignatureException;
import com.oblodai.models.PaymentEvent;
import com.oblodai.models.PayoutEvent;
import com.oblodai.models.WalletEvent;
import com.oblodai.models.WebhookEvent;
import com.oblodai.webhooks.WebhookDeliveryInfo;
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
 * <p>Three rules the gateway's delivery model imposes:
 *
 * <ol>
 *   <li>Verify over the raw request bytes. A framework that parsed and re-serialized the body has
 *       already changed them, and the signature will not match.
 *   <li>Deduplicate on {@code X-Webhook-Id} — it is stable across the gateway's retries.
 *   <li>Drop stale events. Deliveries can arrive out of order; keep the last {@code sequence} you
 *       applied per object and skip anything not newer.
 * </ol>
 *
 * <pre>
 * OBLODAI_WEBHOOK_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.WebhookReceiver
 * </pre>
 */
public final class WebhookReceiver {

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> LAST_SEQUENCE = new ConcurrentHashMap<>();

    private WebhookReceiver() {}

    /**
     * @param args optional: the port to listen on
     * @throws IOException when the port cannot be bound
     */
    public static void main(String[] args) throws IOException {
        String secret = System.getenv("OBLODAI_WEBHOOK_SECRET");
        // During a rotation keep the retiring secret for about 26 hours: deliveries queued before
        // the rotation stay signed with it for their whole retry life.
        String previousSecret = System.getenv("OBLODAI_WEBHOOK_SECRET_PREVIOUS");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(
                "/oblodai/webhook",
                exchange -> {
                    byte[] rawBody = exchange.getRequestBody().readAllBytes();
                    try {
                        WebhookVerifier.Options options = WebhookVerifier.options(secret);
                        if (previousSecret != null) options = options.previousSecret(previousSecret);

                        WebhookDeliveryInfo delivery =
                                WebhookVerifier.verifyDelivery(
                                        rawBody,
                                        WebhookHeaders.ofMulti(exchange.getRequestHeaders()),
                                        options);
                        handle(delivery);
                        // Answer 2xx quickly; the gateway retries anything else.
                        exchange.sendResponseHeaders(200, -1);
                    } catch (SignatureException rejected) {
                        // Not from the gateway (or too old to trust): never process it.
                        System.err.println("rejected delivery: " + rejected.code());
                        exchange.sendResponseHeaders(400, -1);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        System.out.println("listening on http://localhost:" + port + "/oblodai/webhook");
    }

    private static void handle(WebhookDeliveryInfo delivery) {
        if (delivery.id() != null && !SEEN.add(delivery.id())) {
            System.out.println("duplicate delivery " + delivery.id() + ", already applied");
            return;
        }
        if (delivery.isTest()) {
            // A rehearsal from webhooks().test(...) or the sandbox: signed like a live delivery, but
            // no money moved. Log it, answer 2xx, and never touch the order.
            System.out.println("test delivery " + delivery.eventType() + ", not applied");
            return;
        }
        WebhookEvent event = delivery.event();
        Long last = LAST_SEQUENCE.get(event.uuid());
        if (WebhookVerifier.isStale(event, last)) {
            System.out.println("stale event for " + event.uuid() + ", ignored");
            return;
        }
        LAST_SEQUENCE.put(event.uuid(), event.sequence());

        // The event is a sealed union: pattern matching covers every kind the gateway sends.
        if (event instanceof PaymentEvent payment) {
            System.out.println(
                    "invoice "
                            + payment.orderId()
                            + " is "
                            + payment.status()
                            + " ("
                            + payment.paymentAmount()
                            + " "
                            + payment.payerCurrency()
                            + ")");
        } else if (event instanceof PayoutEvent payout) {
            System.out.println("payout " + payout.uuid() + " is " + payout.status());
        } else if (event instanceof WalletEvent wallet) {
            System.out.println(
                    "wallet " + wallet.address() + " received " + wallet.paymentAmount());
        }
    }
}
