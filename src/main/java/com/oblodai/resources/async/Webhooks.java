package com.oblodai.resources.async;

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
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.errors.ConfigException;
import com.oblodai.models.WebhookDelivery;
import com.oblodai.models.WebhookEndpoint;
import com.oblodai.models.WebhookSecretRotated;
import com.oblodai.models.WebhookTestResult;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Webhook endpoint management and delivery inspection. Verification of an incoming delivery lives
 * in {@code com.oblodai.webhooks}, not here.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Webhooks}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
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
     * @return a future of the registered endpoint, with its signing secret
     */
    public CompletableFuture<WebhookEndpoint> register(String url) {
        return register(url, RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks}.
     *
     * @param url the endpoint deliveries are sent to
     * @param options per-call options
     * @return a future of the registered endpoint, with its signing secret
     */
    public CompletableFuture<WebhookEndpoint> register(String url, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_WEBHOOKS,
                new WebhooksRequest().url(url),
                options,
                WebhookEndpoint.class);
    }

    /**
     * {@code POST /v1/webhooks/rotate-secret} — mints a new secret; the old one keeps verifying
     * until {@code previous_secret_valid_until}.
     *
     * @return a future of the new secret, and how long the old one stays valid
     */
    public CompletableFuture<WebhookSecretRotated> rotateSecret() {
        return rotateSecret(RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/rotate-secret}.
     *
     * @param options per-call options
     * @return a future of the new secret, and how long the old one stays valid
     */
    public CompletableFuture<WebhookSecretRotated> rotateSecret(RequestOptions options) {
        return callAsync(
                Routes.POST_V1_WEBHOOKS_ROTATE_SECRET, null, options, WebhookSecretRotated.class);
    }

    /**
     * {@code POST /v1/webhooks/deliveries} — the delivery log, newest first.
     *
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> deliveries() {
        return deliveries(RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/deliveries}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> deliveries(RequestOptions options) {
        return deliveries(new WebhooksDeliveriesRequest(), options);
    }

    /**
     * {@code POST /v1/webhooks/deliveries}.
     *
     * @param params page bounds
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> deliveries(WebhooksDeliveriesRequest params) {
        return deliveries(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/webhooks/deliveries}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> deliveries(
            WebhooksDeliveriesRequest params, RequestOptions options) {
        return pagerAsync(
                Routes.POST_V1_WEBHOOKS_DELIVERIES, params, options, WebhookDelivery.class);
    }

    /**
     * {@code POST /v1/test-webhook/payment} — delivers a sample payment event to
     * {@code url_callback}, signed exactly like a real one.
     *
     * @param request what the sample event should say
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testPayment(TestWebhookPaymentRequest request) {
        return testPayment(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/payment}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testPayment(
            TestWebhookPaymentRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_TEST_WEBHOOK_PAYMENT, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/payout} — a sample payout event.
     *
     * @param request what the sample event should say
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testPayout(TestWebhookPayoutRequest request) {
        return testPayout(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/payout}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testPayout(
            TestWebhookPayoutRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_TEST_WEBHOOK_PAYOUT, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/wallet} — a sample static-wallet event.
     *
     * @param request what the sample event should say
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testWallet(TestWebhookWalletRequest request) {
        return testWallet(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/wallet}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return a future of what was delivered, and how the endpoint answered
     */
    public CompletableFuture<WebhookTestResult> testWallet(
            TestWebhookWalletRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_TEST_WEBHOOK_WALLET, request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/test-webhook/&#123;payment|payout|wallet&#125;} — picks the door by kind. The
     * request belongs to that kind: a {@link TestWebhookPaymentRequest},
     * {@link TestWebhookPayoutRequest} or {@link TestWebhookWalletRequest}.
     *
     * @param kind which event to rehearse
     * @param request what the sample event should say
     * @return a future of what was delivered, and how the endpoint answered
     * @throws IllegalArgumentException when the kind is outside this snapshot's vocabulary
     */
    public CompletableFuture<WebhookTestResult> test(WebhookKind kind, Object request) {
        return test(kind, request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/test-webhook/&#123;payment|payout|wallet&#125;}.
     *
     * @param kind which event to rehearse
     * @param request what the sample event should say
     * @param options per-call options
     * @return a future of what was delivered, and how the endpoint answered
     * @throws IllegalArgumentException when the kind is outside this snapshot's vocabulary
     */
    public CompletableFuture<WebhookTestResult> test(
            WebhookKind kind, Object request, RequestOptions options) {
        return callAsync(routeFor(kind), request, options, WebhookTestResult.class);
    }

    /**
     * {@code POST /v1/payment/testing-webhook} — the older rehearsal door, payment events only.
     *
     * @param request what the sample event should say
     * @return a future of what was delivered, and how the endpoint answered
     * @deprecated use {@link #test(WebhookKind, Object)} with {@link WebhookKind#PAYMENT}, or
     *     {@link #testPayment(TestWebhookPaymentRequest)}.
     */
    @Deprecated
    public CompletableFuture<WebhookTestResult> testLegacy(PaymentTestingWebhookRequest request) {
        return testLegacy(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/testing-webhook}.
     *
     * @param request what the sample event should say
     * @param options per-call options
     * @return a future of what was delivered, and how the endpoint answered
     * @deprecated use {@link #test(WebhookKind, Object, RequestOptions)} with
     *     {@link WebhookKind#PAYMENT}.
     */
    @Deprecated
    public CompletableFuture<WebhookTestResult> testLegacy(
            PaymentTestingWebhookRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_PAYMENT_TESTING_WEBHOOK, request, options, WebhookTestResult.class);
    }

    private static RouteSpec routeFor(WebhookKind kind) {
        if (kind == null) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG, "a webhook kind is required", "kind");
        }
        if (WebhookKind.PAYMENT.equals(kind)) return Routes.POST_V1_TEST_WEBHOOK_PAYMENT;
        if (WebhookKind.PAYOUT.equals(kind)) return Routes.POST_V1_TEST_WEBHOOK_PAYOUT;
        if (WebhookKind.WALLET.equals(kind)) return Routes.POST_V1_TEST_WEBHOOK_WALLET;
        // A kind the gateway grew after this snapshot: it has no rehearsal route here yet.
        throw new ConfigException(
                ConfigException.BAD_CONFIG,
                "no rehearsal route for webhook kind \"" + kind.wire() + "\"",
                "kind");
    }
}
