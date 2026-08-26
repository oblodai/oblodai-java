package com.oblodai.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.Oblodai;
import com.oblodai.OblodaiAsync;
import com.oblodai.support.MockHttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Every route, called through both clients, must produce the same request: the declared method and
 * path, the merchant's API key where the route is signed, the admin token only where the gateway
 * asks for it, an idempotency key exactly where the gateway deduplicates. The async tree is a hand-written mirror of the blocking one, and this is
 * what holds the mirror honest — neither tree has a route wired to the wrong place for long.
 */
class RouteWiringTest {

    /** A result generic enough to decode into any model the covered methods return. */
    private static final String ANY_RESULT =
            "{\"items\":[],\"paginate\":{\"total\":0,\"per_page\":1,\"offset\":0,\"has_pages\":false},"
                    + "\"enabled\":true,\"resolution\":\"accepted\"}";

    private static MockHttpClient scripted(RouteSpec spec) {
        MockHttpClient http = new MockHttpClient();
        if (spec.bare()) {
            http.raw(200, "%PDF-1.7", "content-type", "application/pdf");
        } else {
            http.ok(ANY_RESULT);
        }
        return http;
    }

    private static Oblodai.Builder client(MockHttpClient http) {
        return Oblodai.builder()
                .publicId("pk")
                .secret("s")
                .adminToken("adm")
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of());
    }

    /** The assertions both clients must satisfy for one route. */
    private static void assertRequest(RouteSpec spec, MockHttpClient http, String shape) {
        MockHttpClient.Recorded recorded = http.onlyCall();
        assertEquals(spec.method(), recorded.method(), shape + ": HTTP method");
        assertTrue(
                recorded.uri().getPath().matches("^" + spec.path().replaceAll("\\{[a-z_]+}", "[^/]+") + "$"),
                shape + ": path " + recorded.uri().getPath() + " matches " + spec.path());

        switch (spec.auth()) {
            case PUBLIC -> {
                assertNull(recorded.header("x-signature"), shape + ": public routes are unsigned");
                assertNull(recorded.header("x-public-id"), shape + ": and carry no key");
            }
            case ONBOARD -> {
                assertNull(recorded.header("x-signature"), shape + ": onboarding routes are unsigned");
                assertEquals("adm", recorded.header("x-admin-token"), shape);
            }
            // One API key signs every signed route; there is no second pair to pick between.
            case KEY -> {
                assertEquals("pk", recorded.header("x-public-id"), shape + ": the merchant's API key");
                assertTrue(
                        recorded.header("x-signature").matches("^[0-9a-f]{64}$"), shape + ": hex signature");
                assertNotNull(recorded.header("x-timestamp"), shape);
            }
        }
        if (spec.auth() != RouteAuth.ONBOARD) {
            assertNull(
                    recorded.header("x-admin-token"),
                    shape + ": the admin token rides on onboarding routes only");
        }

        if (spec.idempotent()) {
            assertNotNull(
                    recorded.header("idempotency-key"), shape + ": a deduplicated write carries a key");
        } else {
            assertNull(
                    recorded.header("idempotency-key"),
                    shape + ": a route the gateway does not deduplicate must not");
        }
    }

    @TestFactory
    List<DynamicTest> everyRouteIsWiredTheSameWayOnBothClients() {
        Map<String, Consumer<Oblodai>> blocking = RouteCoverage.blocking();
        Map<String, Consumer<OblodaiAsync>> asynchronous = RouteCoverageAsync.asynchronous();
        List<DynamicTest> tests = new ArrayList<>();
        blocking.forEach(
                (key, call) ->
                        tests.add(
                                DynamicTest.dynamicTest(
                                        key,
                                        () -> {
                                            RouteSpec spec = Routes.of(key);

                                            MockHttpClient blockingHttp = scripted(spec);
                                            call.accept(client(blockingHttp).build());
                                            assertRequest(spec, blockingHttp, "blocking");

                                            Consumer<OblodaiAsync> asyncCall = asynchronous.get(key);
                                            assertNotNull(asyncCall, key + " has no asynchronous call");
                                            MockHttpClient asyncHttp = scripted(spec);
                                            asyncCall.accept(client(asyncHttp).buildAsync());
                                            assertRequest(spec, asyncHttp, "async");

                                            assertEquals(
                                                    blockingHttp.onlyCall().uri(),
                                                    asyncHttp.onlyCall().uri(),
                                                    key + ": both clients must call the same URL");
                                            assertEquals(
                                                    blockingHttp.onlyCall().body(),
                                                    asyncHttp.onlyCall().body(),
                                                    key + ": both clients must send the same body");
                                        })));
        return tests;
    }
}
