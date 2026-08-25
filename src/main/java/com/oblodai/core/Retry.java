package com.oblodai.core;

import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import java.util.concurrent.ThreadLocalRandom;

/** The decisions of {@link RetryOptions}, as two pure functions. */
public final class Retry {

    private Retry() {}

    /**
     * @param error the failure of the attempt that just finished
     * @param attempt 0 for the first retry decision (after attempt #1 failed)
     * @param safeToRepeat true when re-sending cannot duplicate a side effect
     * @param options the policy
     * @return whether to make another attempt
     */
    public static boolean shouldRetry(
            Throwable error, int attempt, boolean safeToRepeat, RetryOptions options) {
        if (attempt >= options.maxRetries()) return false;
        if (!(error instanceof OblodaiException err)) return false;
        if (!err.retryable()) return false;
        // No response at all, or an answer from something in front of the gateway: the gateway may
        // have done the work, so only repeat what cannot duplicate an effect.
        if (err instanceof TransportException) return safeToRepeat;
        if (err.synthetic()) return safeToRepeat;
        return true;
    }

    /** Delay before the next attempt, in milliseconds. */
    public static long delayMs(Throwable error, int attempt, RetryOptions options) {
        return delayMs(error, attempt, options, ThreadLocalRandom.current().nextDouble());
    }

    /**
     * Delay before the next attempt with an injected random draw, so tests are deterministic.
     *
     * @param error the failure of the attempt that just finished
     * @param attempt 0 for the first retry
     * @param options the policy
     * @param random a draw in [0, 1)
     * @return milliseconds to wait
     */
    public static long delayMs(Throwable error, int attempt, RetryOptions options, double random) {
        if (error instanceof OblodaiException err
                && err.retryAfter() != null
                && err.retryAfter() > 0) {
            return Math.min(err.retryAfter() * 1000L, options.maxRetryAfterMs());
        }
        long exponential = Math.min(options.maxDelayMs(), options.baseDelayMs() * (1L << attempt));
        // Full jitter with a floor, so a burst of retries never lands in the same instant.
        return Math.max(exponential / 4, (long) (random * exponential));
    }
}
