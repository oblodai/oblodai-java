package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.core.RetryOptions;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import com.oblodai.errors.ValidationException;
import com.oblodai.support.MockHttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The paths where a mistake costs money: a write re-sent after an ambiguous failure, an idempotency
 * key that does not deduplicate, a corrected clock that wedges a client, a raw body in a log.
 */
class MoneyPathsTest {

    private static final String HTML = "<html>upstream error</html>";

    private static Oblodai.Builder client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("pk")
                .secret("s")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .retry(new RetryOptions(2, 1, 2, 30_000));
    }

    @Test
    void rejectsACallerKeyOnARouteTheGatewayDoesNotDeduplicate() {
        MockHttpClient http = new MockHttpClient().ok("{}");
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                client(http)
                                        .build()
                                        .payouts()
                                        .approve(
                                                new com.oblodai.contract.requests.PayoutApproveRequest()
                                                        .uuid("p1"),
                                                RequestOptions.of().idempotencyKey("k1")));

        assertEquals(ConfigException.IDEMPOTENCY_UNSUPPORTED, error.code());
        assertTrue(http.calls().isEmpty(), "nothing was sent");
    }

    @Test
    void neverReSendsAnUnsafeWriteAfterAProxy503WithNoEnvelope() {
        MockHttpClient http =
                new MockHttpClient().raw(503, HTML, "content-type", "text/html").ok("{}");
        OblodaiException error =
                assertThrows(OblodaiException.class, () -> client(http).build().payouts().approve("p1"));

        assertEquals(503, error.httpStatus());
        assertTrue(error.synthetic(), "a proxy answered, not the gateway");
        assertTrue(error.retryable(), "the status is transient…");
        assertEquals(1, http.calls().size(), "…but repeating could duplicate the payout");
    }

    @Test
    void retriesAReadRouteAfterAProxyFailureAndHonoursRetryAfter() {
        MockHttpClient http =
                new MockHttpClient()
                        .raw(502, HTML, "content-type", "text/html")
                        .raw(504, HTML, "content-type", "text/html", "retry-after", "0")
                        .ok("{\"balance\":{\"merchant\":[]}}");
        client(http).build().account().balance();
        assertEquals(3, http.calls().size());

        MockHttpClient limited =
                new MockHttpClient().raw(429, HTML, "content-type", "text/html", "retry-after", "120");
        OblodaiException error =
                assertThrows(
                        OblodaiException.class,
                        () -> client(limited).retry(RetryOptions.none()).build().account().balance());
        assertEquals(120, error.retryAfter());
    }

    @Test
    void retriesAnEnvelopedRetryableErrorOnAnUnsafeWrite() {
        // The gateway answered, so it did not perform the operation: repeating cannot duplicate it.
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(409, "{\"code\":\"payout.funds_maturing\",\"retryable\":true,\"retry_after\":0}")
                        .ok("{\"uuid\":\"p\"}");
        client(http).build().payouts().approve("p1");
        assertEquals(2, http.calls().size());
    }

    @Test
    void doesNotForwardACallerKeyToListPages() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok("{\"items\":[],\"paginate\":{\"total\":0,\"per_page\":50,\"offset\":0,\"has_pages\":false}}");
        client(http)
                .build()
                .payouts()
                .history(
                        new com.oblodai.contract.requests.PayoutHistoryRequest(),
                        RequestOptions.of().idempotencyKey("k"))
                .firstPage();
        assertNull(http.onlyCall().header("idempotency-key"));
    }

    @Test
    void ignoresTheDateHeaderOnA401ThatIsNotASignatureFailure() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(
                                401,
                                "{\"code\":\"auth.ip_not_allowed\",\"retryable\":false}",
                                "date",
                                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                                        java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(1)));
        OblodaiException error =
                assertThrows(
                        OblodaiException.class,
                        () -> client(http).retry(RetryOptions.none()).build().account().balance());
        assertEquals("auth.ip_not_allowed", error.code());
        assertEquals(1, http.calls().size(), "no re-sign on an unrelated 401");
    }

    @Test
    void revertsTheClockCorrectionWhenTheReSignedAttemptIsStillRejected() {
        String date =
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(1));
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(401, "{\"code\":\"merchant.bad_signature\",\"retryable\":false}", "date", date)
                        .apiError(401, "{\"code\":\"merchant.bad_signature\",\"retryable\":false}", "date", date)
                        .ok("{\"balance\":{\"merchant\":[]}}");
        Oblodai oblodai = client(http).retry(RetryOptions.none()).build();

        assertThrows(OblodaiException.class, () -> oblodai.account().balance());
        oblodai.account().balance();

        long signedAt = Long.parseLong(http.calls().get(2).header("x-timestamp"));
        assertTrue(
                Math.abs(signedAt - System.currentTimeMillis() / 1000) < 5,
                "one bad Date header must not wedge the client");
    }

    @Test
    void keepsAPathPrefixOnTheBaseUrlAndSignsTheFullPath() {
        MockHttpClient http = new MockHttpClient().ok("{\"balance\":{\"merchant\":[]}}");
        client(http).baseUrl("https://gw.corp/oblodai/").build().account().balance();
        assertEquals("https://gw.corp/oblodai/v1/balance", http.onlyCall().uri().toString());
    }

    @Test
    void dropsCallerHeadersThatCollideWithSignedHeaders() {
        MockHttpClient http = new MockHttpClient().ok("{\"balance\":{\"merchant\":[]}}");
        client(http).header("x-signature", "zz").header("X-Trace", "t1").build().account().balance();

        assertTrue(http.onlyCall().header("x-signature").matches("^[0-9a-f]{64}$"));
        assertEquals("t1", http.onlyCall().header("x-trace"));
    }

    @Test
    void refusesPathParametersThatWouldRewriteTheUrl() {
        Oblodai oblodai = client(new MockHttpClient()).build();
        assertEquals(
                ConfigException.BAD_PATH_PARAM,
                assertThrows(ConfigException.class, () -> oblodai.payments().publicView("..")).code());
        assertEquals(
                ConfigException.BAD_PATH_PARAM,
                assertThrows(ConfigException.class, () -> oblodai.payments().publicView("a/b")).code());
    }

    @Test
    void namesTheRedirectTargetInsteadOfABareEnvelopeError() {
        MockHttpClient http =
                new MockHttpClient().raw(301, "", "location", "https://www.api.test/v1/balance");
        OblodaiException error =
                assertThrows(
                        OblodaiException.class,
                        () -> client(http).retry(RetryOptions.none()).build().account().balance());
        assertEquals(301, error.httpStatus());
        assertTrue(error.getMessage().contains("redirect"));
        assertTrue(error.getMessage().contains("www.api.test"));
    }

    @Test
    void stopsRetryingWhenTheOverallDeadlineWouldBeExceeded() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(503, "{\"code\":\"db.unavailable\",\"retryable\":true,\"retry_after\":2}")
                        .ok("{}");
        TransportException error =
                assertThrows(
                        TransportException.class,
                        () ->
                                client(http)
                                        .deadline(Duration.ofMillis(100))
                                        .build()
                                        .account()
                                        .balance());
        assertEquals(TransportException.DEADLINE, error.code());
        assertEquals(1, http.calls().size());
    }

    @Test
    void keepsTheRawBodyOutOfTheDefaultLoggingPath() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(
                                400,
                                "{\"code\":\"payment.below_minimum\",\"message\":\"too small\",\"retryable\":false}");
        ValidationException error =
                assertThrows(
                        ValidationException.class,
                        () ->
                                client(http)
                                        .build()
                                        .payments()
                                        .create(new PaymentRequest().amount("0").currency("USDT")));

        assertEquals("too small", error.getMessage());
        assertFalse(error.details().containsKey("raw"), "details() must not carry the body");
        assertTrue(error.toString().contains("payment.below_minimum"), "the code is in toString");
        assertFalse(error.toString().contains("{"), "the JSON body is not");
        assertNotNull(error.raw(), "the body is still there for deliberate inspection");
    }
}
