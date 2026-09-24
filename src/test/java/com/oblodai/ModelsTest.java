package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Json;
import com.oblodai.core.Transport;
import com.oblodai.generated.models.PaymentStatus;
import com.oblodai.generated.models.PaymentView;
import com.oblodai.generated.models.RegisterWebhookResult;
import com.oblodai.generated.models.SetWebhookActiveRequest;
import com.oblodai.support.Fixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec §3.4: answers are objects; unknown fields and enum values survive; toString hides secrets. */
class ModelsTest {

    private static Object tree(String json) throws Exception {
        return Transport.tree(Json.mapper().readTree(json));
    }

    @Test
    void anUnknownFieldAndAnUnknownEnumValueParseAndRoundTrip() throws Exception {
        String json =
                Fixtures.payment("u", "teleported", "25")
                        .replace("\"paid_at\":null}", "\"paid_at\":null,\"brand_new\":{\"nested\":true}}");
        PaymentView view = PaymentView.fromJson(tree(json));

        assertEquals("teleported", view.status().value());
        assertFalse(view.status().isKnown());
        assertEquals(PaymentStatus.of("teleported"), view.status());
        assertEquals(Map.of("nested", true), view.extra().get("brand_new"));
        assertEquals(Map.of("nested", true), view.toMap().get("brand_new"), "sent back as it came");
        assertEquals("teleported", view.toMap().get("status"));
    }

    @Test
    void toStringIsShortAndHidesSecrets() throws Exception {
        String json = "{\"endpoint_id\":\"w1\",\"url\":\"https://shop.test/hook\",\"secret\":\"whsec_live\"}";
        RegisterWebhookResult result = RegisterWebhookResult.fromJson(tree(json));
        String shown = result.toString();
        assertFalse(shown.contains("whsec_live"), shown);
        assertTrue(shown.contains("secret=[redacted]"), shown);
        assertTrue(shown.contains("endpointId=\"w1\""), shown);
        assertTrue(PaymentView.fromJson(tree(Fixtures.payment("u"))).toString().length() <= 1024);
    }

    @Test
    void anExplicitNullIsSentOnlyWhenSet() {
        SetWebhookActiveRequest unset = SetWebhookActiveRequest.builder().build();
        assertFalse(unset.toMap().containsKey("active"));
    }
}
