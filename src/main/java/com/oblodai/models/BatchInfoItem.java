package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * One element of a batch as {@code /v1/batch/info} reports it. Unlike {@link BatchElement} it also
 * carries the element's own {@code status}, and its {@code result} is left untyped because a batch
 * of any kind is reported through the same shape.
 *
 * @param idx zero-based position of this element in the submitted batch
 * @param ok whether this element succeeded
 * @param orderId the merchant reference of this element, echoed back when one was sent
 * @param status this element's own lifecycle state
 * @param result the created object, present only when this element succeeded
 * @param message human-readable failure text, present only when this element failed
 * @param errorCode machine-readable failure code, present only when this element failed
 * @param httpStatus the HTTP status the equivalent single-item call would have answered with
 */
public record BatchInfoItem(
        @JsonProperty("idx") Integer idx,
        @JsonProperty("ok") Boolean ok,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") String status,
        @JsonProperty("result") Map<String, Object> result,
        @JsonProperty("message") String message,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("http_status") Integer httpStatus) {}
