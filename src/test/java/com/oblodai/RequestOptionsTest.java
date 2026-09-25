package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.errors.ConfigException;
import com.oblodai.errors.TransportException;
import com.oblodai.errors.UnavailableException;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.support.Clients;
import com.oblodai.support.MockHttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec §3.2 and §3.9: explicit per-call options, and a caller-visible X-Request-ID. */
class RequestOptionsTest {

    private static final String BALANCE = "{\"balance\":{\"merchant\":[]}}";
    private static final String DOWN = "{\"code\":\"db.unavailable\",\"retryable\":true,\"retry_after\":0}";

    @Test
    void theFiveOptionsAreExplicitAndImmutable() {
        RequestOptions base = RequestOptions.of();
        RequestOptions full =
                base.idempotencyKey("k-1")
                        .timeout(Duration.ofSeconds(5))
                        .maxRetries(0)
                        .extraHeader("X-Trace", "t-1")
                        .requestId("req-1");
        assertEquals("k-1", full.idempotencyKey());
        assertEquals(Duration.ofSeconds(5), full.timeout());
        assertEquals(0, full.maxRetries());
        assertEquals(Map.of("X-Trace", "t-1"), full.extraHeaders());
        assertEquals("req-1", full.requestId());
        assertEquals(RequestOptions.of(), base, "the original is untouched");

        assertEquals(ConfigException.BAD_CONFIG, assertThrows(ConfigException.class, () -> base.timeout(Duration.ZERO)).code());
        assertEquals(ConfigException.BAD_CONFIG, assertThrows(ConfigException.class, () -> base.maxRetries(-1)).code());
        assertEquals(ConfigException.BAD_HEADER, assertThrows(ConfigException.class, () -> base.extraHeader(SigningProtocol.HEADER_SIGNATURE, "x")).code());
        assertEquals(ConfigException.BAD_HEADER, assertThrows(ConfigException.class, () -> base.requestId("a\nb")).code());
        assertEquals(
                ConfigException.BAD_IDEMPOTENCY_KEY,
                assertThrows(ConfigException.class, () -> base.idempotencyKey("has space")).code());
    }

    @Test
    void timeoutIsPerAttemptAndADuration() {
        MockHttpClient http = new MockHttpClient().slow(500);
        TransportException error =
                assertThrows(
                        TransportException.class,
                        () ->
                                Clients.client(http)
                                        .account()
                                        .getBalance(
                                                RequestOptions.of().timeout(Duration.ofMillis(20)).maxRetries(0)));
        assertEquals(TransportException.TIMEOUT, error.code());
    }

    @Test
    void maxRetriesOverridesTheClientPolicyForOneCall() {
        MockHttpClient none = new MockHttpClient().apiError(503, DOWN).ok(BALANCE);
        assertThrows(
                UnavailableException.class,
                () -> Clients.client(none).account().getBalance(RequestOptions.of().maxRetries(0)));
        assertEquals(1, none.calls().size());

        MockHttpClient more =
                new MockHttpClient().apiError(503, DOWN).apiError(503, DOWN).apiError(503, DOWN).ok(BALANCE);
        Clients.client(more).account().getBalance(RequestOptions.of().maxRetries(3));
        assertEquals(4, more.calls().size(), "one attempt plus three retries, above the client's two");
    }

    @Test
    void extraHeadersRideOnThisCallOnly() {
        MockHttpClient http = new MockHttpClient().ok(BALANCE).ok(BALANCE);
        Oblodai oblodai = Clients.client(http);
        oblodai.account().getBalance(RequestOptions.of().extraHeaders(Map.of("X-Trace", "t-1")));
        oblodai.account().getBalance();
        assertEquals("t-1", http.calls().get(0).header("x-trace"));
        assertEquals(null, http.calls().get(1).header("x-trace"));
    }

    @Test
    void everyCallCarriesItsOwnRequestIdOnEveryAttempt() {
        MockHttpClient http = new MockHttpClient().apiError(503, DOWN).ok(BALANCE).ok(BALANCE);
        Oblodai oblodai = Clients.client(http);
        oblodai.account().getBalance();
        oblodai.account().getBalance();

        String first = http.calls().get(0).header("x-request-id");
        assertTrue(first.matches("^[0-9a-f-]{36}$"), first);
        assertEquals(first, http.calls().get(1).header("x-request-id"), "one id for the call's attempts");
        assertNotEquals(first, http.calls().get(2).header("x-request-id"), "a fresh id per call");
    }

    @Test
    void theCallerNamesTheRequestIdByOptionOrByHeader() {
        MockHttpClient http = new MockHttpClient().ok(BALANCE).ok(BALANCE);
        Oblodai oblodai = Clients.client(http);
        oblodai.account().getBalance(RequestOptions.of().requestId("order-1001"));
        oblodai.account().getBalance(RequestOptions.of().extraHeader("X-Request-ID", "from-header"));
        assertEquals("order-1001", http.calls().get(0).header("x-request-id"));
        assertEquals("from-header", http.calls().get(1).header("x-request-id"));
    }
    @Test
    void theFaucetTakesTheKeyInItsBodyAndRefusesItGivenTwice() {
        // Ruling 10: the faucet deduplicates by its own body field; the option fills it.
        String faucet =
                "{\"asset\":\"USDT\",\"amount\":\"5\",\"journal_id\":\"00000000-0000-4000-8000-000000000001\"}";
        MockHttpClient http = new MockHttpClient().ok(faucet).ok(faucet);
        Oblodai oblodai = Clients.client(http);
        oblodai.sandbox()
                .faucet(
                        com.oblodai.generated.models.FaucetRequest.builder().amount("5").asset("USDT").build(),
                        RequestOptions.of().idempotencyKey("tap-1"));
        assertTrue(http.onlyCall().body().contains("\"idempotency_key\":\"tap-1\""), http.onlyCall().body());
        assertEquals(null, http.onlyCall().header(SigningProtocol.HEADER_IDEMPOTENCY_KEY));

        // The field and the option both naming the key: ambiguous, refused before the network —
        // the same rule in every Oblodai SDK.
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                oblodai.sandbox()
                                        .faucet(
                                                com.oblodai.generated.models.FaucetRequest.builder()
                                                        .amount("5")
                                                        .asset("USDT")
                                                        .idempotencyKey("own")
                                                        .build(),
                                                RequestOptions.of().idempotencyKey("tap-2")));
        assertEquals(ConfigException.BAD_CONFIG, error.code());
        assertTrue(error.getMessage().contains("idempotency_key"), error.getMessage());
        assertEquals(1, http.calls().size());
    }
}
