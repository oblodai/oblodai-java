package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.RequestInfo;
import com.oblodai.core.ResponseInfo;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.UnavailableException;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.generated.models.BalanceResult;
import com.oblodai.generated.models.PaymentRequest;
import com.oblodai.generated.models.PaymentView;
import com.oblodai.support.Clients;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Spec §3.5: the raw response, a client copy with other options, request and response hooks. */
class RawOptionsHooksTest {

    private static final String BALANCE = "{\"balance\":{\"merchant\":[]}}";
    private static final String DOWN = "{\"code\":\"db.unavailable\",\"retryable\":true,\"retry_after\":0}";

    @Test
    void withRawResponseGivesTheValueStatusHeadersAndRequestId() {
        MockHttpClient http =
                new MockHttpClient()
                        .raw(
                                201,
                                "{\"state\":0,\"result\":" + Fixtures.payment("u") + "}",
                                "content-type",
                                "application/json",
                                "x-ratelimit-remaining",
                                "99");
        Oblodai oblodai = Clients.client(http);

        ApiResponse<PaymentView> raw =
                oblodai.withRawResponse(
                        c -> c.payments().create(PaymentRequest.builder().amount("25").currency("USDT").build()));

        assertEquals("u", raw.value().uuid());
        assertEquals(201, raw.status());
        assertEquals("99", raw.header("X-RateLimit-Remaining").orElseThrow());
        assertEquals(http.onlyCall().header("x-request-id"), raw.requestId(), "the id the SDK sent");
    }

    @Test
    void theGatewaysOwnRequestIdWins() {
        MockHttpClient http =
                new MockHttpClient()
                        .raw(200, "{\"state\":0,\"result\":" + BALANCE + "}", "x-request-id", "srv-1");
        ApiResponse<BalanceResult> raw = Clients.client(http).withRawResponse(c -> c.account().getBalance());
        assertEquals("srv-1", raw.requestId());
    }

    @Test
    void anErrorStillThrowsAndTheAsyncFormWorksToo() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(404, "{\"code\":\"payment.not_found\",\"retryable\":false}")
                        .ok(BALANCE);
        Oblodai oblodai = Clients.client(http);
        assertThrows(
                com.oblodai.errors.NotFoundException.class,
                () -> oblodai.withRawResponse(c -> c.account().getBalance()));

        ApiResponse<BalanceResult> raw =
                oblodai.async().withRawResponse(c -> c.account().getBalance()).join();
        assertEquals(200, raw.status());
        assertThrows(IllegalStateException.class, () -> oblodai.withRawResponse(c -> "no call"));
    }

    @Test
    void withOptionsCopiesTheClientWithOtherDefaults() {
        MockHttpClient http =
                new MockHttpClient().apiError(503, DOWN).apiError(503, DOWN).ok(BALANCE).ok(BALANCE);
        Oblodai base = Clients.client(http);
        Oblodai strict =
                base.withOptions(
                        RequestOptions.of().maxRetries(0).timeout(Duration.ofSeconds(3)).extraHeader("X-Tenant", "acme"));

        assertThrows(UnavailableException.class, () -> strict.account().getBalance());
        assertEquals(1, http.calls().size(), "no retries on the copy");
        assertEquals("acme", http.calls().get(0).header("x-tenant"));

        base.account().getBalance();
        assertEquals(3, http.calls().size(), "the original still retries");
        assertNull(http.calls().get(2).header("x-tenant"), "and has no extra header");
        assertSame(base.transport().clock(), strict.transport().clock(), "the clock is shared");

        assertThrows(ConfigException.class, () -> base.withOptions(RequestOptions.of().idempotencyKey("k")));
        assertThrows(ConfigException.class, () -> base.withOptions(RequestOptions.of().requestId("r")));
    }

    @Test
    void hooksSeeEveryAttemptWithTheSecretsRedacted() {
        List<RequestInfo> requests = new ArrayList<>();
        List<ResponseInfo> responses = new ArrayList<>();
        MockHttpClient http = new MockHttpClient().apiError(503, DOWN).ok(BALANCE);
        Oblodai oblodai =
                Clients.builder(http, new Clients.RecordingSleeper())
                        .adminToken("adm")
                        .onRequest(requests::add)
                        .onResponse(responses::add)
                        .build();

        oblodai.account().getBalance();

        assertEquals(2, requests.size());
        assertEquals(1, requests.get(0).attempt());
        assertEquals(2, requests.get(1).attempt());
        assertEquals("getBalance", requests.get(0).operationId());
        assertEquals(requests.get(0).requestId(), requests.get(1).requestId());
        assertEquals("POST", requests.get(0).method());
        assertEquals("https://api.test/v1/balance", requests.get(0).url());
        String signature =
                requests.get(0).headers().entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(SigningProtocol.REQUEST_HEADER_SIGNATURE.toLowerCase(java.util.Locale.ROOT)))
                        .findFirst()
                        .orElseThrow()
                        .getValue();
        assertEquals("[redacted]", signature);

        assertEquals(2, responses.size());
        assertEquals(503, responses.get(0).status());
        assertInstanceOf(UnavailableException.class, responses.get(0).error());
        assertEquals(200, responses.get(1).status());
        assertNull(responses.get(1).error());
        assertTrue(!responses.get(1).elapsed().isNegative());
    }
}
