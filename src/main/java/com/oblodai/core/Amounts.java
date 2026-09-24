package com.oblodai.core;

import com.oblodai.errors.ConfigException;
import com.oblodai.generated.Facts;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Amounts are decimal strings on the wire and {@link BigDecimal} in Java; a {@code double} or
 * {@code float} never is one. The generated models refuse a floating-point number where an amount
 * belongs ({@link #floatAmount}), and the transport walks every request body before it is signed
 * ({@link #prepareBody}) so a float slipped in through {@code putExtra} fails the same way, before
 * anything reaches the network.
 */
public final class Amounts {

    /** The error code for a floating-point amount. */
    public static final String FLOAT_AMOUNT = "sdk.float_amount";

    /**
     * Request fields the contract types as a JSON {@code number}, so they are not money (a
     * tolerance in percent): {@link Facts#NON_MONEY_NUMBERS}, generated from the contract. A float
     * anywhere else in a body is an amount losing precision.
     */
    public static final Set<String> NON_MONEY_NUMBERS = Facts.NON_MONEY_NUMBERS;

    private Amounts() {}

    /**
     * The error for a floating-point number where an amount belongs.
     *
     * @param value the {@code double} or {@code float}
     * @return a {@link ConfigException} with code {@value #FLOAT_AMOUNT}
     */
    public static ConfigException floatAmount(Object value) {
        return floatAmount(value, null);
    }

    private static ConfigException floatAmount(Object value, String path) {
        return new ConfigException(
                FLOAT_AMOUNT,
                "amount passed as a floating-point number ("
                        + value
                        + "); pass a decimal string \""
                        + value
                        + "\" or new BigDecimal(\""
                        + value
                        + "\") - binary floating point loses precision in money",
                path == null ? "amount" : path);
    }

    /**
     * The request body as it goes to the wire: a {@link BigDecimal} becomes its plain decimal
     * string, and a {@code double}/{@code float} outside {@link #NON_MONEY_NUMBERS} is refused.
     *
     * @param body a JSON tree (maps, lists, strings, numbers, booleans, nulls)
     * @return a copy fit for serialization
     * @throws ConfigException {@value #FLOAT_AMOUNT} for a floating-point amount
     */
    public static Object prepareBody(Object body) {
        return prepare(body, "");
    }

    private static Object prepare(Object value, String path) {
        if (value instanceof Double || value instanceof Float) {
            throw floatAmount(value, path.isEmpty() ? "body" : path);
        }
        if (value instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object item = e.getValue();
                if (NON_MONEY_NUMBERS.contains(key) && item instanceof Number) {
                    out.put(key, item);
                    continue;
                }
                out.put(key, prepare(item, path.isEmpty() ? key : path + "." + key));
            }
            return out;
        }
        if (value instanceof Collection<?> list) {
            List<Object> out = new java.util.ArrayList<>(list.size());
            int i = 0;
            for (Object item : list) {
                out.add(prepare(item, path + "[" + i++ + "]"));
            }
            return out;
        }
        return value;
    }
}
