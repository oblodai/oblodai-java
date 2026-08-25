package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.BatchInfoRequest;
import com.oblodai.contract.requests.PayoutHistoryRequest;
import com.oblodai.core.FileResult;
import com.oblodai.core.Idempotency;
import com.oblodai.core.Pager;
import com.oblodai.core.RetryOptions;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.PermissionException;
import com.oblodai.models.BatchInfo;
import com.oblodai.models.Payout;
import com.oblodai.support.MockHttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behaviour that lives in the resources rather than the transport. */
class ResourceBehaviourTest {

    private static Oblodai.Builder client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("pk")
                .secret("s")
                .payoutKey("wk", "s2")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .retry(new RetryOptions(2, 1, 2, 30_000));
    }

    @Test
    void batchInfoRetriesOnceWithThePayoutKeyWhenTheGatewayWantsTheOtherKind() {
        // The gateway requires the key kind that created the batch, and a caller cannot know which
        // that was; the SDK asks again with the payout key rather than making them guess.
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(403, "{\"code\":\"merchant.wrong_key_kind\",\"retryable\":false}")
                        .ok("{\"batch_id\":\"b1\",\"kind\":\"payout\",\"status\":\"completed\"}");

        BatchInfo info = client(http).build().batches().info(new BatchInfoRequest().batchId("b1"));

        assertEquals("b1", info.batchId());
        assertEquals(2, http.calls().size());
        assertEquals("pk", http.calls().get(0).header("x-public-id"), "the payment key first");
        assertEquals("wk", http.calls().get(1).header("x-public-id"), "then the payout key");
    }

    @Test
    void batchInfoDoesNotLoopWhenThePayoutKeyIsRefusedToo() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(403, "{\"code\":\"merchant.wrong_key_kind\",\"retryable\":false}")
                        .apiError(403, "{\"code\":\"merchant.wrong_key_kind\",\"retryable\":false}");

        assertThrows(
                PermissionException.class,
                () -> client(http).build().batches().info(new BatchInfoRequest().batchId("b1")));
        assertEquals(2, http.calls().size(), "one fallback, not a loop");
    }

    @Test
    void theAsyncBatchInfoFallsBackTheSameWayWithoutBlocking() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(403, "{\"code\":\"merchant.wrong_key_kind\",\"retryable\":false}")
                        .ok("{\"batch_id\":\"b1\"}");

        assertEquals(
                "b1",
                client(http)
                        .buildAsync()
                        .batches()
                        .info(new BatchInfoRequest().batchId("b1"))
                        .join()
                        .batchId());
        assertEquals("wk", http.calls().get(1).header("x-public-id"));
    }

    @Test
    void aBareRouteAnswersWithBytesItsContentTypeAndItsFilename() {
        MockHttpClient http =
                new MockHttpClient()
                        .raw(
                                200,
                                "%PDF-1.7 statement",
                                "content-type",
                                "application/pdf",
                                "content-disposition",
                                "attachment; filename=\"statement-2026-01.pdf\"");

        FileResult file = client(http).build().documents().statement();

        assertEquals("application/pdf", file.contentType());
        assertEquals("statement-2026-01.pdf", file.filename());
        assertTrue(file.size() > 0);
        assertEquals("%PDF-1.7 statement", new String(file.bytes(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void aBareRouteWithAUtf8FilenameDecodesIt() {
        MockHttpClient http =
                new MockHttpClient()
                        .raw(
                                200,
                                "%PDF",
                                "content-type",
                                "application/pdf",
                                "content-disposition",
                                "attachment; filename*=UTF-8''re%C3%A7u.pdf");

        assertEquals("reçu.pdf", client(http).build().documents().feeSchedule().filename());
    }

    @Test
    void anUnconsumedPagerRequestsNothingAndCannotFailTheProcess() {
        MockHttpClient http = new MockHttpClient(); // nothing scripted: any request would fail
        Pager<Payout> pager =
                client(http).build().payouts().history(new PayoutHistoryRequest().limit(10));

        assertNotNull(pager);
        assertTrue(http.calls().isEmpty(), "a list that is never read never asks");
    }

    @Test
    void aFailingAsyncPagerFailsItsFutureRatherThanTheProcess() {
        MockHttpClient http =
                new MockHttpClient().apiError(404, "{\"code\":\"payment.not_found\",\"retryable\":false}");
        // The future is never joined: an ignored failure must not escape as an uncaught error.
        client(http).buildAsync().payments().history().firstPage();
        System.gc();
        assertEquals(1, http.calls().size());
    }

    @Test
    void idempotencyKeysMustSurviveAHeaderRoundTrip() {
        assertThrows(ConfigException.class, () -> Idempotency.assertValid(""));
        assertThrows(ConfigException.class, () -> Idempotency.assertValid(null));
        assertThrows(ConfigException.class, () -> Idempotency.assertValid("has a space"));
        assertThrows(ConfigException.class, () -> Idempotency.assertValid("line\nbreak"));
        assertThrows(ConfigException.class, () -> Idempotency.assertValid("é-not-ascii"));
        assertThrows(ConfigException.class, () -> Idempotency.assertValid("x".repeat(256)));

        Idempotency.assertValid("x".repeat(255));
        Idempotency.assertValid("order-1001:retry#2");
        assertTrue(Idempotency.newKey().matches("^[0-9a-f-]{36}$"));
    }

    @Test
    void aCallerKeyThatCannotBeSentIsRefusedBeforeAnythingLeavesTheProcess() {
        MockHttpClient http = new MockHttpClient().ok("{}");
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                client(http)
                                        .build()
                                        .payments()
                                        .create(
                                                new com.oblodai.contract.requests.PaymentRequest()
                                                        .amount("1")
                                                        .currency("USDT"),
                                                RequestOptions.of().idempotencyKey("bad key")));
        assertEquals(ConfigException.BAD_IDEMPOTENCY_KEY, error.code());
        assertTrue(http.calls().isEmpty());
    }
}
