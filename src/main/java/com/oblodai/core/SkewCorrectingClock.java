package com.oblodai.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.LongSupplier;

/**
 * The clock used for signing. The gateway rejects timestamps more than ±300 s from its own time, so
 * a host with a drifting clock would get {@code merchant.bad_signature} on every call. The transport
 * learns the server's time from the {@code Date} header of a signature-failure response, re-signs
 * once, and keeps the offset only if that re-signed attempt got past authentication.
 */
public final class SkewCorrectingClock {

    /** Offsets beyond this are implausible drift and are ignored (a broken proxy {@code Date}). */
    public static final long MAX_PLAUSIBLE_OFFSET_SECONDS = 24 * 3600L;

    private final LongSupplier base;
    private volatile long offsetSeconds;

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
        return base.getAsLong() + offsetSeconds;
    }

    /** Server-minus-local offset currently applied, in seconds. */
    public long offset() {
        return offsetSeconds;
    }

    /** Applies an offset measured from a server response. */
    public void correct(long offsetSeconds) {
        this.offsetSeconds = offsetSeconds;
    }

    /** Drops the learned offset. */
    public void reset() {
        this.offsetSeconds = 0;
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
        return Math.abs(offset) > MAX_PLAUSIBLE_OFFSET_SECONDS ? null : offset;
    }

    /** Formats a duration of seconds for log lines. */
    static String humanise(long seconds) {
        return Duration.ofSeconds(seconds).toString();
    }
}
