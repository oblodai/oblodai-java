package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.contract.PaymentStatus;
import com.oblodai.contract.PayoutStatus;
import com.oblodai.errors.ConfigException;
import com.oblodai.support.MockHttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Client options, their environment fallbacks, and the two helper families. */
class ConfigTest {

    @Test
    void readsCredentialsAndBaseUrlFromTheEnvironment() {
        MockHttpClient http = new MockHttpClient().ok("{\"balance\":{\"merchant\":[]}}");
        Oblodai oblodai =
                Oblodai.builder()
                        .httpClient(http)
                        .environment(
                                Map.of(
                                        "OBLODAI_PUBLIC_ID", "pk",
                                        "OBLODAI_SECRET", "s",
                                        "OBLODAI_BASE_URL", "https://x.test/"))
                        .build();
        oblodai.account().balance();

        assertEquals("https://x.test/v1/balance", http.onlyCall().uri().toString());
        assertEquals("pk", http.onlyCall().header("x-public-id"));
    }

    @Test
    void refusesPlainHttpExceptForLoopbackOrWhenAllowed() {
        assertTrue(
                assertThrows(
                                ConfigException.class,
                                () ->
                                        Oblodai.builder()
                                                .baseUrl("http://api.oblodai.com")
                                                .environment(Map.of())
                                                .build())
                        .getMessage()
                        .contains("https"));

        Oblodai.builder().baseUrl("http://localhost:8093").environment(Map.of()).build();
        Oblodai.builder().baseUrl("http://127.0.0.1:8095").environment(Map.of()).build();
        Oblodai.builder().baseUrl("http://[::1]:8093").environment(Map.of()).build();
        Oblodai.builder()
                .baseUrl("http://10.0.0.1")
                .allowInsecureBaseUrl(true)
                .environment(Map.of())
                .build();
    }

    @Test
    void refusesHalfAKeyPair() {
        assertTrue(
                assertThrows(
                                ConfigException.class,
                                () ->
                                        Oblodai.builder()
                                                .publicId("pk")
                                                .baseUrl("https://api.test")
                                                .environment(Map.of())
                                                .build())
                        .getMessage()
                        .contains("together"));
        assertThrows(
                ConfigException.class,
                () ->
                        Oblodai.builder()
                                .publicId("pk")
                                .secret("s")
                                .payoutKey("wk", null)
                                .baseUrl("https://api.test")
                                .environment(Map.of())
                                .build());
    }

    @Test
    void moneyHelpersWorkAtArbitraryPrecision() {
        assertEquals("0.3", Money.add("0.1", "0.2"));
        assertEquals("10.500000", Money.add("10.000000", "0.5"));
        assertEquals("-0.000001", Money.subtract("1", "1.000001"));
        assertEquals(0, Money.compare("25", "25.000000"));
        assertEquals(1, Money.compare("0.000000000000000001", "0"));
        assertTrue(Money.isZero("0.000000"));
        assertFalse(Money.isPositive("0"));
        assertEquals("1.5", Money.of(Money.toBigDecimal("1.5")));
    }

    @Test
    void moneyHelpersRefuseAnythingThatIsNotAPlainDecimalAsAnSdkError() {
        // Not an IllegalArgumentException: a caller catching OblodaiException must catch this too.
        for (String notAnAmount :
                new String[] {
                    "1e3", "", " 25", "25 ", "25.", ".5", "--1", "1.2.3", "0x10", "25,00", "NaN", null
                }) {
            ConfigException failure =
                    assertThrows(
                            ConfigException.class,
                            () -> Money.add(notAnAmount, "1"),
                            "accepted " + notAnAmount);
            assertEquals(ConfigException.BAD_AMOUNT, failure.code());
        }
        assertThrows(ConfigException.class, () -> Money.compare("1", "one"));
        assertThrows(ConfigException.class, () -> Money.isZero("0".repeat(Money.MAX_LENGTH + 1)));
        assertEquals(0, Money.compare("0".repeat(Money.MAX_LENGTH), "0"), "64 digits is still fine");
    }

    @Test
    void statusHelpersFollowTheGatewayVocabulary() {
        assertTrue(Statuses.isPaymentPaid(PaymentStatus.PAID_OVER));
        assertFalse(Statuses.isPaymentPaid(PaymentStatus.WRONG_AMOUNT));
        assertTrue(Statuses.isPaymentUnderpaid(PaymentStatus.WRONG_AMOUNT));
        assertFalse(Statuses.isPaymentFinal(PaymentStatus.CONFIRM_CHECK));
        assertTrue(Statuses.isPaymentFinal(PaymentStatus.EXPIRED));
        assertFalse(Statuses.isPayoutFinal(PayoutStatus.SENT));
        assertTrue(Statuses.isPayoutFinal(PayoutStatus.CONFIRMED));
        assertTrue(Statuses.isPayoutSucceeded(PayoutStatus.CONFIRMED));
    }

    @Test
    void anUnknownVocabularyValueIsKeptExactlyAsItArrived() {
        assertEquals(PaymentStatus.PAID, PaymentStatus.of("paid"));
        assertSame(PaymentStatus.PAID, PaymentStatus.of("paid"), "known values are interned");
        assertTrue(PaymentStatus.PAID.isKnown());
        assertEquals("paid", PaymentStatus.PAID.wire());

        // The value the gateway actually sent must survive: a client that cannot log or report it
        // has nothing to take to support.
        PaymentStatus grown = PaymentStatus.of("teleported");
        assertFalse(grown.isKnown());
        assertEquals("teleported", grown.wire());
        assertEquals("teleported", grown.toString());
        assertEquals(grown, PaymentStatus.of("teleported"), "equal by value");
        assertFalse(PaymentStatus.PAID.equals(grown));
        assertTrue(PaymentStatus.VALUES.contains(PaymentStatus.PAID));
        assertFalse(PaymentStatus.VALUES.contains(grown));
        assertNull(PaymentStatus.of(null));
    }

    @Test
    void aStatusTheGatewayGrewSurvivesTheWholeDecode() {
        MockHttpClient http =
                new MockHttpClient().ok("{\"uuid\":\"u1\",\"status\":\"teleported\"}");
        PaymentStatus status =
                Oblodai.builder()
                        .publicId("pk")
                        .secret("s")
                        .baseUrl("https://api.test")
                        .httpClient(http)
                        .environment(Map.of())
                        .build()
                        .payments()
                        .info("u1")
                        .status();

        assertFalse(status.isKnown());
        assertEquals("teleported", status.wire(), "the model keeps what the gateway said");
    }
}
