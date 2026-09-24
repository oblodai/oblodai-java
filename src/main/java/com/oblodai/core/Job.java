package com.oblodai.core;

import com.oblodai.errors.JobTimeoutException;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A long-running operation (a batch, a document export): its {@link #id()}, the create call's
 * answer {@link #result()}, and {@link #waitFor()}, which polls until the status is terminal and
 * returns that last answer. A terminal status is returned, not thrown, so a {@code failed} job is
 * inspected like a finished one.
 *
 * @param <T> the poll answer
 */
public final class Job<T> {

    /** How long {@link #waitFor()} waits by default. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    /** How often {@link #waitFor()} polls by default. */
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(2);

    private final String id;
    private final Object result;
    private final Supplier<T> poll;
    private final Function<T, String> status;
    private final Supplier<FileResult> download;
    private final Sleeper sleeper;

    /**
     * @param id the job's id
     * @param result the create call's answer
     * @param poll one poll
     * @param status the status of a poll answer
     * @param download the finished job's file, or null when the job makes none
     * @param sleeper how to wait between polls
     */
    public Job(
            String id,
            Object result,
            Supplier<T> poll,
            Function<T, String> status,
            Supplier<FileResult> download,
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
    public T poll() {
        return poll.get();
    }

    /**
     * Polls every 2 seconds for up to 5 minutes.
     *
     * @return the answer with a terminal status
     * @throws JobTimeoutException when the job is still running after that
     */
    public T waitFor() {
        return waitFor(DEFAULT_TIMEOUT, DEFAULT_INTERVAL);
    }

    /**
     * Polls every {@code interval} until the status is terminal ({@code com.oblodai.Lro.TERMINAL_STATUSES}).
     *
     * @param timeout how long to wait in total
     * @param interval pause between polls
     * @return the answer with a terminal status
     * @throws JobTimeoutException when {@code timeout} passes first
     */
    public T waitFor(Duration timeout, Duration interval) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            T answer = poll.get();
            String now = status.apply(answer);
            if (JobSupport.TERMINAL.contains(now)) {
                return answer;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw JobSupport.timeout(id, timeout, now);
            }
            Transport.await(sleeper.sleep(Math.min(interval.toMillis(), Duration.ofNanos(remaining).toMillis())));
        }
    }

    /**
     * @return the finished job's file (document jobs only)
     * @throws UnsupportedOperationException when this kind of job makes no file
     */
    public FileResult download() {
        if (download == null) {
            throw new UnsupportedOperationException("job " + id + " produces no file to download");
        }
        return download.get();
    }

    @Override
    public String toString() {
        return "Job(id=" + id + ")";
    }
}
