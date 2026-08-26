package com.oblodai.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.Oblodai;
import com.oblodai.RequestOptions;
import com.oblodai.Statuses;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;
import com.oblodai.contract.requests.PaymentInfoRequest;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.contract.requests.PayoutCalculateRequest;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.contract.requests.PayoutValidateRequest;
import com.oblodai.contract.requests.SandboxDepositRequest;
import com.oblodai.contract.requests.SandboxFaucetRequest;
import com.oblodai.errors.IdempotencyConflictException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.models.Payment;
import com.oblodai.models.Payout;
import com.oblodai.models.PayoutCalculation;
import com.oblodai.models.PayoutValidation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * The SDK against a REAL gateway at {@code OBLODAI_LIVE_URL}. It onboards a merchant, takes a
 * sandbox key and walks the money path, so signing, envelopes, idempotency and the vocabulary are
 * all exercised for real rather than against a fake.
 *
 * <p>Skipped when {@code OBLODAI_LIVE_URL} is unset.
 */
@EnabledIfEnvironmentVariable(named = "OBLODAI_LIVE_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveSandboxTest {

    private static final String ADDRESS = "TQrY8bkbpXKPt2LZbU8jqfnpFbUSF15sbx";

    private Oblodai oblodai;
    private Payment invoice;

    @BeforeAll
    void onboardASandboxMerchant() {
        oblodai = Live.sandboxClient("sdk-live");
    }

    @Test
    @Order(1)
    void readsPublicCatalogDataWithoutCredentials() {
        assertFalse(Live.publicClient().catalog().currencies().currencies().isEmpty());
    }

    @Test
    @Order(2)
    void createsAnInvoiceAndReadsItBackByOrderIdAndUuid() {
        invoice =
                oblodai
                        .payments()
                        .create(
                                new PaymentRequest()
                                        .amount("25")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId("sdk-live-" + System.currentTimeMillis()));
        assertEquals(PaymentStatus.CREATED, invoice.status());

        assertEquals(
                invoice.uuid(),
                oblodai.payments().info(new PaymentInfoRequest().orderId(invoice.orderId())).uuid());
        assertTrue(
                oblodai.payments().history().all(50).stream()
                        .anyMatch(payment -> payment.uuid().equals(invoice.uuid())));

        // A signed GET with a query: the signature covers path plus query, so this fails loudly if
        // the canonical string is wrong.
        assertNotNull(oblodai.sandbox().webhooks(5, 0).firstPage().items());
    }

    @Test
    @Order(3)
    void replaysAnIdempotentCreateAndRefusesAReusedKeyWithADifferentBody() {
        String key = "sdk-idem-" + System.currentTimeMillis();
        RequestOptions options = RequestOptions.of().idempotencyKey(key);

        Payment first =
                oblodai
                        .payments()
                        .create(
                                new PaymentRequest()
                                        .amount("5")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId(key + "-o"),
                                options);
        Payment replay =
                oblodai
                        .payments()
                        .create(
                                new PaymentRequest()
                                        .amount("5")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId(key + "-o"),
                                options);
        assertEquals(first.uuid(), replay.uuid(), "the same key replays the first answer");

        IdempotencyConflictException conflict =
                assertThrows(
                        IdempotencyConflictException.class,
                        () ->
                                oblodai
                                        .payments()
                                        .create(
                                                new PaymentRequest()
                                                        .amount("2")
                                                        .currency("USDT")
                                                        .network(Network.TRON)
                                                        .orderId(key + "-o2"),
                                                options));
        assertEquals("idempotency.key_reused", conflict.code());
        assertEquals(409, conflict.httpStatus());
    }

    @Test
    @Order(4)
    void simulatesADepositSeesTheInvoicePaidThenFundsAndSendsAPayout() {
        oblodai
                .sandbox()
                .deposit(
                        new SandboxDepositRequest()
                                .invoiceId(invoice.uuid())
                                .amount("25")
                                .confirmations(20)
                                .txid("sdk-tx-" + System.currentTimeMillis()));
        assertTrue(Statuses.isPaymentPaid(oblodai.payments().info(invoice.uuid()).status()));

        oblodai.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("100"));
        assertTrue(
                oblodai.account().balance().balance().merchant().stream()
                        .anyMatch(entry -> entry.currency().equals("USDT")));

        PayoutCalculation calculation =
                oblodai
                        .payouts()
                        .calculate(
                                new PayoutCalculateRequest()
                                        .amount("10")
                                        .currency("USDT")
                                        .network(Network.TRON));
        assertNotNull(calculation.feeBearer());

        PayoutValidation validation =
                oblodai
                        .payouts()
                        .validate(
                                new PayoutValidateRequest()
                                        .amount("10")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .address(ADDRESS));
        assertTrue(validation.valid());

        Payout payout =
                oblodai
                        .payouts()
                        .create(
                                new PayoutRequest()
                                        .amount("10")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .address(ADDRESS)
                                        .orderId("sdk-po-" + System.currentTimeMillis()));
        assertNotNull(payout.uuid());
        assertEquals(payout.orderId(), oblodai.payouts().info(payout.uuid()).orderId());
    }

    @Test
    @Order(5)
    void classifiesADomainRefusalWithTheGatewaysOwnRetryableFlag() {
        OblodaiException error =
                assertThrows(
                        OblodaiException.class,
                        () ->
                                oblodai
                                        .payouts()
                                        .create(
                                                new PayoutRequest()
                                                        .amount("999999")
                                                        .currency("USDT")
                                                        .network(Network.TRON)
                                                        .address(ADDRESS)
                                                        .orderId("sdk-big-" + System.currentTimeMillis())));
        assertEquals("payout.insufficient_funds", error.code());
        assertEquals(409, error.httpStatus());
        assertNotNull(error.requestId(), "a request id to quote to support");
    }

    @Test
    @Order(6)
    void verifiesARealDeliveryTheGatewaySignedForARegisteredEndpoint() throws Exception {
        // A receiver on loopback: the gateway signs the delivery with the endpoint secret it just
        // handed us, and the standalone verifier must accept those exact bytes.
        com.sun.net.httpserver.HttpServer receiver =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.BlockingQueue<Object[]> delivered =
                new java.util.concurrent.LinkedBlockingQueue<>();
        receiver.createContext(
                "/hook",
                exchange -> {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    delivered.add(new Object[] {body, exchange.getRequestHeaders()});
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                });
        receiver.start();
        try {
            String url =
                    "http://127.0.0.1:" + receiver.getAddress().getPort() + "/hook";
            String secret = oblodai.webhooks().register(url).secret();
            assertNotNull(secret, "the endpoint secret is shown once, at registration");

            assertTrue(
                    oblodai
                            .webhooks()
                            .testPayment(
                                    new com.oblodai.contract.requests.TestWebhookPaymentRequest()
                                            .urlCallback(url)
                                            .currency("USDT")
                                            .network(Network.TRON)
                                            .status("paid"))
                            .ok());

            Object[] delivery = delivered.poll(15, java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(delivery, "the gateway delivered nothing to the receiver");
            @SuppressWarnings("unchecked")
            java.util.Map<String, java.util.List<String>> headers =
                    (java.util.Map<String, java.util.List<String>>) delivery[1];
            com.oblodai.models.WebhookEvent event =
                    com.oblodai.webhooks.WebhookVerifier.verify(
                            (byte[]) delivery[0],
                            com.oblodai.webhooks.WebhookHeaders.ofMulti(headers),
                            com.oblodai.webhooks.WebhookVerifier.options(secret));
            assertEquals("payment", event.type());
            assertNotNull(event.uuid());
            assertFalse(
                    com.oblodai.webhooks.WebhookVerifier.isStale(event, null),
                    "a first delivery is never stale");
        } finally {
            receiver.stop(0);
        }
    }

    @Test
    @Order(7)
    void theAsyncClientWalksTheSamePathOverTheSameConnections() {
        assertNotNull(oblodai.async().account().balance().join().balance());
        assertEquals(
                invoice.uuid(), oblodai.async().payments().info(invoice.uuid()).join().uuid());
    }
}
