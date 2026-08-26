package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.requests.PaymentHistoryRequest;
import com.oblodai.errors.ContractException;
import com.oblodai.support.MockHttpClient;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

/**
 * A list route must answer with the list envelope. A body that lost its {@code items} or its
 * {@code paginate} decodes into an empty page, and "there is nothing" is not something an SDK may
 * invent: an integration that pages over payouts to reconcile them would read the silence as "no
 * payouts today".
 */
class ListShapeTest {

    private static Oblodai.Builder client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("pk")
                .secret("s")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of());
    }

    @Test
    void aPageWithoutItemsIsAContractFailureNotAnEmptyPage() {
        MockHttpClient http = new MockHttpClient().ok("{\"paginate\":{\"total\":0,\"has_pages\":false}}");
        ContractException failure =
                assertThrows(
                        ContractException.class,
                        () -> client(http).build().payments().history(new PaymentHistoryRequest()).firstPage());
        assertTrue(failure.getMessage().contains("list envelope"), failure.getMessage());
    }

    @Test
    void aPageWithoutPaginateIsAContractFailureToo() {
        MockHttpClient http = new MockHttpClient().ok("{\"items\":[{\"uuid\":\"a\"}]}");
        assertThrows(
                ContractException.class,
                () -> client(http).build().payouts().history().firstPage());
    }

    @Test
    void aPlainListWithoutItemsIsAContractFailure() {
        // auto-withdraw/list answers {items: [...]} with no paginate block: still a list, still a
        // shape the SDK must not invent an empty answer for.
        MockHttpClient http = new MockHttpClient().ok("{\"ok\":true}");
        assertThrows(ContractException.class, () -> client(http).build().settings().listAutoWithdraw());

        MockHttpClient wellFormed = new MockHttpClient().ok("{\"items\":[]}");
        assertEquals(0, client(wellFormed).build().settings().listAutoWithdraw().size());
    }

    @Test
    void theAsyncPagerFailsItsFutureTheSameWay() {
        MockHttpClient http = new MockHttpClient().ok("{\"paginate\":{\"total\":0,\"has_pages\":false}}");
        CompletionException failure =
                assertThrows(
                        CompletionException.class,
                        () -> client(http).buildAsync().payments().history().firstPage().join());
        assertEquals(ContractException.class, failure.getCause().getClass());
    }

    @Test
    void anEmptyButWellFormedPageIsStillFine() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok("{\"items\":[],\"paginate\":{\"total\":0,\"per_page\":50,\"offset\":0,\"has_pages\":false}}");
        assertEquals(0, client(http).build().payments().history().firstPage().items().size());
    }
}
