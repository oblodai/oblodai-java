package com.oblodai.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * The clock used for signing. The gateway rejects timestamps more than {@value
 * Signing#SIGNATURE_SKEW_SECONDS} s from its own time either way, so
 * a host with a drifting clock would get {@code merchant.bad_signature} on every call. The transport
 * learns the server's time from the {@code Date} header of a signature-failure response, re-signs
 * once, and adopts the offset only if that re-signed attempt succeeded (2xx). An offset beyond
 * ±{@value #MAX_CORRECTION_SECONDS} s is never used: a hostile or broken responder could otherwise
 * push every later signature hours into the future (delayed replay).
 *
 * <p>The offset is shared by every call the client makes, so it is held in an {@link AtomicLong} and
 * a call that wants to undo its own correction does so with {@link #revert(long, long)} — a
 * compare-and-set, never a plain write. Two calls racing on a skewed host must not have the loser
 * roll back the winner's correction.
 */
public final class SkewCorrectingClock {

    /**
     * The largest correction ever applied, in seconds (15 minutes): an offset beyond it, or a move of
     * the offset beyond it, is ignored.
     */
    public static final long MAX_CORRECTION_SECONDS = 900L;

    /**
     * Kept for compatibility; equal to {@link #MAX_CORRECTION_SECONDS}.
     *
     * @deprecated use {@link #MAX_CORRECTION_SECONDS}
     */
    @Deprecated
    public static final long MAX_PLAUSIBLE_OFFSET_SECONDS = MAX_CORRECTION_SECONDS;

    private final LongSupplier base;
    private final AtomicLong offsetSeconds = new AtomicLong();

    /** A clock reading the system time. */
    public SkewCorrectingClock() {
        this(() -> System.currentTimeMillis() / 1000L);
    }

    /**
     * A clock reading an injected source of unix seconds (tests).
     *
     * @param base supplier of the local unix time, in seconds
     */
    public SkewCorrectingClock(LongSupplier base) {
        this.base = base;
    }

    /** Current unix time in seconds, including any learned offset. */
    public long now() {
        return now(offsetSeconds.get());
    }

    /**
     * Current unix time in seconds with a specific offset applied, so a caller can record exactly
     * which offset a request was signed with.
     *
     * @param offset the offset to apply, in seconds
     * @return local time plus that offset
     */
    public long now(long offset) {
        return base.getAsLong() + offset;
    }

    /** Server-minus-local offset currently applied, in seconds. */
    public long offset() {
        return offsetSeconds.get();
    }

    /**
     * Applies an offset measured from a server response; one beyond ±{@value
     * #MAX_CORRECTION_SECONDS} s is ignored.
     *
     * @param offsetSeconds server-minus-local offset, in seconds
     */
    public void correct(long offsetSeconds) {
        if (Math.abs(offsetSeconds) > MAX_CORRECTION_SECONDS) return;
        this.offsetSeconds.set(offsetSeconds);
    }

    /**
     * Undoes a correction this caller installed, and only that one.
     *
     * @param installed the offset this caller set
     * @param previous the offset that was in force before it did
     * @return true when the offset was still {@code installed} and has been rolled back; false when
     *     another call has since corrected the clock, whose verdict is newer and is kept
     */
    public boolean revert(long installed, long previous) {
        return offsetSeconds.compareAndSet(installed, previous);
    }

    /** Drops the learned offset. */
    public void reset() {
        this.offsetSeconds.set(0);
    }

    /**
     * Measures the offset from a response {@code Date} header.
     *
     * @param dateHeader value of the HTTP {@code Date} header, or {@code null}
     * @return the server-minus-local offset in seconds, or {@code null} when the header is absent,
     *     unparsable, or so far out that it can only be a broken intermediary
     */
    public Long observeServerDate(String dateHeader) {
        if (dateHeader == null || dateHeader.isBlank()) return null;
        long serverSeconds;
        try {
            ZonedDateTime parsed = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
            serverSeconds = parsed.toEpochSecond();
        } catch (RuntimeException e) {
            return null;
        }
        long offset = serverSeconds - base.getAsLong();
        return Math.abs(offset) > MAX_CORRECTION_SECONDS ? null : offset;
    }

    /** Formats a duration of seconds for log lines. */
    static String humanise(long seconds) {
        return Duration.ofSeconds(seconds).toString();
    }
}
