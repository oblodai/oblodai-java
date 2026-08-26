// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.*;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/payment/batch}.
 *
 * <p>Fluent setters; unset fields are omitted from the JSON, never sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PaymentBatchRequest {

    /** What to do when an item fails: continue (default) — process the rest; stop — halt processing after the first error. Example: {@code continue}. */
    @JsonProperty("on_error")
    private String onError;

    /** Array of 1 to 5000 items — the same fields as POST /v1/payment; set order_id on every item: results are matched by it and it protects against duplicates. Required. */
    @JsonProperty("payments")
    private List<Payment> payments;

    /** Sets {@code on_error}. */
    public PaymentBatchRequest onError(String value) {
        this.onError = value;
        return this;
    }

    /** Sets {@code on_error} from the generated vocabulary. */
    public PaymentBatchRequest onError(BatchOnError value) {
        this.onError = value == null ? null : value.wire();
        return this;
    }

    /** Current {@code on_error}. */
    public String onError() {
        return onError;
    }

    /** Sets {@code payments}. */
    public PaymentBatchRequest payments(List<Payment> value) {
        this.payments = value;
        return this;
    }

    /** Current {@code payments}. */
    public List<Payment> payments() {
        return payments;
    }

    public static final class Payment {

        /** Under/overpayment tolerance, 0–5 %. Overrides the merchant setting. */
        @JsonProperty("accuracy_payment_percent")
        private Double accuracyPaymentPercent;

        /** Private merchant data, echoed back in webhooks (not visible to the buyer). */
        @JsonProperty("additional_data")
        private String additionalData;

        /** Amount to pay, in currency. Required. Example: {@code 10}. */
        @JsonProperty("amount")
        private String amount;

        /** Price currency code: any of the 23 fiats (USD, EUR, RUB, …) or any coin (USDT, BTC, …). JPY and KRW have zero decimal places. Required. Example: {@code USD}. */
        @JsonProperty("currency")
        private String currency;

        /** Allow paying up the remaining amount. */
        @JsonProperty("is_payment_multiple")
        private Boolean isPaymentMultiple;

        /** Revive an expired invoice by order_id instead of creating a new one. */
        @JsonProperty("is_refresh")
        private Boolean isRefresh;

        /** Invoice lifetime in seconds, 300–43200; default 3600. Values outside the range are clamped to the nearest bound. Example: {@code 3600}. */
        @JsonProperty("lifetime_seconds")
        private Integer lifetimeSeconds;

        /** Settlement network (e.g. tron, ethereum). Optional — see the currency and network selection modes. Example: {@code tron}. */
        @JsonProperty("network")
        private String network;

        /** Merchant reference; idempotency key. Strongly recommended. Required. Example: {@code order-1}. */
        @JsonProperty("order_id")
        private String orderId;

        /** Payer email. If set, a cheque is sent there automatically after payment; it is also the default recipient for POST /v1/payment/send-email. */
        @JsonProperty("payer_email")
        private String payerEmail;

        /** Deprecated: % of the network markup charged to the payer (0–100); payer-facing markups are configured via discount. */
        @JsonProperty("subtract")
        private Integer subtract;

        /** Payment page theme: dark | light. Example: {@code dark}. */
        @JsonProperty("theme")
        private String theme;

        /** Settlement currency — the crypto used for payment. Defaults to currency (only if currency is a coin); with a fiat price set it explicitly or omit it together with network. Example: {@code USDT}. */
        @JsonProperty("to_currency")
        private String toCurrency;

        /** Per-invoice webhook. Requires a registered endpoint (POST /v1/webhooks): delivery is signed with its secret. */
        @JsonProperty("url_callback")
        private String urlCallback;

        /** "Back to shop" link on the payment page. */
        @JsonProperty("url_return")
        private String urlReturn;

        /** Redirect after successful payment. */
        @JsonProperty("url_success")
        private String urlSuccess;

        /** Sets {@code accuracy_payment_percent}. */
        public Payment accuracyPaymentPercent(Double value) {
            this.accuracyPaymentPercent = value;
            return this;
        }

        /** Current {@code accuracy_payment_percent}. */
        public Double accuracyPaymentPercent() {
            return accuracyPaymentPercent;
        }

        /** Sets {@code additional_data}. */
        public Payment additionalData(String value) {
            this.additionalData = value;
            return this;
        }

        /** Current {@code additional_data}. */
        public String additionalData() {
            return additionalData;
        }

        /** Sets {@code amount}. */
        public Payment amount(String value) {
            this.amount = value;
            return this;
        }

        /** Current {@code amount}. */
        public String amount() {
            return amount;
        }

        /** Sets {@code currency}. */
        public Payment currency(String value) {
            this.currency = value;
            return this;
        }

        /** Current {@code currency}. */
        public String currency() {
            return currency;
        }

        /** Sets {@code is_payment_multiple}. */
        public Payment isPaymentMultiple(Boolean value) {
            this.isPaymentMultiple = value;
            return this;
        }

        /** Current {@code is_payment_multiple}. */
        public Boolean isPaymentMultiple() {
            return isPaymentMultiple;
        }

        /** Sets {@code is_refresh}. */
        public Payment isRefresh(Boolean value) {
            this.isRefresh = value;
            return this;
        }

        /** Current {@code is_refresh}. */
        public Boolean isRefresh() {
            return isRefresh;
        }

        /** Sets {@code lifetime_seconds}. */
        public Payment lifetimeSeconds(Integer value) {
            this.lifetimeSeconds = value;
            return this;
        }

        /** Current {@code lifetime_seconds}. */
        public Integer lifetimeSeconds() {
            return lifetimeSeconds;
        }

        /** Sets {@code network}. */
        public Payment network(String value) {
            this.network = value;
            return this;
        }

        /** Sets {@code network} from the generated vocabulary. */
        public Payment network(Network value) {
            this.network = value == null ? null : value.wire();
            return this;
        }

        /** Current {@code network}. */
        public String network() {
            return network;
        }

        /** Sets {@code order_id}. */
        public Payment orderId(String value) {
            this.orderId = value;
            return this;
        }

        /** Current {@code order_id}. */
        public String orderId() {
            return orderId;
        }

        /** Sets {@code payer_email}. */
        public Payment payerEmail(String value) {
            this.payerEmail = value;
            return this;
        }

        /** Current {@code payer_email}. */
        public String payerEmail() {
            return payerEmail;
        }

        /** Sets {@code subtract}. */
        public Payment subtract(Integer value) {
            this.subtract = value;
            return this;
        }

        /** Current {@code subtract}. */
        public Integer subtract() {
            return subtract;
        }

        /** Sets {@code theme}. */
        public Payment theme(String value) {
            this.theme = value;
            return this;
        }

        /** Current {@code theme}. */
        public String theme() {
            return theme;
        }

        /** Sets {@code to_currency}. */
        public Payment toCurrency(String value) {
            this.toCurrency = value;
            return this;
        }

        /** Current {@code to_currency}. */
        public String toCurrency() {
            return toCurrency;
        }

        /** Sets {@code url_callback}. */
        public Payment urlCallback(String value) {
            this.urlCallback = value;
            return this;
        }

        /** Current {@code url_callback}. */
        public String urlCallback() {
            return urlCallback;
        }

        /** Sets {@code url_return}. */
        public Payment urlReturn(String value) {
            this.urlReturn = value;
            return this;
        }

        /** Current {@code url_return}. */
        public String urlReturn() {
            return urlReturn;
        }

        /** Sets {@code url_success}. */
        public Payment urlSuccess(String value) {
            this.urlSuccess = value;
            return this;
        }

        /** Current {@code url_success}. */
        public String urlSuccess() {
            return urlSuccess;
        }

    }
}
