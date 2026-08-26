// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/webhooks}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WebhooksRequest {

    /** HTTPS callback URL. SSRF check: private and local addresses are rejected. Required. Example: {@code https://shop.example/oblodai/callback}. */
    @JsonProperty("url")
    private String url;

    /** Sets {@code url}. */
    public WebhooksRequest url(String value) {
        this.url = value;
        return this;
    }

    /** Current {@code url}. */
    public String url() {
        return url;
    }

}
