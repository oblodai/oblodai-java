package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.SandboxDepositRequest;
import com.oblodai.contract.requests.SandboxFaucetRequest;
import com.oblodai.contract.requests.SandboxWebhooksReplayRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.FaucetResult;
import com.oblodai.models.SandboxDeposit;
import com.oblodai.models.SandboxReplay;
import com.oblodai.models.SandboxReset;
import com.oblodai.models.WebhookDelivery;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The developer sandbox: fake money, simulated deposits and a webhook inspector. Every route here
 * needs a {@code test_} key — a live key is refused.
 */
public final class Sandbox extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Sandbox(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/sandbox/faucet} — credits test funds to the sandbox balance. Payout key.
     *
     * @param request which asset, and how much
     * @return what was credited
     */
    public FaucetResult faucet(SandboxFaucetRequest request) {
        return faucet(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/faucet}.
     *
     * @param request which asset, and how much
     * @param options per-call options
     * @return what was credited
     */
    public FaucetResult faucet(SandboxFaucetRequest request, RequestOptions options) {
        return call(Routes.POST_V1_SANDBOX_FAUCET, request, options, FaucetResult.class);
    }

    /**
     * {@code POST /v1/sandbox/deposit} — simulates an on-chain deposit to an invoice. Repeat the
     * call with the same {@code txid} to add confirmations to it.
     *
     * @param request which invoice, how much, and under which txid
     * @return the invoice as the simulated deposit left it
     */
    public SandboxDeposit deposit(SandboxDepositRequest request) {
        return deposit(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/deposit}.
     *
     * @param request which invoice, how much, and under which txid
     * @param options per-call options
     * @return the invoice as the simulated deposit left it
     */
    public SandboxDeposit deposit(SandboxDepositRequest request, RequestOptions options) {
        return call(Routes.POST_V1_SANDBOX_DEPOSIT, request, options, SandboxDeposit.class);
    }

    /**
     * {@code GET /v1/sandbox/webhooks} — the deliveries the sandbox made, with the payloads it
     * sent.
     *
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> webhooks() {
        return webhooks(null, null, RequestOptions.none());
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param limit page size, or null for the gateway's default
     * @param offset where to start, or null for the beginning
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> webhooks(Integer limit, Integer offset) {
        return webhooks(limit, offset, RequestOptions.none());
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param options per-call options
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> webhooks(RequestOptions options) {
        return webhooks(null, null, options);
    }

    /**
     * {@code GET /v1/sandbox/webhooks}.
     *
     * @param limit page size, or null for the gateway's default
     * @param offset where to start, or null for the beginning
     * @param options per-call options
     * @return a lazy pager over the deliveries
     */
    public Pager<WebhookDelivery> webhooks(
            Integer limit, Integer offset, RequestOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", limit);
        if (offset != null) params.put("offset", offset);
        return pager(Routes.GET_V1_SANDBOX_WEBHOOKS, params, options, WebhookDelivery.class);
    }

    /**
     * {@code POST /v1/sandbox/webhooks/replay} — re-sends a delivery that has already reached a
     * terminal state (delivered or dead).
     *
     * @param deliveryId the delivery to replay
     * @return the replayed delivery
     */
    public SandboxReplay replay(String deliveryId) {
        return replay(deliveryId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/webhooks/replay}.
     *
     * @param deliveryId the delivery to replay
     * @param options per-call options
     * @return the replayed delivery
     */
    public SandboxReplay replay(String deliveryId, RequestOptions options) {
        return call(
                Routes.POST_V1_SANDBOX_WEBHOOKS_REPLAY,
                new SandboxWebhooksReplayRequest().deliveryId(deliveryId),
                options,
                SandboxReplay.class);
    }

    /**
     * {@code POST /v1/sandbox/reset} — cancels the open invoices and zeroes the balances, leaving
     * a clean store behind. Payout key.
     *
     * @return what was cleared
     */
    public SandboxReset reset() {
        return reset(RequestOptions.none());
    }

    /**
     * {@code POST /v1/sandbox/reset}.
     *
     * @param options per-call options
     * @return what was cleared
     */
    public SandboxReset reset(RequestOptions options) {
        return call(Routes.POST_V1_SANDBOX_RESET, null, options, SandboxReset.class);
    }
}
