// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payout/link/batch}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayoutLinkBatchRequest {

    /** Up to 500 links per call; each one succeeds or fails independently, the response is aligned with the request indexes. Required. */
    @JsonProperty("items")
    private List<Item> items;

    /** Sets {@code items}. */
    public PayoutLinkBatchRequest items(List<Item> value) {
        this.items = value;
        return this;
    }

    /** Current {@code items}. */
    public List<Item> items() {
        return items;
    }

    public static final class Item {

        /** Amount in currency, as a string; greater than zero. Required. Example: {@code 25}. */
        @JsonProperty("amount")
        private String amount;

        /** Payout crypto asset (USDT, BTC, …); fiat is not possible. Required. Example: {@code USDT}. */
        @JsonProperty("currency")
        private String currency;

        /** If set, the recipient receives an email with a "Claim funds" button; a delivery failure does not cancel link creation. Example: {@code user@example.com}. */
        @JsonProperty("email")
        private String email;

        /** Link lifetime in seconds, clamped to 3600–2592000 (one hour to 30 days); without the field or with 0 the link lives 1 hour, not the maximum — set it explicitly. Example: {@code 604800}. */
        @JsonProperty("expires_in_seconds")
        private Integer expiresInSeconds;

        /** Who pays the network fee: "recipient" (default — deducted from the amount, the recipient receives less) or "merchant" (the amount plus the fee is reserved, the recipient receives exactly amount). Example: {@code merchant}. */
        @JsonProperty("fee_bearer")
        private String feeBearer;

        /** Payout network for the recipient (tron, bitcoin, …). Required. Example: {@code tron}. */
        @JsonProperty("network")
        private String network;

        /** Message to the recipient (shown on the claim page and in the email). */
        @JsonProperty("note")
        private String note;

        /** Claim code — a second factor for the link: "auto" — we generate it and return it ONCE in the response, or your own (6–64 visible characters), empty — no code. Pass the code to the recipient over a channel SEPARATE from the link (it is not put into the email); after 10 incorrect attempts the link is locked. Example: {@code auto}. */
        @JsonProperty("passcode")
        private String passcode;

        /** Your deduplication key, unique per merchant; the Idempotency-Key header has no effect on this endpoint. Required. Example: {@code bonus-42}. */
        @JsonProperty("reference")
        private String reference;

        /** Title — shown to the recipient on the claim page. */
        @JsonProperty("title")
        private String title;

        /** Sets {@code amount}. */
        public Item amount(String value) {
            this.amount = value;
            return this;
        }

        /** Current {@code amount}. */
        public String amount() {
            return amount;
        }

        /** Sets {@code currency}. */
        public Item currency(String value) {
            this.currency = value;
            return this;
        }

        /** Current {@code currency}. */
        public String currency() {
            return currency;
        }

        /** Sets {@code email}. */
        public Item email(String value) {
            this.email = value;
            return this;
        }

        /** Current {@code email}. */
        public String email() {
            return email;
        }

        /** Sets {@code expires_in_seconds}. */
        public Item expiresInSeconds(Integer value) {
            this.expiresInSeconds = value;
            return this;
        }

        /** Current {@code expires_in_seconds}. */
        public Integer expiresInSeconds() {
            return expiresInSeconds;
        }

        /** Sets {@code fee_bearer}. */
        public Item feeBearer(String value) {
            this.feeBearer = value;
            return this;
        }

        /** Sets {@code fee_bearer} from the generated vocabulary. */
        public Item feeBearer(FeeBearer value) {
            this.feeBearer = value == null ? null : value.wire();
            return this;
        }

        /** Current {@code fee_bearer}. */
        public String feeBearer() {
            return feeBearer;
        }

        /** Sets {@code network}. */
        public Item network(String value) {
            this.network = value;
            return this;
        }

        /** Sets {@code network} from the generated vocabulary. */
        public Item network(Network value) {
            this.network = value == null ? null : value.wire();
            return this;
        }

        /** Current {@code network}. */
        public String network() {
            return network;
        }

        /** Sets {@code note}. */
        public Item note(String value) {
            this.note = value;
            return this;
        }

        /** Current {@code note}. */
        public String note() {
            return note;
        }

        /** Sets {@code passcode}. */
        public Item passcode(String value) {
            this.passcode = value;
            return this;
        }

        /** Current {@code passcode}. */
        public String passcode() {
            return passcode;
        }

        /** Sets {@code reference}. */
        public Item reference(String value) {
            this.reference = value;
            return this;
        }

        /** Current {@code reference}. */
        public String reference() {
            return reference;
        }

        /** Sets {@code title}. */
        public Item title(String value) {
            this.title = value;
            return this;
        }

        /** Current {@code title}. */
        public String title() {
            return title;
        }

    }
}
