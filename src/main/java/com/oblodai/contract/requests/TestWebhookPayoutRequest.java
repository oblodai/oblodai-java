// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/test-webhook/payout}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestWebhookPayoutRequest {

    /** Currency in the body. Example: {@code USDT}. */
    @JsonProperty("currency")
    private String currency;

    /** Network in the body. Example: {@code tron}. */
    @JsonProperty("network")
    private String network;

    /** Your order_id, which is put into the test event body. */
    @JsonProperty("order_id")
    private String orderId;

    /** Status in the body — from the status dictionary of this event type. Default paid (confirmed for a payout). Example: {@code paid}. One of PayoutStatus. */
    @JsonProperty("status")
    private String status;

    /** Where to send the test body. Required. Example: {@code https://shop.example/oblodai/callback}. */
    @JsonProperty("url_callback")
    private String urlCallback;

    /** UUID of the object (payment, wallet or payout) put into the test event body. */
    @JsonProperty("uuid")
    private String uuid;

    /** Sets {@code currency}. */
    public TestWebhookPayoutRequest currency(String value) {
        this.currency = value;
        return this;
    }

    /** Current {@code currency}. */
    public String currency() {
        return currency;
    }

    /** Sets {@code network}. */
    public TestWebhookPayoutRequest network(String value) {
        this.network = value;
        return this;
    }

    /** Sets {@code network} from the generated vocabulary. */
    public TestWebhookPayoutRequest network(Network value) {
        this.network = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code network}. */
    public String network() {
        return network;
    }

    /** Sets {@code order_id}. */
    public TestWebhookPayoutRequest orderId(String value) {
        this.orderId = value;
        return this;
    }

    /** Current {@code order_id}. */
    public String orderId() {
        return orderId;
    }

    /** Sets {@code status}. */
    public TestWebhookPayoutRequest status(String value) {
        this.status = value;
        return this;
    }

    /** Current {@code status}. */
    public String status() {
        return status;
    }

    /** Sets {@code url_callback}. */
    public TestWebhookPayoutRequest urlCallback(String value) {
        this.urlCallback = value;
        return this;
    }

    /** Current {@code url_callback}. */
    public String urlCallback() {
        return urlCallback;
    }

    /** Sets {@code uuid}. */
    public TestWebhookPayoutRequest uuid(String value) {
        this.uuid = value;
        return this;
    }

    /** Current {@code uuid}. */
    public String uuid() {
        return uuid;
    }

}
