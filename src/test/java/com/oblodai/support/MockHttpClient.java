package com.oblodai.support;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * An {@link HttpClient} that answers from a script and records every request it saw. The SDK takes
 * the client as an option, so the fake substitutes at exactly the seam the real one occupies:
 * everything above it — signing, envelopes, retries, pagination — runs unchanged.
 */
public final class MockHttpClient extends HttpClient {

    /** One recorded request. */
    public record Recorded(URI uri, String method, Map<String, String> headers, String body) {

        /** A header value, looked up in lower case. */
        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    /** One scripted answer. */
    public static final class Scripted {
        int status = 200;
        String body = "{\"state\":0,\"result\":{}}";
        final Map<String, String> headers = new LinkedHashMap<>();
        Throwable throwable;
        long delayMs;

        Scripted() {
            headers.put("content-type", "application/json");
        }
    }

    private final Deque<Scripted> script = new ArrayDeque<>();
    private final List<Recorded> calls = new ArrayList<>();

    /** Every request the SDK made, in order. */
    public List<Recorded> calls() {
        return calls;
    }

    /** The single request the SDK made. */
    public Recorded onlyCall() {
        if (calls.size() != 1) throw new AssertionError("expected exactly one call, got " + calls.size());
        return calls.get(0);
    }

    /**
     * Queues a success envelope.
     *
     * @param resultJson the {@code result} payload, as JSON text
     * @return this
     */
    public MockHttpClient ok(String resultJson) {
        Scripted next = new Scripted();
        next.body = "{\"state\":0,\"result\":" + resultJson + "}";
        script.add(next);
        return this;
    }

    /**
     * Queues an error envelope.
     *
     * @param status HTTP status
     * @param errorJson the {@code error} object, as JSON text
     * @param headers extra response headers, name and value in turn
     * @return this
     */
    public MockHttpClient apiError(int status, String errorJson, String... headers) {
        Scripted next = new Scripted();
        next.status = status;
        next.body = "{\"error\":" + errorJson + "}";
        addHeaders(next, headers);
        script.add(next);
        return this;
    }

    /**
     * Queues a body that carries no gateway envelope, as a proxy or load balancer would answer.
     *
     * @param status HTTP status
     * @param body the raw body
     * @param headers extra response headers, name and value in turn
     * @return this
     */
    public MockHttpClient raw(int status, String body, String... headers) {
        Scripted next = new Scripted();
        next.status = status;
        next.body = body;
        addHeaders(next, headers);
        script.add(next);
        return this;
    }

    /**
     * Queues a transport failure.
     *
     * @param failure what the client throws instead of answering
     * @return this
     */
    public MockHttpClient fails(Throwable failure) {
        Scripted next = new Scripted();
        next.throwable = failure;
        script.add(next);
        return this;
    }

    /**
     * Queues an answer that arrives late enough to trip a timeout.
     *
     * @param delayMs how long to wait before answering
     * @return this
     */
    public MockHttpClient slow(long delayMs) {
        Scripted next = new Scripted();
        next.delayMs = delayMs;
        script.add(next);
        return this;
    }

    private static void addHeaders(Scripted next, String... headers) {
        for (int i = 0; i + 1 < headers.length; i += 2) {
            next.headers.put(headers[i].toLowerCase(Locale.ROOT), headers[i + 1]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        Map<String, String> headers = new LinkedHashMap<>();
        request.headers().map().forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v.get(0)));
        calls.add(new Recorded(request.uri(), request.method(), headers, bodyOf(request)));

        Scripted next = script.poll();
        if (next == null) {
            return CompletableFuture.failedFuture(
                    new AssertionError("no scripted response for " + request.method() + " " + request.uri()));
        }
        if (next.throwable != null) return CompletableFuture.failedFuture(next.throwable);

        CompletableFuture<HttpResponse<T>> answer =
                CompletableFuture.completedFuture(
                        (HttpResponse<T>) new Stub(request, next.status, next.body, next.headers));
        if (next.delayMs > 0) {
            // The real client fails the future with HttpTimeoutException; a delayed answer that
            // outlives the request timeout must do the same.
            Duration timeout = request.timeout().orElse(Duration.ofDays(1));
            if (next.delayMs > timeout.toMillis()) {
                return CompletableFuture.supplyAsync(
                                () -> null,
                                CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS))
                        .thenCompose(
                                ignored ->
                                        CompletableFuture.<HttpResponse<T>>failedFuture(
                                                new HttpTimeoutException("request timed out")));
            }
            return CompletableFuture.supplyAsync(
                            () -> null,
                            CompletableFuture.delayedExecutor(next.delayMs, TimeUnit.MILLISECONDS))
                    .thenCompose(ignored -> answer);
        }
        return answer;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> handler,
            HttpResponse.PushPromiseHandler<T> pushHandler) {
        return sendAsync(request, handler);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException {
        try {
            return sendAsync(request, handler).join();
        } catch (RuntimeException e) {
            throw new IOException(e.getCause() == null ? e : e.getCause());
        }
    }

    private static String bodyOf(HttpRequest request) {
        return request.bodyPublisher()
                .map(
                        publisher -> {
                            StringBuilder sb = new StringBuilder();
                            publisher.subscribe(
                                    new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                                        @Override
                                        public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                                            s.request(Long.MAX_VALUE);
                                        }

                                        @Override
                                        public void onNext(java.nio.ByteBuffer item) {
                                            byte[] chunk = new byte[item.remaining()];
                                            item.get(chunk);
                                            sb.append(new String(chunk, StandardCharsets.UTF_8));
                                        }

                                        @Override
                                        public void onError(Throwable throwable) {}

                                        @Override
                                        public void onComplete() {}
                                    });
                            return sb.toString();
                        })
                .orElse(null);
    }

    /** A minimal {@link HttpResponse} over the scripted body. */
    private static final class Stub implements HttpResponse<byte[]> {

        private final HttpRequest request;
        private final int status;
        private final String text;
        private final Map<String, String> headerMap;

        Stub(HttpRequest request, int status, String text, Map<String, String> headerMap) {
            this.request = request;
            this.status = status;
            this.text = text;
            this.headerMap = headerMap;
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<byte[]>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            Map<String, List<String>> multi = new LinkedHashMap<>();
            headerMap.forEach((k, v) -> multi.put(k, List.of(v)));
            return HttpHeaders.of(multi, (a, b) -> true);
        }

        @Override
        public byte[] body() {
            return text.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    // --- the rest of the HttpClient surface, unused by the SDK ----------------------------------

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }

    @Override
    public Redirect followRedirects() {
        return Redirect.NEVER;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.of(ProxySelector.of((InetSocketAddress) null));
    }

    @Override
    public SSLContext sslContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SSLParameters sslParameters() {
        return new SSLParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }

    @Override
    public Version version() {
        return Version.HTTP_1_1;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        throw new UnsupportedOperationException("the mock client has no websocket support");
    }
}
