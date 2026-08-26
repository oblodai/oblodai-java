package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Envelope;
import com.oblodai.core.Json;
import com.oblodai.errors.ApiException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.OblodaiException;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

/**
 * The error envelope is read field by field. Whatever a gateway, a proxy or a hostile intermediary
 * writes into it, the caller gets an SDK exception with usable values — never a binder failure, and
 * never a delay computed from a number that overflowed.
 */
class EnvelopeTest {

    private static final ObjectMapper MAPPER = Json.newMapper();

    private static OblodaiException decode(int status, String body) {
        return assertThrows(
                OblodaiException.class, () -> Envelope.decode(MAPPER, status, body, null, null));
    }

    @Test
    void aRetryableFieldThatIsNotABooleanFallsBackToTheStatus() {
        OblodaiException soft =
                decode(429, "{\"error\":{\"code\":\"request.rate_limited\",\"retryable\":\"yes\"}}");
        assertEquals("request.rate_limited", soft.code());
        assertTrue(soft.retryable(), "429 defaults to retryable");

        OblodaiException hard =
                decode(400, "{\"error\":{\"code\":\"payment.below_minimum\",\"retryable\":[true]}}");
        assertFalse(hard.retryable(), "400 defaults to not retryable");
    }

    @Test
    void aMessageOrFieldOfTheWrongTypeDoesNotBreakTheDecode() {
        OblodaiException error =
                decode(
                        500,
                        "{\"error\":{\"code\":\"internal.oops\",\"message\":{\"nested\":1},\"field\":7,"
                                + "\"request_id\":\"req-1\"}}");
        assertEquals("internal.oops", error.code());
        assertEquals("HTTP 500", error.getMessage());
        assertNull(error.field());
        assertEquals("req-1", error.requestId());
    }

    @Test
    void anEnvelopeWithNoUsableCodeIsTreatedAsNoEnvelopeAtAllButKeepsTheRequestId() {
        OblodaiException noCode = decode(502, "{\"error\":{\"message\":\"bad gateway\"}}");
        assertEquals("internal", noCode.code());
        assertTrue(noCode.synthetic(), "a body without a code did not come from the gateway");

        OblodaiException numericCode =
                decode(500, "{\"error\":{\"code\":500,\"request_id\":\"req-9\"}}");
        assertTrue(numericCode.synthetic());
        assertEquals("req-9", numericCode.requestId(), "a string request id survives");
    }

    @Test
    void retryAfterIsReadFromIntegersFloatsAndNumericStringsAndClamped() {
        assertEquals(
                2,
                decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":1.4}}").retryAfter());
        assertEquals(
                3, decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":\"3\"}}").retryAfter());
        assertEquals(
                Envelope.MAX_RETRY_AFTER_SECONDS,
                decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":1e12}}").retryAfter(),
                "a hint no process would honour is clamped, not converted into a wrapped int");
        assertEquals(86_400, Envelope.MAX_RETRY_AFTER_SECONDS, "a day is the plausibility bound");
        assertEquals(
                3_600,
                decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":3600}}").retryAfter(),
                "an hour-long hint is reported as it stands; only the retry loop caps the pause");
        assertEquals(
                0, decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":-90}}").retryAfter());
        assertNull(
                decode(503, "{\"error\":{\"code\":\"db.unavailable\",\"retry_after\":{\"in\":5}}}")
                        .retryAfter());
    }

    @Test
    void theRetryAfterHeaderTakesDeltaSecondsOrAnHttpDateAndNothingElse() {
        long now = System.currentTimeMillis();
        assertEquals(120, Envelope.parseRetryAfter("120", now));
        assertEquals(120, Envelope.parseRetryAfter("  120  ", now));
        assertEquals(0, Envelope.parseRetryAfter("-30", now));
        assertNull(Envelope.parseRetryAfter("soon", now));
        assertNull(Envelope.parseRetryAfter("", now));

        String inTwoMinutes =
                DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(120));
        long seconds = Envelope.parseRetryAfter(inTwoMinutes, now);
        assertTrue(seconds >= 118 && seconds <= 122, "an HTTP-date becomes a delay: " + seconds);

        String year9999 = "Fri, 31 Dec 9999 23:59:59 GMT";
        assertEquals(
                Envelope.MAX_RETRY_AFTER_SECONDS,
                Envelope.parseRetryAfter(year9999, now),
                "a far-future date is capped, never negative and never overflowed");

        String longAgo = "Mon, 01 Jan 2001 00:00:00 GMT";
        assertEquals(0, Envelope.parseRetryAfter(longAgo, now));
    }

    @Test
    void aStateThatIsNotTheIntegerZeroIsNotASuccessEnvelope() {
        assertThrows(
                ContractException.class,
                () -> Envelope.decode(MAPPER, 200, "{\"state\":\"0\",\"result\":{}}", null, null));
        assertThrows(
                ContractException.class,
                () -> Envelope.decode(MAPPER, 200, "{\"state\":1,\"result\":{}}", null, null));
        assertThrows(
                ContractException.class,
                () -> Envelope.decode(MAPPER, 200, "<html>hello</html>", null, null));
    }

    @Test
    void aRedirectNamesItsTargetAndIsAnApiFailure() {
        ApiException redirect =
                assertThrows(
                        ApiException.class,
                        () -> Envelope.decode(MAPPER, 302, "", null, "https://elsewhere.test/v1"));
        assertEquals(302, redirect.httpStatus());
        assertTrue(redirect.getMessage().contains("elsewhere.test"));
    }
}
