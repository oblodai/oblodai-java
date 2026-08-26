// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/testing-webhook}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentTestingWebhookRequest {

    /** Status in the body. Default paid. Example: {@code paid}. One of PaymentStatus. */
    @JsonProperty("status")
    private String status;

    /** Where to send the test body. If not provided, delivery goes to the project's registered endpoint; without an endpoint it fails with webhook.no_endpoint. Signed with the project endpoint's secret, including when url is passed explicitly. Example: {@code https://shop.example/hook}. */
    @JsonProperty("url")
    private String url;

    /** Sets {@code status}. */
    public PaymentTestingWebhookRequest status(String value) {
        this.status = value;
        return this;
    }

    /** Current {@code status}. */
    public String status() {
        return status;
    }

    /** Sets {@code url}. */
    public PaymentTestingWebhookRequest url(String value) {
        this.url = value;
        return this;
    }

    /** Current {@code url}. */
    public String url() {
        return url;
    }

}
