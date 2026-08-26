package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.PaymentAccuracySetRequest;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.errors.AuthenticationException;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.IdempotencyConflictException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.RateLimitException;
import com.oblodai.errors.TransportException;
import com.oblodai.errors.ValidationException;
import com.oblodai.support.MockHttpClient;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Signing, headers, idempotency, retries and clock-skew correction, against a scripted transport. */
class TransportTest {

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
    void signsPathAndQueryOnGetAndSendsNoBody() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok("{\"items\":[],\"paginate\":{\"total\":0,\"per_page\":10,\"offset\":0,\"has_pages\":false}}");
        client(http).build().sandbox().webhooks(10, 0).firstPage();

        MockHttpClient.Recorded call = http.onlyCall();
        assertEquals("https://api.test/v1/sandbox/webhooks?limit=10&offset=0", call.uri().toString());
        assertEquals("GET", call.method());
        assertEquals("", call.body() == null ? "" : call.body());
        assertEquals("pk_test_1", call.header("x-public-id"));
        assertTrue(call.header("x-signature").matches("^[0-9a-f]{64}$"));
        assertNotNull(call.header("x-timestamp"));
    }

    @Test
    void generatesOneIdempotencyKeyPerCreateCallAndReusesItAcrossRetries() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(503, "{\"code\":\"db.unavailable\",\"message\":\"down\",\"retryable\":true}")
                        .ok("{\"uuid\":\"u\"}");
        client(http).build().payments().create(new PaymentRequest().amount("1").currency("USDT"));

        assertEquals(2, http.calls().size());
        String key = http.calls().get(0).header("idempotency-key");
        assertTrue(key.matches("^[0-9a-f-]{36}$"), "a generated key is a uuid");
        assertEquals(key, http.calls().get(1).header("idempotency-key"), "reused on the retry");
        assertTrue(http.calls().get(1).header("x-signature").matches("^[0-9a-f]{64}$"), "re-signed");
    }

    @Test
    void honoursACallerKeyAndAddsNoneToReadRoutes() {
        MockHttpClient http = new MockHttpClient().ok("{\"uuid\":\"u\"}").ok("{\"uuid\":\"u\"}");
        Oblodai oblodai = client(http).build();
        oblodai
                .payouts()
                .create(
                        new PayoutRequest().amount("1").currency("USDT").address("T").orderId("o"),
                        RequestOptions.of().idempotencyKey("my-key-1"));
        oblodai.payments().info("u");

        assertEquals("my-key-1", http.calls().get(0).header("idempotency-key"));
        assertNull(http.calls().get(1).header("idempotency-key"));
    }

    @Test
    void doesNotRetryANonRetryableErrorEvenOnA5xx() {
        MockHttpClient http =
                new MockHttpClient().apiError(500, "{\"code\":\"internal\",\"retryable\":false}");
        OblodaiException error =
                assertThrows(OblodaiException.class, () -> client(http).build().account().balance());

        assertEquals("internal", error.code());
        assertEquals(500, error.httpStatus());
        assertTrue(!error.retryable());
        assertEquals(1, http.calls().size());
    }

    @Test
    void retriesARetryableErrorUntilTheBudgetIsSpent() {
        String rateLimited = "{\"code\":\"request.rate_limited\",\"retryable\":true,\"retry_after\":0}";
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(429, rateLimited, "retry-after", "0")
                        .apiError(429, rateLimited)
                        .apiError(429, rateLimited);

        RateLimitException error =
                assertThrows(RateLimitException.class, () -> client(http).build().account().balance());
        assertEquals(0, error.retryAfter());
        assertEquals(3, http.calls().size(), "one attempt plus two retries");
    }

    @Test
    void retriesATransportFailureOnlyWhenTheRequestIsSafeToRepeat() {
        MockHttpClient read = new MockHttpClient().fails(new IOException("connection reset")).ok(BALANCE);
        client(read).build().account().balance();
        assertEquals(2, read.calls().size(), "a read route is retried");

        MockHttpClient write = new MockHttpClient().fails(new IOException("connection reset")).ok("{}");
        TransportException error =
                assertThrows(
                        TransportException.class,
                        () ->
                                client(write)
                                        .build()
                                        .settings()
                                        .setAccuracy(new PaymentAccuracySetRequest().enabled(true)));
        assertEquals(TransportException.NETWORK, error.code());
        assertEquals(1, write.calls().size(), "an unkeyed write is never re-sent");

        MockHttpClient keyed =
                new MockHttpClient().fails(new IOException("connection reset")).ok("{\"uuid\":\"u\"}");
        client(keyed).build().payments().create(new PaymentRequest().amount("1").currency("USDT"));
        assertEquals(2, keyed.calls().size(), "a keyed write is deduplicated, so it may repeat");
    }

    @Test
    void classifiesTheErrorEnvelopeAndKeepsRequestIdAndField() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(
                                400,
                                "{\"code\":\"payment.below_minimum\",\"message\":\"too small\",\"field\":\"amount\","
                                        + "\"retryable\":false,\"request_id\":\"rq-1\"}")
                        .apiError(401, "{\"code\":\"merchant.bad_signature\",\"message\":\"bad\",\"retryable\":false}")
                        .apiError(409, "{\"code\":\"idempotency.key_reused\",\"message\":\"reused\",\"retryable\":false}");
        Oblodai oblodai = client(http).retry(RetryOptions.none()).build();

        ValidationException validation =
                assertThrows(
                        ValidationException.class,
                        () ->
                                oblodai
                                        .payments()
                                        .create(new PaymentRequest().amount("0").currency("USDT")));
        assertEquals("payment.below_minimum", validation.code());
        assertEquals("amount", validation.field());
        assertEquals("rq-1", validation.requestId());
        assertEquals("payment", validation.family());

        assertThrows(AuthenticationException.class, () -> oblodai.account().balance());
        assertThrows(
                IdempotencyConflictException.class,
                () -> oblodai.payments().create(new PaymentRequest().amount("1").currency("USDT")));
    }

    @Test
    void reSignsOnceWithTheServerClockWhenA401RevealsSkew() {
        long serverNow = System.currentTimeMillis() / 1000 + 3600;
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(
                                401,
                                "{\"code\":\"merchant.bad_signature\",\"retryable\":false}",
                                "date",
                                httpDate(serverNow))
                        .ok(BALANCE);
        client(http).retry(RetryOptions.none()).build().account().balance();

        assertEquals(2, http.calls().size());
        long signedAt = Long.parseLong(http.calls().get(1).header("x-timestamp"));
        assertTrue(Math.abs(signedAt - serverNow) < 5, "the retry signs with the gateway's clock");
    }

    @Test
    void timesOutAndReportsTransportTimeout() {
        MockHttpClient http = new MockHttpClient().slow(500);
        TransportException error =
                assertThrows(
                        TransportException.class,
                        () ->
                                client(http)
                                        .timeout(Duration.ofMillis(20))
                                        .retry(RetryOptions.none())
                                        .build()
                                        .account()
                                        .balance());
        assertEquals(TransportException.TIMEOUT, error.code());
    }

    @Test
    void signsMoneyOutAndMoneyInWithTheOneApiKey() {
        // A merchant has one key; a payout and an invoice go out under the same public id.
        MockHttpClient http = new MockHttpClient().ok("{\"uuid\":\"p\"}").ok("{\"uuid\":\"i\"}");
        Oblodai oblodai = client(http).build();
        oblodai
                .payouts()
                .create(new PayoutRequest().amount("1").currency("USDT").address("T").orderId("o"));
        oblodai.payments().create(new PaymentRequest().amount("1").currency("USDT"));

        assertEquals("pk_test_1", http.calls().get(0).header("x-public-id"));
        assertEquals("pk_test_1", http.calls().get(1).header("x-public-id"));
    }

    @Test
    void refusesACredentialledRouteWithNoKeysButServesPublicOnes() {
        MockHttpClient http = new MockHttpClient().ok("{\"currencies\":[],\"pricing_currencies\":[]}");
        Oblodai oblodai =
                Oblodai.builder()
                        .baseUrl("https://api.test")
                        .httpClient(http)
                        .environment(Map.of())
                        .build();

        assertNotNull(oblodai.catalog().currencies());
        ConfigException missing =
                assertThrows(ConfigException.class, () -> oblodai.account().balance());
        assertEquals(ConfigException.MISSING_CREDENTIALS, missing.code());
    }

    @Test
    void theAsyncClientSharesTheSameEngineAndFailsWithTheSameError() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(BALANCE)
                        .apiError(404, "{\"code\":\"payment.not_found\",\"retryable\":false}");
        OblodaiAsync async = client(http).buildAsync();

        assertNotNull(async.account().balance().join());
        Throwable failure =
                assertThrows(
                                java.util.concurrent.CompletionException.class,
                                () -> async.payments().info("missing").join())
                        .getCause();
        assertInstanceOf(OblodaiException.class, failure);
        assertEquals("payment.not_found", ((OblodaiException) failure).code());
    }

    @Test
    void theClockLearnsNothingFromAnImplausibleDate() {
        SkewCorrectingClock clock = new SkewCorrectingClock(() -> 1_700_000_000L);
        assertNull(clock.observeServerDate(null));
        assertNull(clock.observeServerDate("not a date"));
        assertNull(clock.observeServerDate(httpDate(1_700_000_000L + 48 * 3600)));
        assertEquals(60L, clock.observeServerDate(httpDate(1_700_000_060L)));
    }

    private static String httpDate(long epochSeconds) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSeconds), java.time.ZoneOffset.UTC));
    }
}
