package com.oblodai.core;

import com.oblodai.errors.JobTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The non-blocking twin of {@link Job}: {@link #waitFor()} completes with the answer whose status
 * is terminal, without holding a thread between polls.
 *
 * @param <T> the poll answer
 */
public final class AsyncJob<T> {

    private final String id;
    private final Object result;
    private final Supplier<CompletableFuture<T>> poll;
    private final Function<T, String> status;
    private final Supplier<CompletableFuture<FileResult>> download;
    private final Sleeper sleeper;

    /**
     * @param id the job's id
     * @param result the create call's answer
     * @param poll one poll
     * @param status the status of a poll answer
     * @param download the finished job's file, or null when the job makes none
     * @param sleeper how to wait between polls
     */
    public AsyncJob(
            String id,
            Object result,
            Supplier<CompletableFuture<T>> poll,
            Function<T, String> status,
            Supplier<CompletableFuture<FileResult>> download,
            Sleeper sleeper) {
        this.id = id;
        this.result = result;
        this.poll = poll;
        this.status = status;
        this.download = download;
        this.sleeper = sleeper;
    }

    /** @return the job's id */
    public String id() {
        return id;
    }

    /** @return the create call's answer, as the create method returned it */
    public Object result() {
        return result;
    }

    /** @return the job's current state: one poll */
    public CompletableFuture<T> poll() {
        return poll.get();
    }

    /** @return the answer with a terminal status, polling every 2 seconds for up to 5 minutes */
    public CompletableFuture<T> waitFor() {
        return waitFor(Job.DEFAULT_TIMEOUT, Job.DEFAULT_INTERVAL);
    }

    /**
     * @param timeout how long to wait in total
     * @param interval pause between polls
     * @return the answer with a terminal status; fails with {@link JobTimeoutException} when
     *     {@code timeout} passes first
     */
    public CompletableFuture<T> waitFor(Duration timeout, Duration interval) {
        long deadline = System.nanoTime() + timeout.toNanos();
        return step(deadline, timeout, interval);
    }

    private CompletableFuture<T> step(long deadline, Duration timeout, Duration interval) {
        return poll.get()
                .thenCompose(
                        answer -> {
                            String now = status.apply(answer);
                            if (JobSupport.TERMINAL.contains(now)) {
                                return CompletableFuture.completedFuture(answer);
                            }
                            long remaining = deadline - System.nanoTime();
                            if (remaining <= 0) {
                                return CompletableFuture.failedFuture(JobSupport.timeout(id, timeout, now));
                            }
                            long pause = Math.min(interval.toMillis(), Duration.ofNanos(remaining).toMillis());
                            return sleeper.sleep(pause).thenCompose(ignored -> step(deadline, timeout, interval));
                        });
    }

    /** @return the finished job's file (document jobs only) */
    public CompletableFuture<FileResult> download() {
        if (download == null) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("job " + id + " produces no file to download"));
        }
        return download.get();
    }

    @Override
    public String toString() {
        return "AsyncJob(id=" + id + ")";
    }
}
