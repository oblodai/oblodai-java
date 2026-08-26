package com.oblodai.core;

import com.oblodai.contract.RouteSpec;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The state of one logical call across its attempts: what to send, whether re-sending it is safe,
 * when the whole call must be over, what the clock was corrected to, and which HTTP exchange is in
 * flight right now — so cancelling the future the caller holds actually aborts the socket.
 */
final class Exchange {

    final RouteSpec route;
    final CallOptions options;
    final byte[] body;
    final String idempotencyKey;
    final boolean safeToRepeat;
    final long deadlineAt;

    /** Attempt number: 0 while the first attempt is in flight. */
    int attempt;

    /** Whether this call has already re-signed once for clock skew. */
    boolean skewTried;

    /** The offset the current attempt was signed with. */
    long signedOffset;

    /** The offset this call installed when it corrected the clock. */
    long skewInstalled;

    /** The offset that was in force before this call corrected the clock. */
    long skewBefore;

    private final AtomicReference<CompletableFuture<?>> inFlight = new AtomicReference<>();
    private volatile boolean cancelled;

    Exchange(
            RouteSpec route,
            CallOptions options,
            byte[] body,
            String idempotencyKey,
            boolean safeToRepeat,
            long deadlineAt) {
        this.route = route;
        this.options = options;
        this.body = body;
        this.idempotencyKey = idempotencyKey;
        this.safeToRepeat = safeToRepeat;
        this.deadlineAt = deadlineAt;
    }

    String label() {
        return route.method() + " " + route.path();
    }

    /** Records the HTTP exchange now in flight, aborting it at once if the call is already off. */
    void inFlight(CompletableFuture<?> attemptFuture) {
        inFlight.set(attemptFuture);
        if (cancelled) attemptFuture.cancel(true);
    }

    /** Marks the call cancelled and aborts whatever attempt is in flight. */
    void cancel(boolean mayInterrupt) {
        cancelled = true;
        CompletableFuture<?> current = inFlight.get();
        if (current != null) current.cancel(mayInterrupt);
    }

    /** Whether the caller has cancelled the future this call belongs to. */
    boolean cancelled() {
        return cancelled;
    }
}
