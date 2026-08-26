package com.oblodai.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.errors.ApiErrors;
import com.oblodai.errors.ApiException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.ErrorDetail;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Response envelopes, as the gateway writes them:
 *
 * <pre>
 *   success : { "state": 0, "result": &lt;payload&gt; }
 *   list    : result = { "items": [...], "paginate": { total, per_page, offset, has_pages } }
 *   error   : { "error": { code, message, field?, retryable, retry_after?, request_id? } }
 * </pre>
 *
 * <p>Every non-{@code bare} route uses these; bare routes (PDF documents) bypass this class.
 *
 * <p>The error object is decoded field by field, never as one all-or-nothing bind: a proxy that
 * answers {@code {"error":{"code":"x","retryable":"yes"}}} must still produce an SDK exception a
 * caller can branch on, not a {@link IllegalArgumentException} from the JSON binder.
 */
public final class Envelope {

    /**
     * Plausibility bound on any {@code retry_after} the gateway or a proxy states, in seconds: a day.
     * The value is reported to the caller as it stands, so this only stops nonsense (a date in the
     * year 9999, a number that would not fit an int) from reaching them; the pause the retry loop
     * actually takes is capped far lower by {@link RetryOptions#maxRetryAfterMs()}.
     */
    public static final int MAX_RETRY_AFTER_SECONDS = 86_400;

    /** The date formats an HTTP {@code Retry-After} may legally use. */
    private static final List<DateTimeFormatter> HTTP_DATES =
            List.of(
                    DateTimeFormatter.RFC_1123_DATE_TIME,
                    DateTimeFormatter.ofPattern(
                            "EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("EEE MMM ppd HH:mm:ss yyyy", Locale.ENGLISH)
                            .withZone(java.time.ZoneOffset.UTC));

    private Envelope() {}

    /**
     * Interprets a response body and returns the {@code result} node.
     *
     * @param mapper JSON mapper
     * @param status HTTP status of the answer
     * @param text the raw body, kept intact so a non-JSON failure keeps its evidence
     * @param retryAfterHeader the {@code Retry-After} header, when present
     * @param location the {@code Location} header, when the answer was a redirect
     * @return the {@code result} node of a success envelope
     * @throws com.oblodai.errors.ApiException when the body carries an error envelope, or the status
     *     says failure and nothing in front of the gateway wrote one
     * @throws ContractException when a 2xx body is not a {@code {state, result}} envelope
     */
    public static JsonNode decode(
            ObjectMapper mapper, int status, String text, String retryAfterHeader, String location) {
        Integer retryAfter = parseRetryAfter(retryAfterHeader, System.currentTimeMillis());

        if (status >= 300 && status < 400) throw redirect(status, location, text, retryAfter);

        JsonNode body = null;
        if (text != null && !text.isEmpty()) {
            try {
                body = mapper.readTree(text);
            } catch (Exception parseFailure) {
                if (status >= 400) throw noEnvelope(status, text, text, retryAfter, null);
                throw new ContractException(
                        "expected a JSON envelope, got " + describe(text), status, text);
            }
        }

        JsonNode error = body != null && body.isObject() ? body.get("error") : null;
        if (error != null && error.isObject()) {
            ErrorDetail detail = errorDetail(error, status);
            if (detail != null) throw ApiErrors.from(status, detail, body, false, retryAfter);
            // An "error" block with no usable code is not an envelope this SDK can classify.
            throw noEnvelope(status, text, body, retryAfter, string(error.get("request_id")));
        }
        if (status >= 400) throw noEnvelope(status, text, body, retryAfter, null);

        JsonNode state = body == null ? null : body.path("state");
        if (body != null
                && body.isObject()
                && state.isIntegralNumber()
                && state.intValue() == 0
                && body.has("result")) {
            return body.get("result");
        }
        throw new ContractException(
                "response is not a {state:0,result} envelope: " + describe(text), status, body);
    }

    /** The error the transport raises when an answer redirects — the gateway never does. */
    public static ApiException redirect(int status, String location, Object raw, Integer retryAfter) {
        String where = location == null ? "" : " to " + location;
        return ApiErrors.from(
                status,
                new ErrorDetail(
                        "internal",
                        "unexpected redirect (HTTP " + status + ")" + where + "; check baseUrl",
                        null,
                        null,
                        null,
                        null),
                raw,
                true,
                retryAfter);
    }

    /**
     * The {@code error} object, decoded one field at a time.
     *
     * @param error the {@code error} node of the body
     * @param status HTTP status, used when the object carries no message
     * @return the detail, or {@code null} when the object has no usable {@code code} and therefore
     *     cannot be treated as a gateway envelope at all
     */
    static ErrorDetail errorDetail(JsonNode error, int status) {
        String code = string(error.get("code"));
        if (code == null || code.isBlank()) return null;
        String message = string(error.get("message"));
        Boolean retryable = error.path("retryable").isBoolean() ? error.get("retryable").asBoolean() : null;
        return new ErrorDetail(
                code,
                message == null || message.isEmpty() ? "HTTP " + status : message,
                string(error.get("field")),
                retryable,
                retryAfterOf(error.get("retry_after")),
                string(error.get("request_id")));
    }

    /** A JSON string, or {@code null} for anything else (a number, an object, an absent field). */
    private static String string(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    /** {@code retry_after} as an integer, a float or a numeric string; anything else is absent. */
    private static Integer retryAfterOf(JsonNode node) {
        if (node == null) return null;
        if (node.isNumber()) return clampSeconds(node.doubleValue());
        if (node.isTextual()) return secondsOf(node.textValue().trim());
        return null;
    }

    /**
     * {@code Retry-After} as delta-seconds or an HTTP-date; {@code null} when absent or unusable.
     *
     * @param value the header value
     * @param nowMillis the current time, so an HTTP-date can be turned into a delay
     * @return seconds to wait, clamped to {@code [0, MAX_RETRY_AFTER_SECONDS]}, or null
     */
    public static Integer parseRetryAfter(String value, long nowMillis) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        Integer seconds = secondsOf(v);
        if (seconds != null) return seconds;
        for (DateTimeFormatter format : HTTP_DATES) {
            try {
                long at = ZonedDateTime.parse(v, format).toEpochSecond();
                // Wide arithmetic first: a date in the year 9999 must clamp, not wrap.
                return clampSeconds(Math.ceil((at * 1000.0 - nowMillis) / 1000.0));
            } catch (RuntimeException ignored) {
                // try the next format
            }
        }
        return null;
    }

    /** A decimal delta-seconds string, clamped; null when the text is not a plain number. */
    private static Integer secondsOf(String text) {
        if (!text.matches("[+-]?\\d+(\\.\\d+)?")) return null;
        try {
            return clampSeconds(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Seconds clamped into {@code [0, MAX_RETRY_AFTER_SECONDS]}, computed in a wide type. */
    private static Integer clampSeconds(double seconds) {
        if (Double.isNaN(seconds)) return null;
        double clamped = Math.min(Math.max(seconds, 0d), (double) MAX_RETRY_AFTER_SECONDS);
        return (int) Math.ceil(clamped);
    }

    private static ApiException noEnvelope(
            int status, String text, Object raw, Integer retryAfter, String requestId) {
        String message =
                "HTTP "
                        + status
                        + " without an Oblodai error envelope ("
                        + describe(text)
                        + ") — the answer came from a proxy or load balancer, not the API";
        return ApiErrors.from(
                status,
                new ErrorDetail("internal", message, null, null, null, requestId),
                raw,
                true,
                retryAfter);
    }

    private static String describe(String text) {
        if (text == null || text.isEmpty()) return "<empty body>";
        String head = text.substring(0, Math.min(120, text.length())).replaceAll("\\s+", " ");
        return text.length() > 120 ? head + "…" : head;
    }
}
