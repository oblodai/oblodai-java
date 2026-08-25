package com.oblodai.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.errors.ApiErrors;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.ErrorDetail;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
 */
public final class Envelope {

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

        if (status >= 300 && status < 400) {
            String where = location == null ? "" : " to " + location;
            throw ApiErrors.from(
                    status,
                    new ErrorDetail(
                            "internal",
                            "unexpected redirect (HTTP " + status + ")" + where + "; check baseUrl",
                            null,
                            null,
                            null,
                            null),
                    text,
                    true,
                    retryAfter);
        }

        JsonNode body = null;
        if (text != null && !text.isEmpty()) {
            try {
                body = mapper.readTree(text);
            } catch (Exception parseFailure) {
                if (status >= 400) throw noEnvelope(status, text, text, retryAfter);
                throw new ContractException(
                        "expected a JSON envelope, got " + describe(text), status, text);
            }
        }

        if (body != null && body.isObject() && body.get("error") != null && body.get("error").isObject()) {
            ErrorDetail detail = mapper.convertValue(body.get("error"), ErrorDetail.class);
            throw ApiErrors.from(status, detail, body, false, retryAfter);
        }
        if (status >= 400) throw noEnvelope(status, text, body, retryAfter);

        if (body != null
                && body.isObject()
                && body.path("state").asInt(-1) == 0
                && body.has("result")) {
            return body.get("result");
        }
        throw new ContractException(
                "response is not a {state:0,result} envelope: " + describe(text), status, body);
    }

    /** {@code Retry-After} as delta-seconds or an HTTP-date; {@code null} when absent or unusable. */
    public static Integer parseRetryAfter(String value, long nowMillis) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        if (v.matches("\\d+")) {
            try {
                return Integer.valueOf(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            long at = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond();
            return (int) Math.max(0, Math.ceil((at * 1000.0 - nowMillis) / 1000.0));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static com.oblodai.errors.ApiException noEnvelope(
            int status, String text, Object raw, Integer retryAfter) {
        String message =
                "HTTP "
                        + status
                        + " without an Oblodai error envelope ("
                        + describe(text)
                        + ") — the answer came from a proxy or load balancer, not the API";
        return ApiErrors.from(
                status,
                new ErrorDetail("internal", message, null, null, null, null),
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
