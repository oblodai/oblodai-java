package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.generated.SigningProtocol;
import com.oblodai.generated.models.BatchInfoRequest;
import com.oblodai.core.FileResult;
import com.oblodai.core.Idempotency;
import com.oblodai.core.Pager;
import com.oblodai.core.RetryOptions;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.PermissionException;
import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.PayoutView;
import com.oblodai.generated.models.DownloadDocumentJobFileQuery;
import com.oblodai.generated.models.GetBatchDocumentQuery;
import com.oblodai.generated.models.GetPaymentLinkDocumentQuery;
import com.oblodai.generated.models.GetSplitDocumentQuery;
import com.oblodai.generated.models.HistoryRequest;
import com.oblodai.generated.models.PaymentRequest;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behaviour that lives in the resources rather than the transport. */
class ResourceBehaviourTest {

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
    void batchInfoIsOneCallWithTheMerchantsOneKey() {
        // There is no second key pair to fall back to any more: whichever route created the batch,
        // the merchant's API key reads it, and one call is all the SDK makes.
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.batchInfo("completed"));

        BatchInfoResponse info = client(http).build().batches().getInfo(BatchInfoRequest.builder().batchId("b1").build());

        assertEquals("b1", info.batchId());
        assertEquals(1, http.calls().size(), "no second attempt under another key");
        assertEquals("pk", http.onlyCall().header(SigningProtocol.HEADER_PUBLIC_ID));
    }

    @Test
    void batchInfoSurfacesA403AsItCameWithoutRetrying() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(403, "{\"code\":\"auth.ip_not_allowed\",\"retryable\":false}");

        PermissionException refused =
                assertThrows(
                        PermissionException.class,
                        () -> client(http).build().batches().getInfo(BatchInfoRequest.builder().batchId("b1").build()));
        assertEquals("auth.ip_not_allowed", refused.code());
        assertEquals(1, http.calls().size(), "a 403 is the caller's answer, not a cue to re-sign");
    }

    @Test
    void theAsyncBatchInfoBehavesTheSameWithoutBlocking() {
        MockHttpClient http = new MockHttpClient().ok(Fixtures.batchInfo("processing"));

        assertEquals(
                "b1",
                client(http)
                        .buildAsync()
                        .batches()
                        .getInfo(BatchInfoRequest.builder().batchId("b1").build())
                        .join()
                        .batchId());
        assertEquals(1, http.calls().size());
        assertEquals("pk", http.onlyCall().header(SigningProtocol.HEADER_PUBLIC_ID));
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

        FileResult file = client(http).build().documents().getStatement();

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

        assertEquals("reçu.pdf", client(http).build().documents().getFees().filename());
    }

    @Test
    void documentReportsKeyedByAnObjectSendItAsTheUuidQueryParameter() {
        // The gateway names the parameter `uuid` whatever the object is — a batch, a link, a wallet
        // — so the SDK's friendlier argument names must still land there.
        MockHttpClient http =
                new MockHttpClient()
                        .raw(200, "%PDF", "content-type", "application/pdf")
                        .raw(200, "%PDF", "content-type", "application/pdf")
                        .raw(200, "csv", "content-type", "text/csv")
                        .raw(200, "%PDF", "content-type", "application/pdf");
        Oblodai oblodai = client(http).build();

        oblodai.documents().getSplit(GetSplitDocumentQuery.builder().uuid("i1").build());
        oblodai.documents().getPaymentLink(GetPaymentLinkDocumentQuery.builder().uuid("l1").build());
        oblodai.documents().getBatch(GetBatchDocumentQuery.builder().uuid("b1").format("csv").build());
        oblodai.documents().downloadJobFile(DownloadDocumentJobFileQuery.builder().jobId("j1").build());

        assertTrue(http.calls().get(0).uri().getQuery().contains("uuid=i1"), "split report");
        assertTrue(http.calls().get(1).uri().getQuery().contains("uuid=l1"), "link report");
        assertTrue(http.calls().get(2).uri().getQuery().contains("uuid=b1"), "batch report");
        assertTrue(http.calls().get(2).uri().getQuery().contains("format=csv"), "format");
        assertTrue(http.calls().get(3).uri().getQuery().contains("job_id=j1"), "job file");
    }

    @Test
    void anUnconsumedPagerRequestsNothingAndCannotFailTheProcess() {
        MockHttpClient http = new MockHttpClient(); // nothing scripted: any request would fail
        Pager<PayoutView> pager =
                client(http).build().payouts().listHistory(HistoryRequest.builder().limit(10L).build());

        assertNotNull(pager);
        assertTrue(http.calls().isEmpty(), "a list that is never read never asks");
    }

    @Test
    void aFailingAsyncPagerFailsItsFutureRatherThanTheProcess() {
        MockHttpClient http =
                new MockHttpClient().apiError(404, "{\"code\":\"payment.not_found\",\"retryable\":false}");
        // The future is never joined: an ignored failure must not escape as an uncaught error.
        client(http).buildAsync().payments().listHistory().firstPage();
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
                                                PaymentRequest.builder().amount("1").currency("USDT").build(),
                                                RequestOptions.of().idempotencyKey("bad key")));
        assertEquals(ConfigException.BAD_IDEMPOTENCY_KEY, error.code());
        assertTrue(http.calls().isEmpty());
    }
}
