package com.oblodai.core;

import java.time.Duration;
import java.util.Map;

/**
 * How one attempt ended, as the response hook sees it: an HTTP response, or {@code status == 0}
 * and a transport {@code error}.
 *
 * @param request the attempt
 * @param status HTTP status, or 0 when the attempt produced no response
 * @param headers response headers, first value of each
 * @param elapsed time from sending the attempt to this point
 * @param error what the attempt ended with (an error status or a transport failure), else null
 */
public record ResponseInfo(
        RequestInfo request,
        int status,
        Map<String, String> headers,
        Duration elapsed,
        Throwable error) {}
