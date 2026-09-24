package com.oblodai;

import com.oblodai.errors.ConfigException;
import com.oblodai.core.Amounts;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Exact arithmetic on amounts: the decimal strings the API writes and the {@link BigDecimal}s the
 * models hold.
 *
 * <p>USDT has 6 decimals, BTC 8 and ETH 18, so a {@code double} cannot hold them. These helpers
 * take a {@link BigDecimal}, an integral number or a decimal string and compute at the full
 * precision of their inputs.
 *
 * <pre>{@code
 * Money.add("10.000000", "0.5");                 // 10.500000
 * Money.compare(invoice.amount(), "25.000000");  // 0 for 25
 * Money.isZero("0.000000");                      // true
 * }</pre>
 *
 * <p>A string must be an amount exactly as the API writes it: digits, at most one dot with digits
 * on both sides, an optional leading minus, at most {@value #MAX_LENGTH} characters; anything else
 * ({@code "1e9"}, {@code "  25"}, an empty string) is {@code sdk.bad_amount}. A {@code double} or
 * {@code float} is {@code sdk.float_amount}. Both are {@link ConfigException}s.
 */
public final class Money {

    /** Longest amount any asset the gateway settles can need, with room to spare. */
    public static final int MAX_LENGTH = 64;

    private static final Pattern DECIMAL = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    private Money() {}

    /**
     * Sum of two amounts, at the scale of the more precise one.
     *
     * @param a left amount
     * @param b right amount
     * @return their sum
     */
    public static BigDecimal add(Object a, Object b) {
        return toBigDecimal(a).add(toBigDecimal(b));
    }

    /**
     * Difference of two amounts, at the scale of the more precise one.
     *
     * @param a left amount
     * @param b right amount
     * @return {@code a - b}
     */
    public static BigDecimal subtract(Object a, Object b) {
        return toBigDecimal(a).subtract(toBigDecimal(b));
    }

    /**
     * Compares two amounts by value, ignoring trailing zeros.
     *
     * @param a left amount
     * @param b right amount
     * @return -1, 0 or 1
     */
    public static int compare(Object a, Object b) {
        return Integer.signum(toBigDecimal(a).compareTo(toBigDecimal(b)));
    }

    /**
     * @param a left amount
     * @param b right amount
     * @return whether the two amounts are the same value ({@code "25"} equals {@code "25.000000"})
     */
    public static boolean equalAmounts(Object a, Object b) {
        return compare(a, b) == 0;
    }

    /**
     * @param amount an amount
     * @return whether it is zero at any scale
     */
    public static boolean isZero(Object amount) {
        return toBigDecimal(amount).signum() == 0;
    }

    /**
     * @param amount an amount
     * @return whether it is greater than zero
     */
    public static boolean isPositive(Object amount) {
        return toBigDecimal(amount).signum() > 0;
    }

    /**
     * The amount as a {@link BigDecimal}.
     *
     * @param amount a {@link BigDecimal}, an integral number or a decimal string
     * @return the same value, exactly
     * @throws ConfigException {@code sdk.bad_amount} for text that is not a plain decimal or a
     *     value of another type, {@code sdk.float_amount} for a {@code double} or {@code float}
     */
    public static BigDecimal toBigDecimal(Object amount) {
        if (amount instanceof BigDecimal d) {
            return d;
        }
        if (amount instanceof Double || amount instanceof Float) {
            throw Amounts.floatAmount(amount);
        }
        if (amount instanceof Long || amount instanceof Integer || amount instanceof Short || amount instanceof Byte) {
            return BigDecimal.valueOf(((Number) amount).longValue());
        }
        if (amount instanceof BigInteger b) {
            return new BigDecimal(b);
        }
        if (amount instanceof String text) {
            return parse(text);
        }
        throw notAnAmount(amount == null ? null : amount.getClass().getSimpleName() + " " + amount);
    }

    private static BigDecimal parse(String amount) {
        // Length first: the check must cost the same for a hostile input as for a real one.
        if (amount == null || amount.isEmpty() || amount.length() > MAX_LENGTH) {
            throw notAnAmount(amount);
        }
        if (!DECIMAL.matcher(amount).matches()) throw notAnAmount(amount);
        return new BigDecimal(amount);
    }

    private static ConfigException notAnAmount(String amount) {
        String shown =
                amount == null
                        ? "null"
                        : '"'
                                + (amount.length() > MAX_LENGTH
                                        ? amount.substring(0, MAX_LENGTH) + "…"
                                        : amount)
                                + '"';
        return new ConfigException(
                ConfigException.BAD_AMOUNT,
                "not a decimal amount: "
                        + shown
                        + " — amounts are digits with at most one dot, as the API writes them",
                "amount");
    }

    /**
     * Renders a {@link BigDecimal} the way the API expects it — plain, never in scientific notation.
     *
     * @param amount the value
     * @return the amount as a decimal string
     */
    public static String of(BigDecimal amount) {
        return amount.toPlainString();
    }
}
