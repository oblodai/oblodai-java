package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Page;
import com.oblodai.core.Pager;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.generated.models.HistoryRequest;
import com.oblodai.generated.models.PaymentHistoryRequest;
import com.oblodai.generated.models.PaymentView;
import com.oblodai.generated.models.PayoutView;
import com.oblodai.generated.models.SandboxDelivery;
import com.oblodai.generated.models.SandboxListWebhooksQuery;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Paged lists: an iterator over every item, {@code byPage()}, the first page, a cap. */
class PaginationTest {

    private static String page(String items, long offset, boolean more) {
        return Fixtures.page(items, offset, more);
    }

    private static Oblodai.Builder builder(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("p")
                .secret("s")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of());
    }

    private static Oblodai client(MockHttpClient http) {
        return builder(http).build();
    }

    private static PaymentHistoryRequest limit(long limit) {
        return PaymentHistoryRequest.builder().limit(limit).build();
    }

    private static HistoryRequest payoutLimit(long limit) {
        return HistoryRequest.builder().limit(limit).build();
    }

    @Test
    void firstPageFetchesOnceAndIterationWalksEveryPage() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("c", "d"), 2, true))
                        .ok(page(Fixtures.payments("e"), 4, false));
        Oblodai oblodai = client(http);

        Page<PaymentView> first = oblodai.payments().listHistory(limit(2)).firstPage();
        assertEquals(List.of("a", "b"), first.items().stream().map(PaymentView::uuid).toList());
        assertTrue(first.hasPages());
        assertEquals(100, first.total());
        assertEquals(1, http.calls().size());

        List<String> seen = new ArrayList<>();
        for (PaymentView payment : oblodai.payments().listHistory(limit(2))) {
            seen.add(payment.uuid());
        }
        assertEquals(List.of("a", "b", "c", "d", "e"), seen);
        assertEquals(4, http.calls().size());
        assertTrue(http.calls().get(2).body().contains("\"offset\":2"), "walks by offset");
        assertTrue(http.calls().get(2).body().contains("\"limit\":2"));
    }

    @Test
    void byPageYieldsEveryPageOneRequestEach() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("c"), 2, false));
        Pager<PaymentView> pager = client(http).payments().listHistory(limit(2));

        List<Integer> sizes = new ArrayList<>();
        for (Page<PaymentView> page : pager.byPage()) {
            sizes.add(page.items().size());
        }
        assertEquals(List.of(2, 1), sizes);
        assertEquals(2, http.calls().size());

        // The first page is cached: walking again re-reads only the pages after it.
        http.ok(page(Fixtures.payments("c"), 2, false));
        int pages = 0;
        for (Page<PaymentView> ignored : pager.byPage()) {
            pages++;
        }
        assertEquals(2, pages);
        assertEquals(3, http.calls().size());
    }

    @Test
    void nothingIsRequestedUntilThePagerIsConsumed() {
        MockHttpClient http = new MockHttpClient().ok(page(Fixtures.payments("a"), 0, false));
        Pager<PaymentView> pager = client(http).payments().listHistory();
        assertTrue(http.calls().isEmpty(), "building the pager sends nothing");
        assertEquals(1, pager.all().size());
        assertEquals(1, http.calls().size());
        assertTrue(http.onlyCall().body().contains("\"limit\":50"), "the default page size");
    }

    @Test
    void allCollectsAcrossPagesWithACap() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page("[" + Fixtures.payout("a") + "," + Fixtures.payout("b") + "]", 0, true))
                        .ok(page("[" + Fixtures.payout("c") + "]", 2, false));
        List<PayoutView> payouts = client(http).payouts().listHistory(payoutLimit(2)).all();
        assertEquals(3, payouts.size());

        MockHttpClient capped = new MockHttpClient().ok(page(Fixtures.payments("a", "b"), 0, true));
        assertEquals(1, client(capped).payments().listHistory(limit(2)).all(1).size());
        assertEquals(1, capped.calls().size(), "the cap stops the walk");
    }

    @Test
    void aGetListPagesInTheQuery() {
        String delivery =
                "{\"id\":\"d1\",\"url\":\"https://shop.test/hook\",\"event_type\":\"invoice.paid\","
                        + "\"attempts\":1,\"last_error\":\"\",\"payload\":{},\"status\":\"delivered\","
                        + "\"created_at\":\"2026-09-24T10:00:00Z\",\"updated_at\":\"2026-09-24T10:00:00Z\"}";
        MockHttpClient http = new MockHttpClient().ok(page("[" + delivery + "]", 5, false));
        Pager<SandboxDelivery> pager =
                client(http)
                        .sandbox()
                        .listWebhooks(SandboxListWebhooksQuery.builder().limit(10L).offset(5L).build());
        assertEquals("d1", pager.firstPage().items().get(0).id());
        assertEquals(
                "https://api.test/v1/sandbox/webhooks?limit=10&offset=5", http.onlyCall().uri().toString());
    }

    @Test
    void aListRefusesAnIdempotencyKeyBeforeSendingAnything() {
        MockHttpClient http = new MockHttpClient();
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                client(http)
                                        .payments()
                                        .listHistory(limit(2), RequestOptions.of().idempotencyKey("k")));
        assertEquals(ConfigException.IDEMPOTENCY_UNSUPPORTED, error.code());
        assertTrue(http.calls().isEmpty());
    }

    @Test
    void anAnswerThatIsNotAListIsAContractFailure() {
        MockHttpClient http = new MockHttpClient().ok("{\"items\":\"nope\"}");
        ContractException error =
                assertThrows(ContractException.class, () -> client(http).payments().listHistory().firstPage());
        assertEquals(ContractException.BAD_ENVELOPE, error.code());
    }

    @Test
    void theAsyncPagerWalksWithoutBlocking() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("c"), 2, false));
        List<PaymentView> all = builder(http).buildAsync().payments().listHistory(limit(2)).all().join();
        assertEquals(3, all.size());

        MockHttpClient paged =
                new MockHttpClient()
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("c"), 2, false));
        List<Integer> sizes = new ArrayList<>();
        builder(paged)
                .buildAsync()
                .payments()
                .listHistory(limit(2))
                .byPage(page -> sizes.add(page.items().size()))
                .join();
        assertEquals(List.of(2, 1), sizes);
    }

    @Test
    void aStreamIsLazyToo() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(Fixtures.payments("a", "b"), 0, true))
                        .ok(page(Fixtures.payments("c"), 2, false));
        List<String> firstTwo =
                client(http).payments().listHistory(limit(2)).stream().limit(2).map(PaymentView::uuid).toList();
        assertEquals(List.of("a", "b"), firstTwo);
        assertEquals(1, http.calls().size(), "the second page was never needed");
    }
}
