package com.oblodai.core;

import com.oblodai.errors.ContractException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * Reading a response body with a ceiling. The JDK's {@code BodyHandlers.ofByteArray()} buffers
 * whatever arrives, so a gateway (or a proxy in front of it) that answers a {@code Content-Length}
 * of gigabytes would take the caller's process down with it. These handlers stop at a documented
 * limit and fail the call instead.
 */
public final class Bodies {

    /** Ceiling for a JSON envelope. The largest real answer is a page of a list route. */
    public static final long JSON_LIMIT = 8L * 1024 * 1024;

    /** Ceiling for a {@code bare} route — the generated PDF and CSV documents. */
    public static final long BARE_LIMIT = 64L * 1024 * 1024;

    private Bodies() {}

    /**
     * A byte-array body handler that refuses to buffer more than {@code max} bytes.
     *
     * @param max ceiling in bytes
     * @param label the route, for the error message
     * @return the handler
     */
    public static HttpResponse.BodyHandler<byte[]> limited(long max, String label) {
        return info -> {
            long declared = info.headers().firstValueAsLong("content-length").orElse(-1L);
            if (declared > max) return failed(oversize(label, declared, max));
            return new LimitingSubscriber(max, label);
        };
    }

    private static ContractException oversize(String label, long size, long max) {
        return new ContractException(
                ContractException.RESPONSE_TOO_LARGE,
                label
                        + ": response body is "
                        + size
                        + " bytes, over the "
                        + max
                        + "-byte ceiling this SDK reads — fetch the document by its job instead",
                0,
                null);
    }

    private static HttpResponse.BodySubscriber<byte[]> failed(RuntimeException failure) {
        return new LimitingSubscriber(failure);
    }

    /** Accumulates the body, cancelling the exchange the moment it grows past the ceiling. */
    private static final class LimitingSubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final List<ByteBuffer> chunks = new ArrayList<>();
        private final long max;
        private final String label;
        private final RuntimeException refuseUpFront;
        private Flow.Subscription subscription;
        private long size;

        LimitingSubscriber(long max, String label) {
            this.max = max;
            this.label = label;
            this.refuseUpFront = null;
        }

        LimitingSubscriber(RuntimeException refuseUpFront) {
            this.max = 0;
            this.label = "";
            this.refuseUpFront = refuseUpFront;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            if (refuseUpFront != null) {
                subscription.cancel();
                body.completeExceptionally(refuseUpFront);
                return;
            }
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (body.isDone()) return;
            for (ByteBuffer item : items) {
                size += item.remaining();
                if (size > max) {
                    subscription.cancel();
                    body.completeExceptionally(oversize(label, size, max));
                    return;
                }
                chunks.add(item);
            }
        }

        @Override
        public void onError(Throwable failure) {
            body.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            if (body.isDone()) return;
            byte[] out = new byte[(int) size];
            int at = 0;
            for (ByteBuffer chunk : chunks) {
                int length = chunk.remaining();
                chunk.get(out, at, length);
                at += length;
            }
            body.complete(out);
        }
    }
}
