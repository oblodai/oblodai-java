package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.oblodai.errors.NotFoundException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.TransportException;
import com.oblodai.generated.models.LookupRequest;
import com.oblodai.support.Clients;
import com.oblodai.support.MockHttpClient;
import org.junit.jupiter.api.Test;

/** Spec §3.6: an error prints as {@code [code] text (request_id=...)}. */
class ErrorStringTest {

    @Test
    void anApiErrorPrintsItsCodeTextAndRequestId() {
        MockHttpClient http =
                new MockHttpClient()
                        .apiError(
                                404,
                                "{\"code\":\"payment.not_found\",\"message\":\"no such invoice\","
                                        + "\"retryable\":false,\"request_id\":\"rq-7\"}");
        NotFoundException error =
                assertThrows(
                        NotFoundException.class,
                        () -> Clients.client(http).payments().getInfo(LookupRequest.builder().uuid("x").build()));
        assertEquals("[payment.not_found] no such invoice (request_id=rq-7)", error.getMessage());
        assertEquals(error.getMessage(), error.toString());
        assertEquals("no such invoice", error.text());
    }

    @Test
    void withoutARequestIdTheSuffixIsLeftOut() {
        OblodaiException error = new TransportException(TransportException.TIMEOUT, "request timed out", null);
        assertEquals("[transport.timeout] request timed out", error.toString());
    }
}
