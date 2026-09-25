package com.oblodai.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.ApiResponse;
import com.oblodai.Oblodai;
import com.oblodai.core.FileResult;
import com.oblodai.core.Signing;
import com.oblodai.generated.models.PaymentInfoResult;
import com.oblodai.generated.models.PaymentView;
import com.oblodai.generated.models.PayoutItem;
import com.oblodai.support.Clients;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Spec §3.10: the README code runs. Every ```java and ```kotlin block of README.md and README.ru.md
 * must appear in examples/ (compiled with the tests), and every {@link ReadmeSnippets} method runs
 * here against a scripted gateway; the Kotlin block runs in {@code ReadmeKotlinTest}.
 */
class ReadmeExamplesTest {

    private static final Pattern BLOCK = Pattern.compile("```(java|kotlin)\\n(.*?)```", Pattern.DOTALL);

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String examplesSource() throws IOException {
        StringBuilder out = new StringBuilder();
        try (Stream<Path> files = Files.walk(Path.of("examples"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt")).toList()) {
                out.append(Files.readString(file)).append('\n');
            }
        }
        return normalize(out.toString());
    }

    @Test
    void everyReadmeBlockIsCompiledCode() throws IOException {
        String source = examplesSource();
        for (String readme : List.of("README.md", "README.ru.md")) {
            Matcher m = BLOCK.matcher(Files.readString(Path.of(readme)));
            int blocks = 0;
            while (m.find()) {
                blocks++;
                assertTrue(
                        source.contains(normalize(m.group(2))),
                        readme + ": a " + m.group(1) + " block is not in examples/:\n" + m.group(2));
            }
            assertTrue(blocks >= 10, readme + " has " + blocks + " code blocks");
        }
    }

    @Test
    void theQuickStartRuns() {
        Oblodai configured = ReadmeSnippets.configure();
        assertNotNull(configured.payments());
        configured.close();

        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.payment("u1"))
                        .ok(Fixtures.payment("u1", "paid", "25"))
                        .ok(Fixtures.payout("p1"));
        Oblodai oblodai = Clients.client(http);

        PaymentView invoice = ReadmeSnippets.acceptPayment(oblodai);
        PaymentInfoResult now = ReadmeSnippets.readBack(oblodai, invoice);
        PayoutItem payout = ReadmeSnippets.sendPayout(oblodai);

        assertEquals("u1", invoice.uuid());
        assertEquals("paid", now.status().value());
        assertEquals("p1", payout.uuid());
        assertEquals("withdrawal-77", http.calls().get(2).header("idempotency-key"));
        assertTrue(http.calls().get(0).body().contains("\"amount\":\"25\""));
    }

    @Test
    void optionsListsErrorsAndRawRun() {
        String balance = "{\"balance\":{\"merchant\":[]}}";
        MockHttpClient http =
                new MockHttpClient()
                        .ok(balance)
                        .ok(Fixtures.page(Fixtures.payments("a", "b"), 0, false))
                        .ok(Fixtures.page(Fixtures.payments("a"), 0, false))
                        .apiError(404, "{\"code\":\"payment.not_found\",\"message\":\"no\",\"retryable\":false}")
                        .ok(Fixtures.payment("r1"));
        Oblodai oblodai = Clients.client(http);

        assertNotNull(ReadmeSnippets.options(oblodai));
        assertEquals("order-1001-balance", http.calls().get(0).header("x-request-id"));
        assertEquals("t-42", http.calls().get(0).header("x-trace"));
        assertEquals(2, ReadmeSnippets.lists(oblodai));
        assertEquals("payment.not_found", ReadmeSnippets.errors(oblodai));
        ApiResponse<PaymentView> raw = ReadmeSnippets.raw(oblodai);
        assertEquals("r1", raw.value().uuid());
    }

    @Test
    void jobsWebhooksAndAsyncRun() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.batchSubmitted())
                        .ok(Fixtures.batchInfo("completed"))
                        .ok(Fixtures.documentAccepted())
                        .ok(Fixtures.documentJob("done"))
                        .raw(200, "%PDF", "content-type", "application/pdf",
                                "content-disposition", "attachment; filename=\"statement.pdf\"")
                        .ok(Fixtures.payment("a1"));
        Oblodai oblodai = Clients.client(http);

        FileResult file = ReadmeSnippets.jobs(oblodai);
        assertEquals("statement.pdf", file.filename());
        assertEquals("a1", ReadmeSnippets.async(oblodai));

        String body = Fixtures.paymentWebhook("u1", 3);
        long now = System.currentTimeMillis() / 1000;
        Map<String, String> headers =
                Map.of("X-Webhook-Timestamp", Long.toString(now), "X-Webhook-Signature", Signing.signWebhook("whsec", now, body));
        byte[] raw = body.getBytes(StandardCharsets.UTF_8);
        assertEquals(200, ReadmeSnippets.webhooks(raw, headers, "whsec"));
        assertEquals(401, ReadmeSnippets.webhooks(raw, headers, "another-secret"));
    }

    @Test
    void theExampleProgramsRun() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.payment("u1"))
                        .ok(Fixtures.payment("u1", "paid", "25"))
                        .ok(Fixtures.payoutValidation(true))
                        .ok(Fixtures.payout("p1"));
        Oblodai oblodai = Clients.client(http);
        assertTrue(AcceptPayment.run(oblodai).contains("is paid"), "accept payment");
        assertTrue(SendPayout.run(oblodai).startsWith("payout p1 is pending"), "send payout");

        MockHttpClient refused = new MockHttpClient().ok(Fixtures.payoutValidation(false));
        assertTrue(SendPayout.run(Clients.client(refused)).startsWith("payout would be refused"));

        MockHttpClient failing =
                new MockHttpClient()
                        .ok(Fixtures.payoutValidation(true))
                        .apiError(409, "{\"code\":\"payout.insufficient_funds\",\"message\":\"low\",\"retryable\":false}");
        assertTrue(
                SendPayout.run(Clients.client(failing)).startsWith("payout failed: [payout.insufficient_funds]"));
    }

    @Test
    void theWebhookReceiverVerifiesDeduplicatesAndOrders() {
        WebhookReceiver receiver = new WebhookReceiver("whsec", null);
        long now = System.currentTimeMillis() / 1000;
        List<Integer> statuses = new ArrayList<>();
        for (String body : List.of(Fixtures.paymentWebhook("u1", 5), Fixtures.paymentWebhook("u1", 5), "not json")) {
            Map<String, String> headers =
                    Map.of(
                            "X-Webhook-Timestamp", Long.toString(now),
                            "X-Webhook-Signature", Signing.signWebhook("whsec", now, body),
                            "X-Webhook-Event-Id", "ev-1");
            statuses.add(receiver.handle(body.getBytes(StandardCharsets.UTF_8), com.oblodai.webhooks.WebhookHeaders.of(headers)));
        }
        assertEquals(List.of(200, 200, 400), statuses);
        assertEquals(
                401,
                receiver.handle(
                        "{}".getBytes(StandardCharsets.UTF_8),
                        com.oblodai.webhooks.WebhookHeaders.of(Map.of("X-Webhook-Timestamp", "1", "X-Webhook-Signature", "00"))));
    }

    /**
     * Ordering is per object, and a conversion's object is its {@code id}: conversion B arriving
     * after conversion A with a lower sequence is B's first state and is applied; an older state of
     * A arriving late is dropped.
     */
    @Test
    void theWebhookReceiverOrdersEachConversionOnItsOwn() {
        WebhookReceiver receiver = new WebhookReceiver("whsec", null);
        long now = System.currentTimeMillis() / 1000;
        List<Boolean> applied = new ArrayList<>();
        int n = 0;
        for (String body :
                List.of(
                        "{\"type\":\"conversion\",\"id\":\"A\",\"status\":\"completed\",\"sequence\":5}",
                        "{\"type\":\"conversion\",\"id\":\"B\",\"status\":\"completed\",\"sequence\":3}",
                        "{\"type\":\"conversion\",\"id\":\"A\",\"status\":\"refunded\",\"sequence\":4}")) {
            Map<String, String> headers =
                    Map.of(
                            "X-Webhook-Timestamp", Long.toString(now),
                            "X-Webhook-Signature", Signing.signWebhook("whsec", now, body),
                            "X-Webhook-Event-Id", "ev-" + n++);
            int before = receiver.appliedCount();
            assertEquals(200, receiver.handle(body.getBytes(StandardCharsets.UTF_8), com.oblodai.webhooks.WebhookHeaders.of(headers)));
            applied.add(receiver.appliedCount() > before);
        }
        assertEquals(List.of(true, true, false), applied);
    }
}
