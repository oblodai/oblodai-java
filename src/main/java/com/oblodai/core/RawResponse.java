package com.oblodai.core;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * What one HTTP attempt produced, before the envelope is read.
 *
 * @param status HTTP status
 * @param headers response headers
 * @param body response bytes
 * @param requestId the {@code X-Request-ID} the SDK sent with the call
 */
public record RawResponse(int status, HttpHeaders headers, byte[] body, String requestId) {

    /**
     * @param name header name, matched case-insensitively
     * @return its first value
     */
    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    /** @return the response content type, or {@code null} */
    public String contentType() {
        return headers.firstValue("content-type").orElse(null);
    }

    /** @return the body as UTF-8 text */
    public String text() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
