package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.Bodies;
import com.oblodai.errors.ContractException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

/**
 * A response body is read with a ceiling. Without one, a gateway (or anything wearing its address)
 * that answers with a gigabyte takes the caller's process down instead of failing one call.
 */
class BodiesTest {

    /** The response metadata the handler inspects, with the headers we want it to see. */
    private static HttpResponse.ResponseInfo info(Map<String, List<String>> headers) {
        return new HttpResponse.ResponseInfo() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(headers, (a, b) -> true);
            }

            @Override
            public java.net.http.HttpClient.Version version() {
                return java.net.http.HttpClient.Version.HTTP_1_1;
            }
        };
    }

    /** A subscription that records whether the subscriber cut the stream off. */
    private static final class Sub implements Flow.Subscription {
        boolean cancelled;

        @Override
        public void request(long n) {}

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private static byte[] chunk(int size) {
        return new byte[size];
    }

    @Test
    void readsABodyThatFitsUnderTheCeiling() throws Exception {
        HttpResponse.BodySubscriber<byte[]> body =
                Bodies.limited(1024, "POST /v1/payment").apply(info(Map.of()));
        body.onSubscribe(new Sub());
        body.onNext(List.of(ByteBuffer.wrap("hel".getBytes()), ByteBuffer.wrap("lo".getBytes())));
        body.onComplete();

        assertEquals("hello", new String(body.getBody().toCompletableFuture().get()));
    }

    @Test
    void stopsTheStreamAndFailsTheCallWhenTheBodyGrowsPastTheCeiling() {
        Sub subscription = new Sub();
        HttpResponse.BodySubscriber<byte[]> body =
                Bodies.limited(1024, "POST /v1/payment/history").apply(info(Map.of()));
        body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(chunk(600))));
        body.onNext(List.of(ByteBuffer.wrap(chunk(600))));

        assertTrue(subscription.cancelled, "the subscription is cancelled, not drained");
        CompletionException failure =
                assertThrows(
                        CompletionException.class, () -> body.getBody().toCompletableFuture().join());
        ContractException overSize = assertInstanceOf(ContractException.class, failure.getCause());
        assertTrue(overSize.getMessage().contains("1024-byte ceiling"), overSize.getMessage());
    }

    @Test
    void refusesBeforeReadingAnythingWhenContentLengthAlreadyExceedsTheCeiling() {
        Sub subscription = new Sub();
        HttpResponse.BodySubscriber<byte[]> body =
                Bodies.limited(1024, "GET /v1/documents/statement")
                        .apply(info(Map.of("content-length", List.of("999999999"))));
        body.onSubscribe(subscription);

        assertTrue(subscription.cancelled);
        assertThrows(CompletionException.class, () -> body.getBody().toCompletableFuture().join());
    }

    @Test
    void theTwoCeilingsAreTheDocumentedOnes() {
        assertEquals(8L * 1024 * 1024, Bodies.JSON_LIMIT);
        assertEquals(64L * 1024 * 1024, Bodies.BARE_LIMIT);
    }
}
