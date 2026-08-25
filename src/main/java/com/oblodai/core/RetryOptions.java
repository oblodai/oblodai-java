package com.oblodai.core;

/**
 * Retry policy. Two questions decide every retry:
 *
 * <ol>
 *   <li><b>Can it succeed?</b> — the gateway's {@code retryable} flag (authoritative when the
 *       gateway wrote the envelope), or a transient status on an answer that carries no envelope.
 *   <li><b>Is repeating safe?</b> — only for read-only routes and for writes the gateway
 *       deduplicates by {@code Idempotency-Key}. A write the gateway does not deduplicate is never
 *       re-sent once it MAY have reached the gateway: a transport error or a proxy 503 after the
 *       request left the socket could mean the payout already happened.
 * </ol>
 *
 * <p>An enveloped error on an unsafe write is still retried when {@code retryable} — the gateway
 * answered, so it did not perform the operation. {@code Retry-After} always wins over the computed
 * backoff; otherwise the delay is exponential with full jitter.
 *
 * @param maxRetries retries after the first attempt; 0 disables retrying
 * @param baseDelayMs delay before the first retry
 * @param maxDelayMs ceiling for a computed (non-{@code Retry-After}) delay
 * @param maxRetryAfterMs ceiling honoured for a server-provided {@code Retry-After}
 */
public record RetryOptions(int maxRetries, long baseDelayMs, long maxDelayMs, long maxRetryAfterMs) {

    /** Two retries, 250 ms base, 4 s ceiling, 30 s cap on {@code Retry-After}. */
    public static final RetryOptions DEFAULT = new RetryOptions(2, 250, 4_000, 30_000);

    /** No retries at all. */
    public static RetryOptions none() {
        return new RetryOptions(0, 250, 4_000, 30_000);
    }

    /** The same policy with a different retry count. */
    public RetryOptions withMaxRetries(int retries) {
        return new RetryOptions(retries, baseDelayMs, maxDelayMs, maxRetryAfterMs);
    }
}
