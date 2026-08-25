package com.oblodai;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Exact arithmetic on the decimal strings the API uses for money.
 *
 * <p>Amounts cross the wire as strings and stay strings in this SDK: USDT has 6 decimals, BTC 8 and
 * ETH 18, so a {@code double} cannot hold them and a rounded {@code BigDecimal} would quietly change
 * what a merchant is paid. These helpers add, subtract and compare at the full precision of their
 * inputs; {@link #toBigDecimal(String)} is there for the moment you deliberately want one.
 *
 * <pre>{@code
 * Money.add("10.000000", "0.5");      // "10.500000"
 * Money.compare("25", "25.000000");   // 0
 * Money.isZero("0.000000");           // true
 * }</pre>
 */
public final class Money {

    private static final Pattern DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private Money() {}

    /**
     * Sum of two amounts, at the scale of the more precise one.
     *
     * @param a left amount
     * @param b right amount
     * @return their sum as a decimal string
     */
    public static String add(String a, String b) {
        return toBigDecimal(a).add(toBigDecimal(b)).toPlainString();
    }

    /**
     * Difference of two amounts, at the scale of the more precise one.
     *
     * @param a left amount
     * @param b right amount
     * @return {@code a - b} as a decimal string
     */
    public static String subtract(String a, String b) {
        return toBigDecimal(a).subtract(toBigDecimal(b)).toPlainString();
    }

    /**
     * Compares two amounts by value, ignoring trailing zeros.
     *
     * @param a left amount
     * @param b right amount
     * @return -1, 0 or 1
     */
    public static int compare(String a, String b) {
        return Integer.signum(toBigDecimal(a).compareTo(toBigDecimal(b)));
    }

    /**
     * @param a left amount
     * @param b right amount
     * @return whether the two amounts are the same value ({@code "25"} equals {@code "25.000000"})
     */
    public static boolean equalAmounts(String a, String b) {
        return compare(a, b) == 0;
    }

    /**
     * @param amount an amount
     * @return whether it is zero at any scale
     */
    public static boolean isZero(String amount) {
        return toBigDecimal(amount).signum() == 0;
    }

    /**
     * @param amount an amount
     * @return whether it is greater than zero
     */
    public static boolean isPositive(String amount) {
        return toBigDecimal(amount).signum() > 0;
    }

    /**
     * The amount as a {@link BigDecimal}, for arithmetic the helpers here do not cover.
     *
     * @param amount a decimal string as the API renders it
     * @return the same value, exactly
     * @throws IllegalArgumentException when the text is not a plain decimal
     */
    public static BigDecimal toBigDecimal(String amount) {
        if (amount == null || !DECIMAL.matcher(amount).matches()) {
            throw new IllegalArgumentException("not a decimal amount: \"" + amount + "\"");
        }
        return new BigDecimal(amount);
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
