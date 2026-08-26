package com.oblodai;

import com.oblodai.errors.ConfigException;
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
 *
 * <p>Every helper takes the amount exactly as the API writes it: digits, at most one dot with digits
 * on both sides of it, an optional leading minus, at most {@value #MAX_LENGTH} characters. Anything
 * else — a float, an empty string, {@code "1e9"}, {@code "  25"}, a thousand digits — is a {@code
 * sdk.bad_amount} {@link ConfigException}, never a language-level failure a caller cannot catch
 * alongside the SDK's own errors.
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
     * @throws ConfigException ({@code sdk.bad_amount}) when the text is not a plain decimal
     */
    public static BigDecimal toBigDecimal(String amount) {
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
