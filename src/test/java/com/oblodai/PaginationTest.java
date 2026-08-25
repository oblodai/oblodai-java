package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.PaymentHistoryRequest;
import com.oblodai.contract.requests.PayoutHistoryRequest;
import com.oblodai.core.Page;
import com.oblodai.models.Payment;
import com.oblodai.support.MockHttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Paging: one page on demand, every item on iteration, and nothing fetched until asked. */
class PaginationTest {

    private static String page(String items, int offset, int total, int perPage, boolean more) {
        return "{\"items\":"
                + items
                + ",\"paginate\":{\"total\":"
                + total
                + ",\"per_page\":"
                + perPage
                + ",\"offset\":"
                + offset
                + ",\"has_pages\":"
                + more
                + "}}";
    }

    private static String invoices(String... uuids) {
        List<String> out = new ArrayList<>();
        for (String uuid : uuids) out.add("{\"uuid\":\"" + uuid + "\"}");
        return "[" + String.join(",", out) + "]";
    }

    private static Oblodai client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("p")
                .secret("s")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .build();
    }

    @Test
    void firstPageFetchesOnceAndIterationWalksEveryPage() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(invoices("a", "b"), 0, 5, 2, true))
                        .ok(page(invoices("a", "b"), 0, 5, 2, true))
                        .ok(page(invoices("c", "d"), 2, 5, 2, true))
                        .ok(page(invoices("e"), 4, 5, 2, false));
        Oblodai oblodai = client(http);

        Page<Payment> first = oblodai.payments().history(new PaymentHistoryRequest().limit(2)).firstPage();
        assertEquals(List.of("a", "b"), first.items().stream().map(Payment::uuid).toList());
        assertTrue(first.paginate().hasPages());
        assertEquals(1, http.calls().size());

        List<String> seen = new ArrayList<>();
        for (Payment payment : oblodai.payments().history(new PaymentHistoryRequest().limit(2))) {
            seen.add(payment.uuid());
        }
        assertEquals(List.of("a", "b", "c", "d", "e"), seen);
        assertEquals(4, http.calls().size());
        assertTrue(http.calls().get(2).body().contains("\"offset\":2"), "walks by offset");
        assertTrue(http.calls().get(2).body().contains("\"limit\":2"));
    }

    @Test
    void nothingIsRequestedUntilThePagerIsConsumed() {
        MockHttpClient http = new MockHttpClient().ok(page(invoices("a"), 0, 1, 50, false));
        var pager = client(http).payments().history();
        assertTrue(http.calls().isEmpty(), "building the pager sends nothing");
        assertEquals(1, pager.all().size());
        assertEquals(1, http.calls().size());
    }

    @Test
    void allCollectsAcrossPagesWithACap() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(invoices("a", "b"), 0, 3, 2, true))
                        .ok(page(invoices("c"), 2, 3, 2, false));
        assertEquals(
                3,
                client(http)
                        .payouts()
                        .history(new PayoutHistoryRequest().limit(2))
                        .all()
                        .size());

        MockHttpClient capped = new MockHttpClient().ok(page(invoices("a", "b"), 0, 9, 2, true));
        assertEquals(
                1,
                client(capped)
                        .payments()
                        .history(new PaymentHistoryRequest().limit(2))
                        .all(1)
                        .size());
        assertEquals(1, capped.calls().size(), "the cap stops the walk");
    }

    @Test
    void theAsyncPagerWalksWithoutBlocking() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(invoices("a", "b"), 0, 3, 2, true))
                        .ok(page(invoices("c"), 2, 3, 2, false));
        List<Payment> all =
                Oblodai.builder()
                        .publicId("p")
                        .secret("s")
                        .baseUrl("https://api.test")
                        .httpClient(http)
                        .environment(Map.of())
                        .buildAsync()
                        .payments()
                        .history(new PaymentHistoryRequest().limit(2))
                        .all()
                        .join();
        assertEquals(3, all.size());
    }

    @Test
    void aStreamIsLazyToo() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(invoices("a", "b"), 0, 4, 2, true))
                        .ok(page(invoices("c", "d"), 2, 4, 2, false));
        assertEquals(
                List.of("a", "b", "c"),
                client(http)
                        .payments()
                        .history(new PaymentHistoryRequest().limit(2))
                        .stream()
                        .map(Payment::uuid)
                        .limit(3)
                        .toList());
    }
}
