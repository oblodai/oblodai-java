package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.SandboxDepositRequest;
import com.oblodai.contract.requests.SandboxFaucetRequest;
import com.oblodai.contract.requests.SandboxWebhooksReplayRequest;
import com.oblodai.core.AsyncPager;
import com.oblodai.core.Transport;
import com.oblodai.models.FaucetResult;
import com.oblodai.models.SandboxDeposit;
import com.oblodai.models.SandboxReplay;
import com.oblodai.models.SandboxReset;
import com.oblodai.models.WebhookDelivery;
import com.oblodai.resources.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The developer sandbox: fake money, simulated deposits and a webhook inspector. Every route here
 * needs a {@code test_} key — a live key is refused.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Sandbox}: the same methods,
 * returning {@link CompletableFuture} and {@link com.oblodai.core.AsyncPager}.
 */
public final class Sandbox extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Sandbox(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/sandbox/faucet} — credits test funds to the sandbox balance.
     *
     * @param request which asset, and how much
     * @return a future of what was credited
     */
    public CompletableFuture<FaucetResult> faucet(SandboxFaucetRequest request) {
        return faucet(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/faucet}.
     *
     * @param request which asset, and how much
     * @param options per-call options
     * @return a future of what was credited
     */
    public CompletableFuture<FaucetResult> faucet(
            SandboxFaucetRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_SANDBOX_FAUCET, request, options, FaucetResult.class);
    }

    /**
     * {@code POST /v1/sandbox/deposit} — simulates an on-chain deposit to an invoice. Repeat the
     * call with the same {@code txid} to add confirmations to it.
     *
     * @param request which invoice, how much, and under which txid
     * @return a future of the invoice as the simulated deposit left it
     */
    public CompletableFuture<SandboxDeposit> deposit(SandboxDepositRequest request) {
        return deposit(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/deposit}.
     *
     * @param request which invoice, how much, and under which txid
     * @param options per-call options
     * @return a future of the invoice as the simulated deposit left it
     */
    public CompletableFuture<SandboxDeposit> deposit(
            SandboxDepositRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_SANDBOX_DEPOSIT, request, options, SandboxDeposit.class);
    }

    /**
     * {@code GET /v1/sandbox/webhooks} — the deliveries the sandbox made, with the payloads it
     * sent.
     *
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> webhooks() {
        return webhooks(null, null, RequestOptions.none());
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param limit page size, or null for the gateway's default
     * @param offset where to start, or null for the beginning
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> webhooks(Integer limit, Integer offset) {
        return webhooks(limit, offset, RequestOptions.none());
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param options per-call options
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> webhooks(RequestOptions options) {
        return webhooks(null, null, options);
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param limit page size, or null for the gateway's default
     * @param offset where to start, or null for the beginning
     * @param options per-call options
     * @return a lazy non-blocking pager over the deliveries
     */
    public AsyncPager<WebhookDelivery> webhooks(
            Integer limit, Integer offset, RequestOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", limit);
        if (offset != null) params.put("offset", offset);
        return pagerAsync(Routes.GET_V1_SANDBOX_WEBHOOKS, params, options, WebhookDelivery.class);
    }

    /**
     * {@code POST /v1/sandbox/webhooks/replay} — re-sends a delivery that has already reached a
     * terminal state (delivered or dead).
     *
     * @param deliveryId the delivery to replay
     * @return a future of the replayed delivery
     */
    public CompletableFuture<SandboxReplay> replay(String deliveryId) {
        return replay(deliveryId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/webhooks/replay}.
     *
     * @param deliveryId the delivery to replay
     * @param options per-call options
     * @return a future of the replayed delivery
     */
    public CompletableFuture<SandboxReplay> replay(String deliveryId, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_SANDBOX_WEBHOOKS_REPLAY,
                new SandboxWebhooksReplayRequest().deliveryId(deliveryId),
                options,
                SandboxReplay.class);
    }

    /**
     * {@code POST /v1/sandbox/reset} — cancels the open invoices and zeroes the balances, leaving
     * a clean store behind.
     *
     * @return a future of what was cleared
     */
    public CompletableFuture<SandboxReset> reset() {
        return reset(RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/reset}.
     *
     * @param options per-call options
     * @return a future of what was cleared
     */
    public CompletableFuture<SandboxReset> reset(RequestOptions options) {
        return callAsync(Routes.POST_V1_SANDBOX_RESET, null, options, SandboxReset.class);
    }
}
