package com.oblodai.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oblodai.support.Contract;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The generated route registry is the contract snapshot, field for field. Not just the set of
 * routes: the method, the path, the key kind, whether the gateway deduplicates the write, whether a
 * transport failure may be re-sent, whether the answer is bytes rather than an envelope, and which
 * list shape it is. Every one of those decides how the SDK treats money, and none of them is
 * inferred from the path — {@code safe} in particular is the core's own hand-classified verdict.
 */
class RoutesTest {

    /** Routes the gateway serves but the merchant SDK does not model. */
    private static final String NOT_MODELLED = "^/(healthz|readyz|docs|openapi\\.json|internal).*";

    /** The contract's own entry for one route, or null. */
    private static JsonNode declared(String key) {
        for (JsonNode route : Contract.contract().path("routes")) {
            if ((route.path("method").asText() + " " + route.path("path").asText()).equals(key)) {
                return route;
            }
        }
        return null;
    }

    /**
     * Every difference between a generated {@link RouteSpec} and the contract entry it came from.
     * Returning them rather than asserting inline is what lets the flipped-flag test below prove
     * this comparison actually looks at each field.
     */
    static List<String> differences(RouteSpec spec, JsonNode route) {
        List<String> out = new ArrayList<>();
        if (!spec.method().equals(route.path("method").asText())) out.add("method");
        if (!spec.path().equals(route.path("path").asText())) out.add("path");
        if (!spec.auth().name().equalsIgnoreCase(route.path("auth").asText())) out.add("auth");
        if (spec.idempotent() != route.path("idempotent").asBoolean()) out.add("idempotent");
        if (!route.path("safe").isBoolean()) {
            out.add("safe (the contract does not declare it)");
        } else if (spec.safe() != route.path("safe").asBoolean()) {
            out.add("safe");
        }
        if (spec.bare() != route.path("bare").asBoolean()) out.add("bare");
        String list = route.hasNonNull("list") ? route.path("list").asText() : null;
        String specList = spec.list() == null ? null : spec.list().name().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Objects.equals(list, specList)) out.add("list");
        return out;
    }

    @Test
    void theRegistryIsTheGatewaysMerchantSurfaceNothingMoreAndNothingLess() {
        Set<String> declared = new LinkedHashSet<>();
        for (JsonNode route : Contract.contract().path("routes")) {
            String path = route.path("path").asText();
            if (path.matches(NOT_MODELLED)) continue;
            declared.add(route.path("method").asText() + " " + path);
        }
        assertEquals(declared, Routes.ALL.keySet());
        assertEquals(107, Routes.ALL.size(), "the merchant surface of this contract snapshot");
    }

    @TestFactory
    List<DynamicTest> everyRouteMatchesItsContractEntryFieldForField() {
        List<DynamicTest> tests = new ArrayList<>();
        Routes.ALL.forEach(
                (key, spec) ->
                        tests.add(
                                DynamicTest.dynamicTest(
                                        key,
                                        () -> {
                                            JsonNode route = declared(key);
                                            assertNotNull(route, key + " is not in contract.json");
                                            assertEquals(
                                                    List.of(),
                                                    differences(spec, route),
                                                    key + " differs from contract.json");
                                        })));
        return tests;
    }

    @Test
    void theComparisonCatchesAFlippedFlag() {
        String key = "POST /v1/payout";
        RouteSpec spec = Routes.of(key);
        JsonNode route = declared(key);
        assertNotNull(route);
        assertEquals(List.of(), differences(spec, route));

        ObjectNode flipped = ((ObjectNode) route).deepCopy();
        flipped.put("safe", !spec.safe());
        assertEquals(List.of("safe"), differences(spec, flipped), "a flipped safe flag is caught");

        ObjectNode noSafe = ((ObjectNode) route).deepCopy();
        noSafe.remove("safe");
        assertFalse(
                differences(spec, noSafe).isEmpty(),
                "a contract that stopped declaring safe is caught, not defaulted");

        ObjectNode notIdempotent = ((ObjectNode) route).deepCopy();
        notIdempotent.put("idempotent", !spec.idempotent());
        assertEquals(List.of("idempotent"), differences(spec, notIdempotent));

        ObjectNode otherAuth = ((ObjectNode) route).deepCopy();
        otherAuth.put("auth", "public");
        assertEquals(List.of("auth"), differences(spec, otherAuth));
    }

    @Test
    void everySafeRouteIsAReadAndNoMoneyMoverIsSafe() {
        // A cross-check on the contract itself: the SDK re-sends a safe route after a transport
        // failure without an idempotency key, so "safe" must never sit on a route that moves money.
        for (String key : List.of("POST /v1/payout", "POST /v1/payment", "POST /v1/payment/refund",
                "POST /v1/transfer/to-user", "POST /v1/payout/link", "POST /v1/wallet")) {
            assertFalse(Routes.of(key).safe(), key + " must never be re-sent blindly");
        }
        for (String key : List.of("POST /v1/payment/info", "POST /v1/payout/history",
                "POST /v1/balance", "GET /v1/currencies")) {
            assertTrue(Routes.of(key).safe(), key + " is a read");
        }
    }

    @Test
    void everyRecordedFixtureBelongsToAKnownRoute() {
        for (String route : Contract.fixtures().keySet()) {
            assertNotNull(Routes.ALL.get(route), route + " is not in the route registry");
        }
    }

    @Test
    void bothCoverageTablesAreTheWholeRegistry() {
        assertEquals(
                Routes.ALL.keySet(),
                new LinkedHashSet<>(RouteCoverage.blocking().keySet()),
                "the blocking coverage table and the route registry must be the same set");
        assertEquals(
                Routes.ALL.keySet(),
                new LinkedHashSet<>(RouteCoverageAsync.asynchronous().keySet()),
                "the async coverage table and the route registry must be the same set");
    }
}
