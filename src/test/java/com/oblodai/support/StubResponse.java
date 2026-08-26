package com.oblodai.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A minimal {@link HttpResponse} over a scripted body, for {@link MockHttpClient}. Its {@code uri()}
 * is the requested one unless the script says the answer came from somewhere else — which is how a
 * redirect an injected client followed is simulated.
 */
final class StubResponse implements HttpResponse<byte[]> {

    private final HttpRequest request;
    private final int status;
    private final String text;
    private final Map<String, String> headerMap;
    private final URI finalUri;

    StubResponse(
            HttpRequest request,
            int status,
            String text,
            Map<String, String> headerMap,
            URI finalUri) {
        this.request = request;
        this.status = status;
        this.text = text;
        this.headerMap = headerMap;
        this.finalUri = finalUri;
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
        return finalUri != null ? finalUri : request.uri();
    }

    @Override
    public HttpClient.Version version() {
        return HttpClient.Version.HTTP_1_1;
    }
}
