package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.RouteSpec;
import com.oblodai.contract.Routes;
import com.oblodai.contract.WebhookKind;
import com.oblodai.contract.requests.PaymentTestingWebhookRequest;
import com.oblodai.contract.requests.TestWebhookPaymentRequest;
import com.oblodai.contract.requests.TestWebhookPayoutRequest;
import com.oblodai.contract.requests.TestWebhookWalletRequest;
import com.oblodai.contract.requests.WebhooksDeliveriesRequest;
import com.oblodai.contract.requests.WebhooksRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.WebhookDelivery;
import com.oblodai.models.WebhookEndpoint;
import com.oblodai.models.WebhookSecretRotated;
import com.oblodai.models.WebhookTestResult;

/**
 * Webhook endpoint management and delivery inspection. Verification of an incoming delivery lives
 * in {@code com.oblodai.webhooks}, not here.
 */
public final class Webhooks extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Webhooks(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/webhooks} — registers (or replaces) the merchant's endpoint; the answer
     * carries the signing secret, and carries it only once.
     *
     * @param url the endpoint deliveries are sent to
     * @return the registered endpoint, with its signing secret
     */
    public WebhookEndpoint register(String url) {
        return register(url, RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks}.
     *
     * @param url the endpoint deliveries are sent to
     * @param options per-call options
     * @return the registered endpoint, with its signing secret
     */
    public WebhookEndpoint register(String url, RequestOptions options) {
        return call(
                Routes.POST_V1_WEBHOOKS,
                new WebhooksRequest().url(url),
                options,
                WebhookEndpoint.class);
    }

    /**
     * {@code POST /v1/webhooks/rotate-secret} — mints a new secret; the old one keeps verifying
     * until {@code previous_secret_valid_until}. Payout key.
     *
     * @return the new secret, and how long the old one stays valid
     */
    public WebhookSecretRotated rotateSecret() {
        return rotateSecret(RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/rotate-secret}.
     *
     * @param options per-call options
     * @return the new secret, and how long the old one stays valid
     */
    public WebhookSecretRotated rotateSecret(RequestOptions options) {
        return call(
                Routes.POST_V1_WEBHOOKS_ROTATE_SECRET, null, options, WebhookSecretRotated.class);
    }

    /**
     * {@code POST /v1/webhooks/deliveries} — the delivery log, newest first.
     *
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> deliveries() {
        return deliveries(new WebhooksDeliveriesRequest(), RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/deliveries}.
     *
     * @param params page bounds
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> deliveries(WebhooksDeliveriesRequest params) {
        return deliveries(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/deliveries}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> deliveries(
            WebhooksDeliveriesRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_WEBHOOKS_DELIVERIES, params, options, WebhookDelivery.class);
    }

    /**
     * {@code POST /v1/test-webhook/payment} — delivers a sample payment event to
     * {@code url_callback}, signed exactly like a real one.
     *
     * @param request what the sample event should say
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testPayment(TestWebhookPaymentRequest request) {
        return testPayment(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/payment}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testPayment(
            TestWebhookPaymentRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_TEST_WEBHOOK_PAYMENT, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/payout} — a sample payout event. Payout key.
     *
     * @param request what the sample event should say
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testPayout(TestWebhookPayoutRequest request) {
        return testPayout(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/payout}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testPayout(TestWebhookPayoutRequest request, RequestOptions options) {
        return call(Routes.POST_V1_TEST_WEBHOOK_PAYOUT, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/wallet} — a sample static-wallet event.
     *
     * @param request what the sample event should say
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testWallet(TestWebhookWalletRequest request) {
        return testWallet(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/wallet}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return what was delivered, and how the endpoint answered
     */
    public WebhookTestResult testWallet(TestWebhookWalletRequest request, RequestOptions options) {
        return call(Routes.POST_V1_TEST_WEBHOOK_WALLET, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/&#123;payment|payout|wallet&#125;} — picks the door by kind. The
     * request belongs to that kind: a {@link TestWebhookPaymentRequest},
     * {@link TestWebhookPayoutRequest} or {@link TestWebhookWalletRequest}.
     *
     * @param kind which event to rehearse
     * @param request what the sample event should say
     * @return what was delivered, and how the endpoint answered
     * @throws IllegalArgumentException when the kind is outside this snapshot's vocabulary
     */
    public WebhookTestResult test(WebhookKind kind, Object request) {
        return test(kind, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/&#123;payment|payout|wallet&#125;}.
     *
     * @param kind which event to rehearse
     * @param request what the sample event should say
     * @param options per-call options
     * @return what was delivered, and how the endpoint answered
     * @throws IllegalArgumentException when the kind is outside this snapshot's vocabulary
     */
    public WebhookTestResult test(WebhookKind kind, Object request, RequestOptions options) {
        return call(routeFor(kind), request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/payment/testing-webhook} — the older rehearsal door, payment events only.
     *
     * @param request what the sample event should say
     * @return what was delivered, and how the endpoint answered
     * @deprecated use {@link #test(WebhookKind, Object)} with {@link WebhookKind#PAYMENT}, or
     *     {@link #testPayment(TestWebhookPaymentRequest)}.
     */
    @Deprecated
    public WebhookTestResult testLegacy(PaymentTestingWebhookRequest request) {
        return testLegacy(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/testing-webhook}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return what was delivered, and how the endpoint answered
     * @deprecated use {@link #test(WebhookKind, Object, RequestOptions)} with
     *     {@link WebhookKind#PAYMENT}.
     */
    @Deprecated
    public WebhookTestResult testLegacy(
            PaymentTestingWebhookRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_PAYMENT_TESTING_WEBHOOK, request, options, WebhookTestResult.class);
    }

    private static RouteSpec routeFor(WebhookKind kind) {
        if (kind == null) throw new IllegalArgumentException("webhook kind is required");
        return switch (kind) {
            case PAYMENT -> Routes.POST_V1_TEST_WEBHOOK_PAYMENT;
            case PAYOUT -> Routes.POST_V1_TEST_WEBHOOK_PAYOUT;
            case WALLET -> Routes.POST_V1_TEST_WEBHOOK_WALLET;
            case UNKNOWN ->
                    throw new IllegalArgumentException(
                            "unknown webhook kind: no test route for it");
        };
    }
}
