package com.oblodai.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.oblodai.core.Json;
import com.oblodai.models.*;
import com.oblodai.support.Contract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Wire models against the golden bodies the gateway recorded.
 *
 * <p>Each row names a route, how to reach the object inside its result, and the model that must
 * describe it. Key sets must match EXACTLY: a field the gateway stopped sending fails here, and so
 * does a field it started sending that the model lacks. The second gate is decoding: every recorded
 * body must round-trip through the model without losing a field.
 */
class ModelsTest {

    private static final ObjectMapper MAPPER = Json.newMapper();

    /**
     * @param route the recorded route
     * @param pick how to reach the modelled object inside the result
     * @param model the record that must describe it
     * @param optional fields that are present on one shape of this object and absent on another
     */
    private record Row(
            String route, Function<JsonNode, JsonNode> pick, Class<?> model, Set<String> optional) {

        Row(String route, Function<JsonNode, JsonNode> pick, Class<?> model, String... optional) {
            this(route, pick, model, Set.of(optional));
        }
    }

    private static final Function<JsonNode, JsonNode> SELF = result -> result;
    private static final Function<JsonNode, JsonNode> FIRST_ITEM = result -> result.path("items").path(0);

    /** Payment fields only {@code info} carries. */
    private static final String[] PAYMENT_INFO_ONLY = {"refunds", "refund_status"};

    /** Payout fields only a failed or wallet-refund payout carries. */
    private static final String[] PAYOUT_EXTRAS = {"error", "error_code", "wallet_uuid"};

    private static final List<Row> ROWS =
            List.of(
                    new Row("POST /v1/payment", SELF, Payment.class, PAYMENT_INFO_ONLY),
                    new Row("POST /v1/payment/info", SELF, Payment.class, PAYMENT_INFO_ONLY),
                    new Row("POST /v1/payment/cancel", SELF, Payment.class, PAYMENT_INFO_ONLY),
                    new Row("POST /v1/payment/history", FIRST_ITEM, Payment.class, PAYMENT_INFO_ONLY),
                    new Row("GET /v1/pay/{id}", SELF, PublicPayment.class),
                    new Row("POST /v1/pay/{id}/select", SELF, PublicPayment.class),
                    new Row("POST /v1/link/{id}/checkout", SELF, PublicPayment.class),
                    new Row("POST /v1/payment/qr", SELF, QrCode.class),
                    new Row("GET /v1/pay/{id}/qr", SELF, QrCode.class),
                    new Row("POST /v1/payment/services", FIRST_ITEM, ServiceMethod.class),
                    new Row("POST /v1/payout/services", FIRST_ITEM, ServiceMethod.class),
                    new Row("POST /v1/payment/batch", SELF, BatchSubmitted.class),
                    new Row("POST /v1/payout/batch", SELF, BatchSubmitted.class),
                    new Row("POST /v1/refund/batch", SELF, BatchSubmitted.class),
                    new Row("POST /v1/transfer/batch", SELF, BatchSubmitted.class),
                    new Row("POST /v1/batch/info", SELF, BatchInfo.class),
                    new Row("POST /v1/payout", SELF, Payout.class, PAYOUT_EXTRAS),
                    new Row("POST /v1/payout/info", SELF, Payout.class, PAYOUT_EXTRAS),
                    new Row("POST /v1/payout/cancel", SELF, Payout.class, PAYOUT_EXTRAS),
                    new Row("POST /v1/payout/history", FIRST_ITEM, Payout.class, PAYOUT_EXTRAS),
                    new Row(
                            "POST /v1/payout/mass",
                            result -> result.path("items").path(0).path("result"),
                            Payout.class,
                            PAYOUT_EXTRAS),
                    new Row("POST /v1/payment/refund", SELF, Payout.class, PAYOUT_EXTRAS),
                    new Row("POST /v1/payment/resolve", SELF, ResolutionRefunded.class, PAYOUT_EXTRAS),
                    new Row("POST /v1/payout/calculate", SELF, PayoutCalculation.class),
                    new Row("POST /v1/payout/validate", SELF, PayoutValidation.class, "funded_by"),
                    new Row(
                            "POST /v1/payout/link",
                            SELF,
                            PayoutLink.class,
                            "claim_token",
                            "claim_url",
                            "batch_id",
                            "payout_id",
                            "claim_address",
                            "email",
                            "passcode"),
                    new Row(
                            "POST /v1/payout/link/info",
                            SELF,
                            PayoutLink.class,
                            "claim_token",
                            "claim_url",
                            "batch_id",
                            "payout_id",
                            "claim_address",
                            "email",
                            "passcode"),
                    new Row(
                            "POST /v1/payout/link/list",
                            FIRST_ITEM,
                            PayoutLink.class,
                            "claim_token",
                            "claim_url",
                            "batch_id",
                            "payout_id",
                            "claim_address",
                            "email",
                            "passcode"),
                    new Row(
                            "POST /v1/payout/link/cancel",
                            SELF,
                            PayoutLink.class,
                            "claim_token",
                            "claim_url",
                            "batch_id",
                            "payout_id",
                            "claim_address",
                            "email",
                            "passcode"),
                    new Row(
                            "POST /v1/payout/link/batch",
                            result -> result.path("items").path(0).path("result"),
                            PayoutLink.class,
                            "claim_token",
                            "claim_url",
                            "batch_id",
                            "payout_id",
                            "claim_address",
                            "email",
                            "passcode"),
                    new Row("GET /v1/claim/{token}", SELF, ClaimPreview.class),
                    new Row("POST /v1/claim/{token}", SELF, ClaimResult.class),
                    new Row("POST /v1/payment/link", SELF, PaymentLinkCreated.class),
                    new Row(
                            "POST /v1/payment/link/info",
                            SELF,
                            PaymentLink.class,
                            "payments",
                            "min_amount",
                            "max_amount",
                            "pinned_currency"),
                    new Row(
                            "POST /v1/payment/link/list",
                            FIRST_ITEM,
                            PaymentLink.class,
                            "payments",
                            "min_amount",
                            "max_amount",
                            "pinned_currency"),
                    new Row(
                            "GET /v1/link/{id}",
                            SELF,
                            PublicPaymentLink.class,
                            "min_amount",
                            "max_amount",
                            "pinned_currency"),
                    new Row("POST /v1/payment/link/toggle", SELF, PaymentLinkToggled.class),
                    new Row("POST /v1/balance", SELF, Balance.class),
                    new Row("POST /v1/referral/info", SELF, ReferralInfo.class),
                    new Row("POST /v1/auto-withdraw/list", FIRST_ITEM, AutoWithdrawRule.class),
                    new Row("POST /v1/auto-withdraw/set", FIRST_ITEM, AutoWithdrawRule.class),
                    new Row("POST /v1/api-allowlist/list", SELF, ApiAllowlist.class),
                    new Row("POST /v1/api-allowlist/add", SELF, ApiAllowlist.class),
                    new Row("POST /v1/api-allowlist/remove", SELF, ApiAllowlist.class),
                    new Row("POST /v1/api-allowlist/enable", SELF, ApiAllowlist.class),
                    new Row("POST /v1/payment/discount/list", FIRST_ITEM, DiscountRule.class),
                    new Row("POST /v1/payment/discount/set", SELF, DiscountRule.class),
                    new Row(
                            "POST /v1/split/rule/list",
                            FIRST_ITEM,
                            SplitRule.class,
                            "merchant_id"),
                    new Row(
                            "POST /v1/split/rule",
                            SELF,
                            SplitRule.class,
                            "active",
                            "address",
                            "network",
                            "merchant_id",
                            "note",
                            "reversible"),
                    new Row("GET /v1/currencies", SELF, Currencies.class),
                    new Row(
                            "GET /v1/currencies",
                            result -> result.path("currencies").path(0).path("networks").path(0),
                            CurrencyNetwork.class,
                            "contract"),
                    new Row("POST /v1/exchange-rate/list", FIRST_ITEM, ExchangeRate.class),
                    new Row("POST /v1/webhooks", SELF, WebhookEndpoint.class),
                    new Row("POST /v1/webhooks/rotate-secret", SELF, WebhookSecretRotated.class),
                    new Row("POST /v1/webhooks/deliveries", FIRST_ITEM, WebhookDelivery.class, "payload"),
                    new Row(
                            "GET /v1/sandbox/webhooks",
                            FIRST_ITEM,
                            WebhookDelivery.class,
                            "payload",
                            "sequence"),
                    new Row("POST /v1/payment/send-email", SELF, EmailSent.class),
                    new Row("POST /v1/payment/resend", SELF, OkResult.class),
                    new Row("POST /v1/payment/accepted/set", SELF, OkResult.class),
                    new Row("POST /v1/split/rule/delete", SELF, OkResult.class),
                    new Row("POST /v1/payment/accepted/list", FIRST_ITEM, AcceptedMethod.class, "reason"),
                    new Row("POST /v1/payment/accuracy/get", SELF, AccuracyConfig.class),
                    new Row("POST /v1/payment/accuracy/set", SELF, AccuracyConfig.class),
                    new Row("POST /v1/payment/autorefund/get", SELF, AutoRefundConfig.class),
                    new Row("POST /v1/payment/autorefund/set", SELF, AutoRefundConfig.class, "configured"),
                    new Row("POST /v1/payment/fee-config/get", SELF, PaymentFeeConfig.class),
                    new Row("POST /v1/payment/fee-config/set", SELF, PaymentFeeConfig.class, "enabled"),
                    new Row("POST /v1/payout/fee-config/get", SELF, PayoutFeeConfig.class),
                    new Row("POST /v1/payout/fee-config/set", SELF, PayoutFeeConfig.class, "configured"),
                    new Row("POST /v1/payout/refund-fee-config/get", SELF, RefundFeeConfig.class),
                    new Row(
                            "POST /v1/payout/refund-fee-config/set",
                            SELF,
                            RefundFeeConfig.class,
                            "configured"),
                    new Row("POST /v1/split/config/get", SELF, SplitConfig.class),
                    new Row("POST /v1/split/config/set", SELF, SplitConfig.class),
                    new Row("POST /v1/split/recipient/optin", SELF, SplitOptIn.class),
                    new Row("POST /v1/split/recipient/optin/get", SELF, SplitOptIn.class),
                    new Row("POST /v1/vrcs", SELF, VrcsStatus.class),
                    new Row(
                            "POST /v1/wallet",
                            SELF,
                            Wallet.class,
                            "destination_tag",
                            "memo",
                            "address_xaddress",
                            "address_muxed"),
                    new Row("POST /v1/wallet/block", SELF, WalletBlocked.class),
                    new Row("POST /v1/wallet/qr", SELF, WalletQr.class),
                    new Row(
                            "POST /v1/wallet/blocked-address-refund",
                            SELF,
                            Payout.class,
                            "error",
                            "error_code"),
                    new Row("POST /v1/transfer/to-personal", SELF, TransferToPersonal.class),
                    new Row("POST /v1/transfer/to-user", SELF, TransferToUser.class),
                    new Row(
                            "POST /v1/documents/jobs",
                            SELF,
                            DocumentJob.class,
                            "ready_within",
                            "file",
                            "error"),
                    new Row(
                            "POST /v1/documents/jobs/info",
                            SELF,
                            DocumentJob.class,
                            "ready_within",
                            "file",
                            "error"),
                    new Row(
                            "POST /v1/documents/jobs/info",
                            result -> result.path("file"),
                            DocumentJob.File.class),
                    new Row(
                            "POST /v1/test-webhook/payment",
                            SELF,
                            WebhookTestResult.class,
                            "error",
                            "url",
                            "duration_ms"),
                    new Row(
                            "POST /v1/test-webhook/payout",
                            SELF,
                            WebhookTestResult.class,
                            "error",
                            "url",
                            "duration_ms"),
                    new Row(
                            "POST /v1/test-webhook/wallet",
                            SELF,
                            WebhookTestResult.class,
                            "error",
                            "url",
                            "duration_ms"),
                    new Row(
                            "POST /v1/payment/testing-webhook",
                            SELF,
                            WebhookTestResult.class,
                            "error",
                            "status_code"),
                    new Row("POST /v1/sandbox/faucet", SELF, FaucetResult.class),
                    new Row("POST /v1/sandbox/deposit", SELF, SandboxDeposit.class),
                    new Row("POST /v1/sandbox/reset", SELF, SandboxReset.class),
                    new Row("POST /v1/sandbox/webhooks/replay", SELF, SandboxReplay.class),
                    new Row("POST /v1/merchants", SELF, MerchantOnboarded.class),
                    new Row("POST /v1/merchants", result -> result.path("api_key"), ApiKeyPair.class),
                    new Row("POST /v1/merchants/{id}/sandbox", SELF, SandboxStore.class),
                    new Row("POST /v1/auto-withdraw/delete", SELF, null));

    /** Routes the gateway guarantees to refuse for API keys, so no success body exists to model. */
    private static final Set<String> NOT_MODELLED =
            Set.of(
                    // API-key payouts auto-approve; approve serves the cabinet's maker-checker flow.
                    "POST /v1/payout/approve");

    @TestFactory
    List<DynamicTest> modelsMatchTheGoldenBodies() {
        Map<String, Contract.Fixture> fixtures = Contract.fixtures();
        List<DynamicTest> tests = new ArrayList<>();
        for (Row row : ROWS) {
            tests.add(
                    DynamicTest.dynamicTest(
                            row.route() + " -> " + (row.model() == null ? "{items}" : row.model().getSimpleName()),
                            () -> {
                                Contract.Fixture fixture = fixtures.get(row.route());
                                assertNotNull(fixture, "no fixture recorded for " + row.route());
                                if (!fixture.isSuccess()) return; // recorded as a refusal here

                                JsonNode node = row.pick().apply(fixture.result());
                                assertTrue(
                                        node != null && !node.isMissingNode() && !node.isNull(),
                                        row.route() + ": the picker found nothing");

                                Set<String> onWire = fieldNames(node);
                                if (row.model() == null) {
                                    assertEquals(Set.of("items"), onWire, row.route() + ": plain list shape");
                                    return;
                                }

                                Set<String> onModel = properties(row.model());
                                Set<String> missing = new LinkedHashSet<>(onModel);
                                missing.removeAll(onWire);
                                missing.removeAll(row.optional());
                                Set<String> unknown = new LinkedHashSet<>(onWire);
                                unknown.removeAll(onModel);
                                unknown.removeAll(row.optional());
                                assertEquals(
                                        Set.of(),
                                        missing,
                                        row.route() + ": the model declares fields the wire does not send");
                                assertEquals(
                                        Set.of(),
                                        unknown,
                                        row.route() + ": the wire sends fields the model does not declare");

                                Object decoded = MAPPER.convertValue(node, row.model());
                                assertNotNull(decoded, row.route() + ": did not decode");
                                // What the model keeps must be what the gateway sent: re-encoding the
                                // decoded object may only drop fields the wire itself left null.
                                JsonNode reencoded = MAPPER.valueToTree(decoded);
                                for (String field : onWire) {
                                    if (node.path(field).isNull()) continue;
                                    assertTrue(
                                            reencoded.has(field),
                                            row.route() + ": " + field + " was lost in decoding");
                                }
                            }));
        }
        return tests;
    }

    @Test
    void everyRecordedSuccessBodyIsCoveredByAModelRow() {
        Set<String> covered = new LinkedHashSet<>();
        for (Row row : ROWS) covered.add(row.route());
        for (Map.Entry<String, Contract.Fixture> entry : Contract.fixtures().entrySet()) {
            Contract.Fixture fixture = entry.getValue();
            if (!fixture.isSuccess() || NOT_MODELLED.contains(entry.getKey())) continue;
            if (!fixture.isJson()) continue; // a rendered document, not a modelled body
            assertTrue(
                    covered.contains(entry.getKey()),
                    entry.getKey() + ": recorded success body has no model row");
        }
    }

    @Test
    void statusesInTheGoldenBodiesAreInTheVocabulary() {
        for (JsonNode payment : Contract.result("POST /v1/payment/history").path("items")) {
            assertNotEqualsUnknown(PaymentStatus.from(payment.path("status").asText()), payment);
        }
        for (JsonNode payout : Contract.result("POST /v1/payout/history").path("items")) {
            assertNotEqualsUnknown(PayoutStatus.from(payout.path("status").asText()), payout);
        }
        for (JsonNode link : Contract.result("POST /v1/payout/link/list").path("items")) {
            assertNotEqualsUnknown(PayoutLinkStatus.from(link.path("status").asText()), link);
        }
        for (JsonNode delivery : Contract.result("POST /v1/webhooks/deliveries").path("items")) {
            assertNotEqualsUnknown(DeliveryStatus.from(delivery.path("status").asText()), delivery);
            assertNotEqualsUnknown(EventType.from(delivery.path("event_type").asText()), delivery);
        }
        for (JsonNode currency : Contract.result("GET /v1/currencies").path("currencies")) {
            for (JsonNode network : currency.path("networks")) {
                assertNotEqualsUnknown(Network.from(network.path("network").asText()), network);
            }
        }
    }

    @Test
    void webhookSamplesDecodeIntoTheEventModels() {
        for (JsonNode sample : Contract.webhookSamples()) {
            JsonNode body = sample.path("body");
            assertNotEqualsUnknown(
                    EventType.from(sample.path("headers").path("X-Webhook-Event").asText()), sample);
            Class<?> model =
                    switch (body.path("type").asText()) {
                        case "payment" -> PaymentEvent.class;
                        case "payout" -> PayoutEvent.class;
                        default -> WalletEvent.class;
                    };
            Set<String> onWire = fieldNames(body);
            Set<String> onModel = properties(model);
            assertEquals(onModel, onWire, "event body of " + body.path("type").asText());
            assertNotNull(
                    MAPPER.convertValue(body, WebhookEvent.class), "the union decodes by its type field");
        }
    }

    @Test
    void everyRecordedErrorCodeIsKnownAndCarriesTheDocumentedEnvelope() {
        Map<String, Contract.Fixture> samples = Contract.errorSamples();
        assertTrue(samples.size() >= 5, "error samples are recorded");
        samples.forEach(
                (code, fixture) -> {
                    assertTrue(ErrorCodes.isKnown(code), code + " is not in the contract's code list");
                    JsonNode error = fixture.error();
                    assertEquals(code, error.path("code").asText());
                    assertTrue(error.path("retryable").isBoolean(), code + ": retryable is a boolean");
                    assertTrue(error.path("request_id").isTextual(), code + ": a request id is present");
                    if (fixture.status() == 429) {
                        assertTrue(error.path("retry_after").asInt() > 0, code + ": retry_after is set");
                    }
                });
    }

    @Test
    void recordedRequestBodiesOnlyUseDocumentedFields() {
        Map<String, JsonNode> schemas = new java.util.LinkedHashMap<>();
        for (JsonNode route : Contract.contract().path("routes")) {
            schemas.put(
                    route.path("method").asText() + " " + route.path("path").asText(),
                    route.path("request_schema"));
        }
        Contract.fixtures()
                .forEach(
                        (route, fixture) -> {
                            JsonNode schema = schemas.get(route);
                            if (schema == null || !schema.has("properties") || fixture.request() == null) return;
                            if (!fixture.request().isObject()) return;
                            for (String field : fieldNames(fixture.request())) {
                                assertTrue(
                                        schema.path("properties").has(field),
                                        route + ": the recorded journey sent undocumented field \"" + field + "\"");
                            }
                        });
    }

    private static void assertNotEqualsUnknown(Enum<?> value, JsonNode context) {
        assertTrue(
                value != null && !value.name().equals("UNKNOWN"),
                "the wire carries a value outside the SDK's vocabulary: " + context);
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> out = new LinkedHashSet<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) out.add(it.next());
        return out;
    }

    /** The property names Jackson will read and write for a model — the mapping under test. */
    private static Set<String> properties(Class<?> model) {
        BeanDescription description =
                MAPPER.getSerializationConfig().introspect(MAPPER.constructType(model));
        Set<String> out = new LinkedHashSet<>();
        for (BeanPropertyDefinition property : description.findProperties()) out.add(property.getName());
        return out;
    }
}
