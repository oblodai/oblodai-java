package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.core.Logger;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import com.oblodai.models.Balance;
import com.oblodai.support.MockHttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * What the transport must do when things go wrong around the call rather than inside it:
 * cancellation, a shared clock two calls are correcting at once, headers a caller tried to override,
 * an HTTP client that followed a redirect, and a result whose shape is not what the route returns.
 */
class TransportSafetyTest {

    private static final String BALANCE = "{\"balance\":{\"merchant\":[]}}";

    private static Oblodai.Builder client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("pk_test_1")
                .secret("secret-1")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .retry(new RetryOptions(2, 1, 2, 30_000));
    }

    @Test
    void cancellingTheFutureAbortsTheExchangeInFlight() throws Exception {
        MockHttpClient http = new MockHttpClient().hangs();
        CompletableFuture<Balance> call = client(http).buildAsync().account().balance();

        // Give the call a moment to reach the client, then cancel it as a caller would.
        for (int i = 0; i < 100 && http.hanging().isEmpty(); i++) Thread.sleep(5);
        assertEquals(1, http.hanging().size(), "the exchange reached the HTTP client");
        assertTrue(call.cancel(true));

        assertTrue(
                http.hanging().get(0).isCancelled(),
                "cancelling the future the caller holds must reach the socket, not stop at a stage");
        assertThrows(java.util.concurrent.CancellationException.class, call::join);
    }

    @Test
    void aCancelledCallSurfacesAsTransportAbortedOnTheBlockingApi() throws Exception {
        MockHttpClient http = new MockHttpClient().hangs();
        OblodaiAsync async = client(http).buildAsync();
        CompletableFuture<Balance> call = async.account().balance();
        for (int i = 0; i < 100 && http.hanging().isEmpty(); i++) Thread.sleep(5);

        call.cancel(true);
        TransportException aborted =
                assertThrows(TransportException.class, () -> com.oblodai.core.Transport.await(call));
        assertEquals(TransportException.ABORTED, aborted.code());
    }

    @Test
    void concurrentCallsOnASkewedHostAllSucceedAfterOneCorrection() throws Exception {
        long serverNow = System.currentTimeMillis() / 1000L + 3600;
        MockHttpClient http = new MockHttpClient().withServerClock(serverNow);
        for (int i = 0; i < 64; i++) http.ok(BALANCE);
        Oblodai oblodai = client(http).retry(RetryOptions.none()).build();

        int callers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < callers; i++) {
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            oblodai.account().balance();
                            succeeded.incrementAndGet();
                        } catch (Throwable failure) {
                            failures.add(failure);
                        }
                        return null;
                    });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(callers, succeeded.get(), "failures: " + failures);
        long offset = oblodai.transport().clock().offset();
        assertTrue(Math.abs(offset - 3600) <= 5, "the learned offset is the server's, got " + offset);
    }

    @Test
    void aRevertOnlyUndoesTheCorrectionThisCallInstalled() {
        SkewCorrectingClock clock = new SkewCorrectingClock(() -> 1_000_000L);
        clock.correct(120);
        assertFalse(clock.revert(60, 0), "another call's offset is not ours to roll back");
        assertEquals(120, clock.offset());
        assertTrue(clock.revert(120, 0));
        assertEquals(0, clock.offset());
    }

    @Test
    void aCallerHeaderNeverOverridesAnSdkOwnedOneAndIsRefusedOutright() {
        assertEquals(
                ConfigException.BAD_HEADER,
                assertThrows(
                                ConfigException.class,
                                () -> client(new MockHttpClient()).header("accept", "text/csv"))
                        .code());
        assertThrows(
                ConfigException.class,
                () -> client(new MockHttpClient()).header("User-Agent", "curl/8"));
        ConfigException adminToken =
                assertThrows(
                        ConfigException.class,
                        () -> client(new MockHttpClient()).header("x-admin-token", "sneaked"));
        assertTrue(adminToken.getMessage().contains("adminToken"), adminToken.getMessage());
    }

    @Test
    void aCallerHeaderThatCouldNotBeSentVerbatimIsAConfigFailure() {
        assertThrows(
                ConfigException.class,
                () -> client(new MockHttpClient()).header("X-Note", "line\r\nX-Injected: 1"));
        assertThrows(
                ConfigException.class, () -> client(new MockHttpClient()).header("X-Note", "naïve"));
        assertThrows(ConfigException.class, () -> client(new MockHttpClient()).header("X-Note", null));
        assertThrows(ConfigException.class, () -> client(new MockHttpClient()).header("X Note", "v"));
    }

    @Test
    void aPerCallHeaderIsSentOnThatCallOnlyAndObeysTheSameRules() {
        MockHttpClient http = new MockHttpClient().ok(BALANCE).ok(BALANCE);
        Oblodai oblodai = client(http).header("X-Tenant", "acme").build();

        oblodai.account().balance(RequestOptions.of().header("X-Trace", "t-1"));
        oblodai.account().balance();

        assertEquals("t-1", http.calls().get(0).header("x-trace"));
        assertEquals("acme", http.calls().get(0).header("x-tenant"), "client headers still ride");
        assertNull(http.calls().get(1).header("x-trace"), "and only on the call that asked for it");

        assertEquals(
                ConfigException.BAD_HEADER,
                assertThrows(
                                ConfigException.class,
                                () -> RequestOptions.of().header("X-Signature", "zz"))
                        .code());
        assertThrows(
                ConfigException.class, () -> RequestOptions.of().header("X-Note", "two\r\nlines"));
    }

    @Test
    void theAdminTokenRidesOnlyOnOnboardingRoutes() {
        MockHttpClient http = new MockHttpClient().ok(BALANCE).ok("{\"merchant_id\":\"m1\"}");
        Oblodai oblodai = client(http).adminToken("adm").build();

        oblodai.account().balance();
        oblodai.merchants().createSandbox("m1");

        assertEquals(null, http.calls().get(0).header("x-admin-token"), "not on a merchant route");
        assertEquals("adm", http.calls().get(1).header("x-admin-token"));
    }

    @Test
    void anInjectedClientThatFollowedARedirectIsCaught() {
        MockHttpClient http = new MockHttpClient().okFrom("https://evil.test/v1/balance", BALANCE);
        OblodaiException error =
                assertThrows(
                        OblodaiException.class,
                        () -> client(http).retry(RetryOptions.none()).build().account().balance());
        assertTrue(error.getMessage().contains("redirect"), error.getMessage());
        assertTrue(error.getMessage().contains("evil.test"), error.getMessage());
    }

    @Test
    void aResultOfTheWrongShapeIsAContractFailureNotABinderCrash() {
        MockHttpClient http = new MockHttpClient().ok("{\"uuid\":{\"nested\":true}}");
        ContractException error =
                assertThrows(
                        ContractException.class,
                        () ->
                                client(http)
                                        .build()
                                        .payments()
                                        .create(new PaymentRequest().amount("1").currency("USDT")));
        assertEquals(ContractException.BAD_ENVELOPE, error.code());
        assertFalse(error.getMessage().contains("nested"), "the body is not quoted into the message");
    }

    @Test
    void anAmountFieldThatArrivesAsANumberIsRefusedRatherThanRounded() {
        MockHttpClient http = new MockHttpClient().ok("{\"uuid\":\"u\",\"amount\":0.30000000000000004}");
        assertThrows(
                ContractException.class,
                () ->
                        client(http)
                                .build()
                                .payments()
                                .create(new PaymentRequest().amount("1").currency("USDT")));
    }

    @Test
    void anInjectedLoggerNeverSeesASensitiveValue() {
        List<Map<String, Object>> lines = new ArrayList<>();
        Logger recorder =
                new Logger() {
                    @Override
                    public void debug(String message, Map<String, Object> fields) {
                        lines.add(fields);
                    }

                    @Override
                    public void info(String message, Map<String, Object> fields) {
                        lines.add(fields);
                    }

                    @Override
                    public void warn(String message, Map<String, Object> fields) {
                        lines.add(fields);
                    }

                    @Override
                    public void error(String message, Map<String, Object> fields) {
                        lines.add(fields);
                    }
                };

        MockHttpClient http = new MockHttpClient().ok(BALANCE);
        client(http).logger(recorder).build().account().balance();

        assertFalse(lines.isEmpty(), "the transport logs its attempts");
        for (Map<String, Object> fields : lines) {
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                if (Logger.SENSITIVE.matcher(field.getKey()).find()) {
                    assertEquals("[redacted]", field.getValue(), field.getKey());
                }
                assertFalse(
                        String.valueOf(field.getValue()).contains("secret-1"),
                        "a signing secret must not reach an injected logger");
            }
        }
        assertNotNull(http.onlyCall().header("x-signature"));
    }
}
