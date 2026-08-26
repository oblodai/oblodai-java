package com.oblodai.examples;

import com.oblodai.Money;
import com.oblodai.Oblodai;
import com.oblodai.OblodaiAsync;
import com.oblodai.RequestOptions;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;
import com.oblodai.contract.requests.MerchantsRequest;
import com.oblodai.contract.requests.PaymentHistoryRequest;
import com.oblodai.contract.requests.PaymentInfoRequest;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.contract.requests.PayoutHistoryRequest;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.core.Page;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.models.MerchantOnboarded;
import com.oblodai.models.Payment;
import com.oblodai.models.PaymentEvent;
import com.oblodai.models.Payout;
import com.oblodai.models.WebhookEndpoint;
import com.oblodai.models.WebhookEvent;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Every code snippet the README and MIGRATION-1.3 show, compiled.
 *
 * <p>Documentation that no longer compiles is worse than no documentation: it is a promise the SDK
 * has stopped keeping, and nobody notices until a reader copies it. The examples are compiled with
 * the tests, so this file fails the build the day a snippet in the docs goes stale.
 *
 * <p>Nothing here is executed — {@code main} does nothing. The methods are the snippets.
 */
public final class DocSnippets {

    private DocSnippets() {}

    /**
     * @param args ignored; this file exists to be compiled, not run
     */
    public static void main(String[] args) {
        System.out.println("the documentation snippets compile against this SDK");
    }

    /** README — "Start in the sandbox". */
    static void startInTheSandbox() {
        Oblodai oblodai =
                Oblodai.builder()
                        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
                        .secret(System.getenv("OBLODAI_SECRET"))
                        .build();

        Payment invoice =
                oblodai.payments()
                        .create(
                                new PaymentRequest()
                                        .amount("25")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId("order-1001")
                                        .urlCallback("https://shop.example/oblodai/webhook"));

        System.out.println(invoice.url() + " " + invoice.address() + " " + invoice.status());

        // Pricing in fiat: currency is what you charge, toCurrency the asset the payer sends.
        new PaymentRequest().amount("25").currency("USD").toCurrency("USDT");
    }

    /** README — "Two keys". */
    static void twoKeys(String id, String secret, String payoutId, String payoutSecret) {
        Oblodai.builder().publicId(id).secret(secret).payoutKey(payoutId, payoutSecret).build();
    }

    /** README — per-call options and lookups. */
    static void requestOptions(Oblodai oblodai, PayoutRequest request, String orderId) {
        oblodai.payouts()
                .create(
                        request,
                        RequestOptions.of()
                                .idempotencyKey(orderId)
                                .timeout(Duration.ofSeconds(10))
                                .deadline(Duration.ofSeconds(45))
                                .header("X-Tenant", "acme")
                                .preferPayoutKey(true));

        oblodai.payments().info("d1b0…");
        oblodai.payments().info(new PaymentInfoRequest().orderId("order-1001"));
    }

    /** README — "Lists". */
    static void lists(Oblodai oblodai) {
        Page<Payment> page =
                oblodai.payments().history(new PaymentHistoryRequest().limit(50)).firstPage();
        System.out.println(page.items().size());

        for (Payout payout : oblodai.payouts().history(new PayoutHistoryRequest().status("confirmed"))) {
            process(payout);
        }

        List<Payout> refunds =
                oblodai.payouts().history(new PayoutHistoryRequest().kind("refund")).all(1000);
        Stream<Payment> stream = oblodai.payments().history().stream();
        System.out.println(refunds.size() + " " + stream.count());
    }

    private static void process(Payout payout) {
        System.out.println(payout.uuid());
    }

    /** README — "Errors". */
    static void errors(Oblodai oblodai, PayoutRequest request) {
        try {
            oblodai.payouts().create(request);
        } catch (OblodaiException e) {
            switch (e.code()) {
                case "payout.insufficient_funds", "payout.funds_maturing" ->
                        scheduleRetry(e.retryAfter() == null ? 60 : e.retryAfter());
                default -> throw e;
            }
        }
    }

    private static void scheduleRetry(int seconds) {
        System.out.println("retry in " + seconds + "s");
    }

    /** README — "Webhooks". */
    static void webhooks(byte[] rawBody, Map<String, String> headers, String secret) {
        WebhookDeliveryInfo delivery =
                WebhookVerifier.verifyDelivery(
                        rawBody, WebhookHeaders.of(headers), WebhookVerifier.options(secret));

        if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
            markOrderPaid(payment.orderId());
        }
        if (delivery.isTest()) return;
        System.out.println(WebhookVerifier.isStale(delivery.event(), 41L));
    }

    private static void markOrderPaid(String orderId) {
        System.out.println("paid: " + orderId);
    }

    /** README — "Async". */
    static void async(Oblodai oblodai, PaymentRequest request) {
        OblodaiAsync client = oblodai.async();
        CompletableFuture<Payment> invoice = client.payments().create(request);
        CompletableFuture<List<Payout>> all = client.payouts().history().all(500);
        System.out.println(invoice.join().uuid() + " " + all.join().size());
    }

    /** README — "Money". */
    static void money() {
        System.out.println(Money.add("10.000000", "0.5"));
        System.out.println(Money.compare("25", "25.000000"));
        System.out.println(Money.isZero("0.000000"));
        System.out.println(Money.toBigDecimal("1.5"));
    }

    /** README — "Self-hosted or local gateway". */
    static void selfHosted() {
        Oblodai.builder().baseUrl("http://127.0.0.1:8095").build();
        Oblodai.builder().baseUrl("http://gw.corp").allowInsecureBaseUrl(true).build();
    }

    /** MIGRATION-1.3 — merchant provisioning. */
    static void provisioning() {
        Oblodai gateway =
                Oblodai.builder()
                        .baseUrl("https://gw.corp")
                        .adminToken(System.getenv("OBLODAI_ADMIN_TOKEN"))
                        .build();

        MerchantOnboarded merchant =
                gateway.merchants()
                        .create(new MerchantsRequest().email("shop@example.com").name("Example Shop"));
        System.out.println(merchant.merchantId());
    }

    /** MIGRATION-1.3 — a receiver that answers the right status to each failure. */
    static int receive(byte[] rawBody, WebhookHeaders headers, WebhookVerifier.Options options) {
        WebhookEvent event;
        try {
            event = WebhookVerifier.verify(rawBody, headers, options);
        } catch (SignatureException bad) {
            return 401;
        } catch (WebhookPayloadException unreadable) {
            System.err.println("authentic delivery I cannot read: " + unreadable.getMessage());
            return 400;
        }
        if (!WebhookVerifier.isKnownEvent(event)) return 200;
        return 200;
    }

    /** MIGRATION-1.3 — a shown-once secret is readable and unloggable. */
    static void secrets(Oblodai oblodai) {
        WebhookEndpoint endpoint = oblodai.webhooks().register("https://shop.example/hook");
        store(endpoint.secret());
        System.out.println("registered " + endpoint);
    }

    private static void store(String secret) {
        System.out.println("stored " + (secret == null ? 0 : secret.length()) + " characters");
    }
}
