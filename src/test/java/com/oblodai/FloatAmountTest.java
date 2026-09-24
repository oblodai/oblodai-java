package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Amounts;
import com.oblodai.generated.Facts;
import com.oblodai.errors.ConfigException;
import com.oblodai.generated.models.PaymentRequest;
import com.oblodai.support.Clients;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Spec §3.3: amounts are {@link BigDecimal} or decimal strings; a {@code double} is refused by the
 * signature, and one that slips in anyway is {@code sdk.float_amount} before anything is sent.
 */
class FloatAmountTest {

    @Test
    void theBuilderTakesABigDecimalOrADecimalStringOnly() throws Exception {
        for (var method : PaymentRequest.Builder.class.getMethods()) {
            if (method.getName().equals("amount")) {
                Class<?> type = method.getParameterTypes()[0];
                assertTrue(type == BigDecimal.class || type == String.class, "amount(" + type + ")");
            }
        }
        MockHttpClient http = new MockHttpClient().ok(Fixtures.payment("u"));
        Clients.client(http)
                .payments()
                .create(PaymentRequest.builder().amount(new BigDecimal("25.10")).currency("USDT").build());
        assertTrue(http.onlyCall().body().contains("\"amount\":\"25.10\""), "the wire keeps the scale");
    }

    @Test
    void aDoubleSmuggledInAsAnExtraFieldFailsBeforeTheNetwork() {
        MockHttpClient http = new MockHttpClient();
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () ->
                                Clients.client(http)
                                        .payments()
                                        .create(
                                                PaymentRequest.builder()
                                                        .amount("1")
                                                        .currency("USDT")
                                                        .putExtra("tip", 0.1)
                                                        .build()));
        assertEquals(Amounts.FLOAT_AMOUNT, error.code());
        assertEquals("tip", error.field());
        assertTrue(http.calls().isEmpty(), "nothing was sent");
    }

    @Test
    void aDoubleInAParsedRequestIsTheSameError() {
        ConfigException error =
                assertThrows(
                        ConfigException.class,
                        () -> PaymentRequest.fromMap(Map.of("amount", 25.5, "currency", "USDT")));
        assertEquals(Amounts.FLOAT_AMOUNT, error.code());
    }

    @Test
    void aToleranceInPercentIsNotMoney() {
        MockHttpClient http = new MockHttpClient().ok(Fixtures.payment("u"));
        Clients.client(http)
                .payments()
                .create(
                        PaymentRequest.builder()
                                .amount("1")
                                .currency("USDT")
                                .accuracyPaymentPercent(1.5)
                                .build());
        assertTrue(http.onlyCall().body().contains("\"accuracy_payment_percent\":1.5"), http.onlyCall().body());
    }

    @Test
    void theNonMoneyNumbersAreTheContractsNumberFieldsOfRequests() throws IOException {
        assertEquals(Facts.NON_MONEY_NUMBERS, Amounts.NON_MONEY_NUMBERS, "the runtime keeps no list of its own");
        Set<String> doubles = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of("src/main/java/com/oblodai/generated/models"))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                Class<?> type;
                try {
                    type = Class.forName("com.oblodai.generated.models." + name);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
                for (Field field : type.getDeclaredFields()) {
                    if (field.getType() == Double.class && !Modifier.isStatic(field.getModifiers())) {
                        doubles.add(field.getName().replaceAll("([A-Z])", "_$1").toLowerCase());
                    }
                }
            }
        }
        // Request fields only: a number field of a response is not in the list, and needs not be.
        assertTrue(doubles.containsAll(Amounts.NON_MONEY_NUMBERS), doubles + " vs " + Amounts.NON_MONEY_NUMBERS);
    }
}
