package com.oblodai.core;

import java.util.Map;

/**
 * One attempt about to be sent, as the request hook sees it.
 *
 * @param method HTTP method
 * @param url the full URL
 * @param headers the headers as sent, with the signature and the admin token redacted
 * @param attempt 1 for the first attempt, 2 for the first retry, and so on
 * @param requestId {@code X-Request-ID} of the call; the same on every attempt
 * @param operationId the route's OpenAPI {@code operationId}
 */
public record RequestInfo(
        String method,
        String url,
        Map<String, String> headers,
        int attempt,
        String requestId,
        String operationId) {}
