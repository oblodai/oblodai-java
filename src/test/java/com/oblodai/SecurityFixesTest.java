package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Calls;
import com.oblodai.core.CallOptions;
import com.oblodai.core.FileResult;
import com.oblodai.core.Redaction;
import com.oblodai.core.RequestInfo;
import com.oblodai.errors.ConfigException;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.generated.models.PayoutLinkCreated;
import com.oblodai.support.Clients;
import com.oblodai.support.MockHttpClient;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression tests of the security fix wave (rulings R3, R6, R8, R10, R11; findings M2, L3, L7). */
class SecurityFixesTest {

    private static final String BALANCE = "{\"balance\":{\"merchant\":[]}}";

    /** M2: a created payout link's toString never prints its claim URL (it embeds the claim token). */
    @Test
    void payoutLinkCreatedToStringRedactsTheClaimUrl() {
        Map<String, Object> wire = new java.util.LinkedHashMap<>();
        wire.put("amount", "10");
        wire.put("claim_token", "CLAIMTOKEN_Xk3f9");
        wire.put("claim_url", "https://pay.oblodai.com/claim/CLAIMTOKEN_Xk3f9");
        wire.put("created_at", "2026-09-27T00:00:00Z");
        wire.put("currency", "USDT");
        wire.put("expires_at", "2026-10-27T00:00:00Z");
        wire.put("fee_bearer", "merchant");
        wire.put("fee_type", "exact");
        wire.put("link_id", "l-1");
        wire.put("network", "tron");
        wire.put("note", "");
        wire.put("passcode_protected", false);
        wire.put("status", "active");
        wire.put("title", "t");
        String printed = PayoutLinkCreated.fromMap(wire).toString();
        assertFalse(printed.contains("CLAIMTOKEN_Xk3f9"), printed);
        assertTrue(printed.contains("claimUrl=[redacted]"), printed);
    }

    /** L3 and R3: the device code and a signed document link never print through toString. */
    @Test
    void theDeviceCodeAndSignedLinksNeverPrint() {
        String printed =
                com.oblodai.generated.models.Wire.describe(
                        "Model",
                        "deviceCode",
                        "DEVICE-SECRET-q3X0",
                        "documentUrl",
                        "https://api.oblodai.com/v1/documents/payout/p1?exp=1&sig=SIGVALUE",
                        "claimUrl",
                        "https://pay.oblodai.com/claim/CLAIMTOKEN_1");
        for (String secret : List.of("DEVICE-SECRET-q3X0", "SIGVALUE", "CLAIMTOKEN_1")) {
            assertFalse(printed.contains(secret), secret + " leaked: " + printed);
        }
    }

    /** R3: hooks never see a claim token in the URL, nor a proxy or passcode header. */
    @Test
    void hooksNeverSeeAClaimTokenOrASecretHeader() {
        List<RequestInfo> seen = new ArrayList<>();
        MockHttpClient http = new MockHttpClient().ok("{}");
        Oblodai oblodai =
                Clients.builder(http, new Clients.RecordingSleeper())
                        .header("Authorization", "Bearer PROXY-TOKEN")
                        .header("X-Api-Key", "PROXY-KEY")
                        .header("X-Claim-Passcode", "PASS-1234")
                        .onRequest(seen::add)
                        .build();
        try {
            oblodai.payoutLinks().getPayoutClaim("CLAIMTOKEN_Xk3f9");
        } catch (RuntimeException ignored) {
            // the answer's shape does not matter here
        }
        assertTrue(http.onlyCall().uri().toString().contains("CLAIMTOKEN_Xk3f9"), "the wire has it");
        String printed = seen.toString();
        assertFalse(seen.isEmpty());
        for (String secret : List.of("CLAIMTOKEN_Xk3f9", "PROXY-TOKEN", "PROXY-KEY", "PASS-1234")) {
            assertFalse(printed.contains(secret), secret + " leaked: " + printed);
        }
        assertTrue(seen.get(0).url().endsWith("/v1/claim/[redacted]"), seen.get(0).url());
    }

    @Test
    void redactUrlHidesBearerPartsAndUserinfo() {
        String out =
                Redaction.redactUrl(
                        "https://u:p@api.test/v1/documents/payout/p1?exp=1&sig=S1&x=keep", null);
        assertEquals("https://api.test/v1/documents/payout/p1?exp=[redacted]&sig=[redacted]&x=keep", out);
        assertEquals(
                "https://api.test/v1/aml/[redacted]",
                Redaction.redactUrl("https://api.test/v1/aml/T0K", "/v1/aml/{token}"));
    }

    /** R6: a hostile Content-Disposition yields a bare, safe name. */
    @Test
    void aHostileFilenameIsReducedToASafeBaseName() {
        assertEquals("passwd", Calls.filename("attachment; filename=\"../../etc/passwd\""));
        assertEquals("x.pdf", Calls.filename("attachment; filename*=UTF-8''..%2F..%2Fx%0A.pdf"));
        assertEquals("a.csv", Calls.filename("attachment; filename=\"C:\\\\tmp\\\\a.csv\""));
        assertNull(Calls.filename("attachment; filename=\"..\""));
        assertNull(Calls.filename("attachment; filename=\".\""));
        assertNull(Calls.filename("attachment; filename=\"dir/\""));
    }

    /** R6: saving never overwrites by default and writes owner-only. */
    @Test
    void savingNeverOverwritesAndIsOwnerOnly(@TempDir Path dir) throws Exception {
        FileResult file = new FileResult("%PDF".getBytes(), "application/pdf", "a.pdf");
        Path existing = dir.resolve("doc.pdf");
        Files.writeString(existing, "keep");
        assertThrows(FileAlreadyExistsException.class, () -> file.writeTo(existing));
        assertEquals("keep", Files.readString(existing));
        Path fresh = file.writeTo(dir.resolve("new.pdf"));
        if (Files.getFileStore(fresh).supportsFileAttributeView("posix")) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(fresh)));
        }
        file.writeTo(existing, true);
        assertEquals("%PDF", Files.readString(existing));
    }

    /** R10: a body over the contract's MaxBody, or a huge-exponent decimal, never leaves. */
    @Test
    void aBodyOverTheContractLimitNeverLeavesTheProcess() {
        MockHttpClient http = new MockHttpClient().ok("{}").ok("{}");
        Oblodai oblodai = Clients.client(http);
        String huge = "9".repeat((int) SigningProtocol.MAX_BODY + 1);
        ConfigException tooBig =
                assertThrows(
                        ConfigException.class,
                        () ->
                                oblodai.transport()
                                        .call(
                                                com.oblodai.generated.Routes.CREATE_PAYMENT,
                                                new CallOptions()
                                                        .body(Map.of("amount", huge, "currency", "USDT"))));
        assertEquals(ConfigException.BODY_TOO_LARGE, tooBig.code());
        ConfigException exponent =
                assertThrows(
                        ConfigException.class,
                        () ->
                                oblodai.transport()
                                        .call(
                                                com.oblodai.generated.Routes.CREATE_PAYMENT,
                                                new CallOptions()
                                                        .body(
                                                                Map.of(
                                                                        "amount",
                                                                        new BigDecimal("1E+999999999"),
                                                                        "currency",
                                                                        "USDT"))));
        assertEquals(ConfigException.BODY_TOO_LARGE, exponent.code());
        assertEquals(0, http.calls().size(), "nothing was sent");
    }

    /** L7: an injected client that follows redirects is refused up front. */
    @Test
    void anInjectedClientThatFollowsRedirectsIsRefused() {
        HttpClient following = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                Oblodai.builder()
                                        .baseUrl("https://api.test")
                                        .httpClient(following)
                                        .environment(Map.of())
                                        .build());
        assertTrue(error.getMessage().contains("NEVER"), error.getMessage());
    }

    /** R11: a page shorter than the limit does not end the walk; offset >= total does. */
    @Test
    void paginationStopsOnlyOnAnEmptyPageOrTheTotal() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(page(2, 0, 5, true))
                        .ok(page(1, 2, 5, false))
                        .ok(page(2, 3, 5, false));
        Oblodai oblodai = Clients.client(http);
        int count = 0;
        for (Object ignored :
                oblodai.sandbox()
                        .listWebhooks(
                                com.oblodai.generated.models.SandboxListWebhooksQuery.builder()
                                        .limit(2L)
                                        .build())) {
            count++;
        }
        assertEquals(5, count);
        assertEquals(3, http.calls().size(), "a short page and has_pages=false did not stop the walk");
    }

    private static String page(int items, int offset, int total, boolean hasPages) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < items; i++) {
            if (i > 0) list.append(',');
            list.append("{\"id\":\"d")
                    .append(offset + i)
                    .append("\",\"url\":\"https://shop.test/hook\",\"event_type\":\"invoice.paid\",")
                    .append("\"attempts\":1,\"last_error\":\"\",\"payload\":{},\"status\":\"delivered\",")
                    .append("\"created_at\":\"2026-09-24T10:00:00Z\",\"updated_at\":\"2026-09-24T10:00:00Z\"}");
        }
        return "{\"items\":["
                + list
                + "],\"paginate\":{\"total\":"
                + total
                + ",\"per_page\":2,\"offset\":"
                + offset
                + ",\"has_pages\":"
                + hasPages
                + "}}";
    }
}
