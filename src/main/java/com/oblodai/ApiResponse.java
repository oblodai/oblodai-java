package com.oblodai;

import com.oblodai.core.RawResponse;
import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * A successful call's value together with its HTTP side: status, headers and request id. What
 * {@link Oblodai#withRawResponse} returns; an error status still throws, exactly as without it.
 *
 * @param <T> the value
 */
public final class ApiResponse<T> {

    private final T value;
    private final RawResponse raw;

    ApiResponse(T value, RawResponse raw) {
        this.value = value;
        this.raw = raw;
    }

    /** @return what the method returned */
    public T value() {
        return value;
    }

    /** @return the HTTP status of the answer */
    public int status() {
        return raw.status();
    }

    /** @return the response headers */
    public HttpHeaders headers() {
        return raw.headers();
    }

    /**
     * @param name header name, matched case-insensitively
     * @return its first value
     */
    public Optional<String> header(String name) {
        return raw.header(name);
    }

    /** @return the answer's {@code X-Request-ID}, else the one this SDK sent with the call */
    public String requestId() {
        return raw.header("X-Request-ID").orElse(raw.requestId());
    }

    /** @return the raw body */
    public byte[] body() {
        return raw.body();
    }

    @Override
    public String toString() {
        return "ApiResponse(status=" + status() + ", requestId=" + requestId() + ")";
    }
}
