// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/accepted/set}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentAcceptedSetRequest {

    /** The full list of currency+network pairs payers may use; an empty list accepts everything in the catalog. Required. */
    @JsonProperty("accepted")
    private List<Accepted> accepted;

    /** Sets {@code accepted}. */
    public PaymentAcceptedSetRequest accepted(List<Accepted> value) {
        this.accepted = value;
        return this;
    }

    /** Current {@code accepted}. */
    public List<Accepted> accepted() {
        return accepted;
    }

    public static final class Accepted {

        /** Asset code. Required. Example: {@code USDT}. */
        @JsonProperty("currency")
        private String currency;

        /** Asset network. Required. Example: {@code tron}. */
        @JsonProperty("network")
        private String network;

        /** Sets {@code currency}. */
        public Accepted currency(String value) {
            this.currency = value;
            return this;
        }

        /** Current {@code currency}. */
        public String currency() {
            return currency;
        }

        /** Sets {@code network}. */
        public Accepted network(String value) {
            this.network = value;
            return this;
        }

        /** Sets {@code network} from the generated vocabulary. */
        public Accepted network(Network value) {
            this.network = value == null ? null : value.wire();
            return this;
        }

        /** Current {@code network}. */
        public String network() {
            return network;
        }

    }
}
