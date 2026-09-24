package com.oblodai.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * How the transport waits between attempts. The default schedules on a timer without holding a
 * thread; a test injects one that records the pause and returns at once.
 */
@FunctionalInterface
public interface Sleeper {

    /** The default: a timer, no thread blocked. */
    Sleeper DEFAULT =
            millis ->
                    CompletableFuture.runAsync(
                            () -> {}, CompletableFuture.delayedExecutor(millis, TimeUnit.MILLISECONDS));

    /**
     * @param millis how long to wait
     * @return a future completing once that time has passed
     */
    CompletableFuture<Void> sleep(long millis);
}
