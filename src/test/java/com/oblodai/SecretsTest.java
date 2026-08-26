package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Credentials;
import com.oblodai.core.Json;
import com.oblodai.models.ApiKeyPair;
import com.oblodai.models.PayoutLink;
import com.oblodai.models.WebhookEndpoint;
import com.oblodai.models.WebhookSecretRotated;
import org.junit.jupiter.api.Test;

/**
 * Values the gateway shows once — a key secret, a webhook signing secret, a cheque's claim token and
 * passcode — stay readable through their accessor and unreadable everywhere a value goes by accident:
 * {@code toString()} (log lines, error dumps, debuggers) and JSON serialisation (structured logs).
 */
class SecretsTest {

    private static final ObjectMapper MAPPER = Json.newMapper();

    private static String json(Object value) throws Exception {
        return MAPPER.writeValueAsString(value);
    }

    @Test
    void aWebhookSecretIsReadableButNeverRendered() throws Exception {
        WebhookEndpoint endpoint = new WebhookEndpoint("we_1", "https://shop.example/hook", "whsec_live");

        assertEquals("whsec_live", endpoint.secret(), "the program can still read it");
        assertFalse(endpoint.toString().contains("whsec_live"), endpoint.toString());
        assertTrue(endpoint.toString().contains("[redacted]"));
        assertFalse(json(endpoint).contains("whsec_live"), json(endpoint));
        assertTrue(json(endpoint).contains("[redacted]"));
        assertTrue(json(endpoint).contains("we_1"), "the rest of the object is untouched");
    }

    @Test
    void soIsARotatedSecretAndAMintedKeyPair() throws Exception {
        WebhookSecretRotated rotated =
                new WebhookSecretRotated("we_1", "https://shop.example/hook", "whsec_new", "2026-02-01T00:00:00Z");
        assertEquals("whsec_new", rotated.secret());
        assertFalse(rotated.toString().contains("whsec_new"));
        assertFalse(json(rotated).contains("whsec_new"));
        assertTrue(rotated.toString().contains("2026-02-01T00:00:00Z"), "the overlap window still shows");

        ApiKeyPair pair = new ApiKeyPair("pk_live_1", "sk_live_zzz", "api");
        assertEquals("sk_live_zzz", pair.secret());
        assertFalse(pair.toString().contains("sk_live_zzz"));
        assertFalse(json(pair).contains("sk_live_zzz"));
        assertTrue(pair.toString().contains("pk_live_1"), "the public id is not a secret");
    }

    @Test
    void aChequesClaimTokenAndPasscodeAreBothProtected() throws Exception {
        PayoutLink link =
                new PayoutLink(
                        "pl_1", null, "25", "USDT", null, null, null, null, null, "ref-1", "Title", "Note",
                        true, "2026-03-01T00:00:00Z", "2026-02-01T00:00:00Z", "clm_secret_token",
                        "https://pay.oblodai.com/claim/clm_secret_token", null, null, null, null, "4821");

        assertEquals("clm_secret_token", link.claimToken());
        assertEquals("4821", link.passcode());
        assertFalse(link.toString().contains("clm_secret_token"), link.toString());
        assertFalse(link.toString().contains("4821"), link.toString());
        assertTrue(link.toString().contains("pl_1"));
        assertFalse(json(link).contains("clm_secret_token"));
        assertFalse(json(link).contains("\"4821\""));
        // The ready-made claim URL embeds the token, so it is protected the same way.
        assertEquals("https://pay.oblodai.com/claim/clm_secret_token", link.claimUrl());
        assertFalse(link.toString().contains("pay.oblodai.com"));
    }

    @Test
    void theCredentialsTheClientHoldsRenderTheSameWay() {
        Credentials credentials = new Credentials("pk_live_1", "sk_live_zzz");
        assertFalse(credentials.toString().contains("sk_live_zzz"));
        assertTrue(credentials.toString().contains("pk_live_1"));
    }
}
