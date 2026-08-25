package com.oblodai.core;

import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * What one HTTP attempt produced, before the envelope is read.
 *
 * @param status HTTP status
 * @param headers response headers
 * @param body response bytes
 */
public record RawResponse(int status, HttpHeaders headers, byte[] body) {

    /** First value of a response header, case-insensitively. */
    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    /** Response content type, or {@code null}. */
    public String contentType() {
        return headers.firstValue("content-type").orElse(null);
    }

    /** The body as UTF-8 text. */
    public String text() {
        return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }
}
