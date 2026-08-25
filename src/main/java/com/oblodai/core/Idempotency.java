package com.oblodai.core;

import com.oblodai.errors.ConfigException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Idempotency keys. On create-type routes the gateway caches the first response per key for the
 * merchant and replays it on a re-send; a different body under the same key is a 409 {@code
 * idempotency.key_reused}. The SDK generates a key once per logical call and reuses it on every
 * retry, so a timeout never turns into a second payout.
 */
public final class Idempotency {

    /** Longest key the gateway accepts. */
    public static final int MAX_KEY_LENGTH = 255;

    private static final Pattern PRINTABLE_ASCII = Pattern.compile("^[\\x21-\\x7e]+$");

    private Idempotency() {}

    /** A fresh random key. */
    public static String newKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a caller-supplied key before it is signed and sent.
     *
     * @param key the caller's key
     * @throws ConfigException when the key could not survive a header round trip
     */
    public static void assertValid(String key) {
        if (key == null || key.isEmpty()) {
            throw bad("idempotencyKey must be a non-empty string");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw bad("idempotencyKey is too long (max " + MAX_KEY_LENGTH + " characters)");
        }
        // Header values must be visible ASCII: the key is signed verbatim, so a stray control
        // character or surrounding whitespace would silently change the MAC on one side only.
        if (!PRINTABLE_ASCII.matcher(key).matches()) {
            throw bad("idempotencyKey must be printable ASCII without spaces");
        }
    }

    private static ConfigException bad(String message) {
        return new ConfigException(ConfigException.BAD_IDEMPOTENCY_KEY, message, "idempotencyKey");
    }
}
