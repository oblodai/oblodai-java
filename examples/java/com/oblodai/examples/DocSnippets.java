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
import com.oblodai.contract.requests.PayoutValidateRequest;
import com.oblodai.contract.requests.SandboxDepositRequest;
import com.oblodai.contract.requests.SandboxFaucetRequest;
import com.oblodai.contract.requests.TestWebhookPaymentRequest;
import com.oblodai.core.Logger;
import com.oblodai.core.Page;
import com.oblodai.core.RetryOptions;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.models.MerchantOnboarded;
import com.oblodai.models.Payment;
import com.oblodai.models.PaymentEvent;
import com.oblodai.models.Payout;
import com.oblodai.models.PayoutValidation;
import com.oblodai.models.WebhookDelivery;
import com.oblodai.models.WebhookEndpoint;
import com.oblodai.models.WebhookEvent;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Every code snippet README.md, README.ru.md and MIGRATION-1.3 show, compiled.
 *
 * <p>Documentation that no longer compiles is worse than no documentation: it is a promise the SDK
 * has stopped keeping, and nobody notices until a reader copies it. The examples are compiled with
 * the tests, so this file fails the build the day a snippet in the docs goes stale — and {@code
 * DocSnippetsTest} fails the build the day a snippet in the docs stops appearing here.
 *
 * <p>Each method below opens with one README block, statement for statement and in the README's own
 * order; anything after that block is only there to consume the values so the compiler is happy.
 * Nothing here is executed — {@code main} does nothing.
 */
public final class DocSnippets {

    private DocSnippets() {}

    /**
     * @param args ignored; this file exists to be compiled, not run
     */
    public static void main(String[] args) {
        System.out.println("the documentation snippets compile against this SDK");
    }

    /** README — "Quick start", accepting a payment. */
    static void quickStartPayment() {
        Oblodai oblodai = Oblodai.builder()
                .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
                .secret(System.getenv("OBLODAI_SECRET"))
                .build();

        Payment invoice = oblodai.payments().create(new PaymentRequest()
                .amount("25")                 // amounts are decimal strings, never floats
                .currency("USDT")             // what you price in: a fiat (USD, EUR, …) or a crypto asset
                .network(Network.TRON)        // omit it to let the payer choose the network on the pay page
                .orderId("order-1001")        // your reference; the invoice is idempotent per order_id
                .urlCallback("https://shop.example/oblodai/webhook"));

        System.out.println(invoice.url() + " " + invoice.address() + " " + invoice.status()); // created

        // Pricing in fiat: currency is what you charge, to_currency the asset the payer sends.
        new PaymentRequest().amount("25").currency("USD").toCurrency("USDT");
    }

    /** README — "Quick start", sending a payout. */
    static void quickStartPayout(Oblodai oblodai, String address) {
        PayoutValidation check = oblodai.payouts().validate(new PayoutValidateRequest()
                .amount("10").currency("USDT").network(Network.TRON).address(address));

        Payout payout = oblodai.payouts().create(new PayoutRequest()
                .amount("10").currency("USDT").network(Network.TRON).address(address)
                .orderId("payout-42"), RequestOptions.of().idempotencyKey("payout-42"));

        System.out.println(check.commission() + " fee, payout " + payout.uuid() + " " + payout.status());
    }

    /** README — "Amounts". */
    static void amounts() {
        System.out.println(Money.add("10.000000", "0.5"));      // "10.500000"
        System.out.println(Money.compare("25", "25.000000"));   // 0 — equal at any scale
        System.out.println(Money.isZero("0.000000"));           // true
        System.out.println(Money.toBigDecimal("1.5"));          // when you deliberately want one
    }

    /** README — "Async". */
    static void async(Oblodai oblodai, PaymentRequest request) {
        OblodaiAsync client = oblodai.async();                 // same engine, connections and clock
        CompletableFuture<Payment> invoice = client.payments().create(request);
        CompletableFuture<List<Payout>> all = client.payouts().history().all(500);

        System.out.println(invoice.join().uuid() + " " + all.join().size());
    }

    /** README — "Sandbox / testing". */
    static void sandbox(String invoiceId) {
        Oblodai sandbox = Oblodai.builder()
                .publicId(System.getenv("OBLODAI_PUBLIC_ID"))       // test_oblodai_…
                .secret(System.getenv("OBLODAI_SECRET"))            // oblodai_test_…
                .build();

        sandbox.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("100"));
        sandbox.sandbox().deposit(new SandboxDepositRequest().invoiceId(invoiceId).amount("25"));

        sandbox.webhooks().testPayment(new TestWebhookPaymentRequest().uuid(invoiceId).status("paid"));
        for (WebhookDelivery delivery : sandbox.sandbox().webhooks()) System.out.println(delivery.status());

        sandbox.sandbox().reset();                                  // cancels open invoices, zeroes balances

        System.out.println(sandbox.sandbox().replay("delivery-id"));
    }

    /** README — "Lists". */
    static void lists(Oblodai oblodai) {
        Page<Payment> page = oblodai.payments().history(new PaymentHistoryRequest().limit(50)).firstPage();

        for (Payout payout : oblodai.payouts().history(new PayoutHistoryRequest().status("confirmed"))) {
            process(payout);                                    // one page fetched at a time
        }

        List<Payout> refunds = oblodai.payouts().history(new PayoutHistoryRequest().kind("refund")).all(1000);
        Stream<Payment> stream = oblodai.payments().history().stream();

        System.out.println(page.items().size() + " " + refunds.size() + " " + stream.count());
    }

    private static void process(Payout payout) {
        System.out.println(payout.uuid());
    }

    /** README — "Webhooks", verifying a delivery. */
    static void webhooks(HttpExchange exchange, String secret, String previousSecret, Set<String> seen)
            throws IOException {
        byte[] rawBody = exchange.getRequestBody().readAllBytes();   // the RAW bytes, always

        WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
                rawBody,
                WebhookHeaders.ofMulti(exchange.getRequestHeaders()), // or of(Map), or a lambda
                WebhookVerifier.options(secret).previousSecret(previousSecret));

        if (delivery.isTest()) return;                               // a rehearsal: no money moved
        if (!seen.add(delivery.id())) return;                        // X-Webhook-Id, stable across retries
        if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
            markOrderPaid(payment.orderId());
        }

        System.out.println(WebhookVerifier.isStale(delivery.event(), 41L));
    }

    private static void markOrderPaid(String orderId) {
        System.out.println("paid: " + orderId);
    }

    /** README — "Webhooks", the status code a receiver answers. */
    static int receive(byte[] rawBody, WebhookHeaders headers, WebhookVerifier.Options options) {
        WebhookEvent event;
        try {
            event = WebhookVerifier.verify(rawBody, headers, options);
        } catch (SignatureException notFromTheGateway) {
            return 401;                       // the ONLY failure that deserves a 401
        } catch (WebhookPayloadException unreadable) {
            return 400;                       // authentic delivery, body is not an event: webhook.bad_payload
        }
        if (!WebhookVerifier.isKnownEvent(event)) return 200;   // newer than this SDK: acknowledge, ignore

        return 200;
    }

    /** README — "Errors". */
    static void errors(Oblodai oblodai, PayoutRequest request) {
        try {
            oblodai.payouts().create(request);
        } catch (OblodaiException e) {
            switch (e.code()) {
                case "payout.insufficient_funds", "payout.funds_maturing" ->
                        scheduleRetry(e.retryAfter() == null ? 60 : e.retryAfter());
                default -> throw e;   // the SDK already retried what was safe to retry
            }
        }
    }

    private static void scheduleRetry(int seconds) {
        System.out.println("retry in " + seconds + "s");
    }

    /** README — "Retries, idempotency and timeouts", per-call options. */
    static void requestOptions(Oblodai oblodai, PayoutRequest request, String orderId) {
        oblodai.payouts().create(request, RequestOptions.of()
                .idempotencyKey(orderId)                  // your own key; generated for you when omitted
                .timeout(Duration.ofSeconds(10))          // per attempt
                .deadline(Duration.ofSeconds(45))         // whole call, retries and pauses included
                .header("X-Tenant", "acme"));             // one call only, on top of the client-wide headers

        oblodai.payments().info("d1b0…");
        oblodai.payments().info(new PaymentInfoRequest().orderId("order-1001"));
    }

    /** README — "Configuration", the builder. */
    static void configuration(String id, String secret) {
        Oblodai oblodai = Oblodai.builder()
                .publicId(id).secret(secret)
                .baseUrl("https://api.oblodai.com")
                .timeout(Duration.ofSeconds(30))
                .deadline(Duration.ofSeconds(90))
                .retry(new RetryOptions(2, 250, 4_000, 30_000))
                .header("X-Tenant", "acme")
                .logger(Logger.console(Logger.Level.INFO))
                .build();

        oblodai.close();
    }

    /** README — "Configuration", a self-hosted or local gateway. */
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
