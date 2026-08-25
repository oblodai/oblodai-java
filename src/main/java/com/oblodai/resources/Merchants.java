package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.MerchantsRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.MerchantOnboarded;
import com.oblodai.models.SandboxStore;

/**
 * Merchant provisioning, for platforms that onboard their merchants themselves. These routes are
 * not HMAC-signed: a self-hosted gateway gates them with its admin token, which the client takes
 * as its {@code adminToken} option.
 */
public final class Merchants extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Merchants(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/merchants} — creates a merchant and mints its payment and payout keys. The
     * keys are shown once and never again.
     *
     * @param request the merchant to create
     * @return the merchant, with its freshly minted keys
     */
    public MerchantOnboarded create(MerchantsRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/merchants}.
     *
     * @param request the merchant to create
     * @param options per-call options
     * @return the merchant, with its freshly minted keys
     */
    public MerchantOnboarded create(MerchantsRequest request, RequestOptions options) {
        return call(Routes.POST_V1_MERCHANTS, request, options, MerchantOnboarded.class);
    }

    /**
     * {@code POST /v1/merchants/&#123;id&#125;/sandbox} — the merchant's development store and its
     * {@code test_} key. Idempotent: calling it again returns the store that already exists.
     *
     * @param merchantId the merchant to give a sandbox
     * @return the sandbox store and its key
     */
    public SandboxStore createSandbox(String merchantId) {
        return createSandbox(merchantId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/merchants/&#123;id&#125;/sandbox}.
     *
     * @param merchantId the merchant to give a sandbox
     * @param options per-call options
     * @return the sandbox store and its key
     */
    public SandboxStore createSandbox(String merchantId, RequestOptions options) {
        return call(
                Routes.POST_V1_MERCHANTS_ID_SANDBOX,
                options(options).pathParam("id", merchantId),
                SandboxStore.class);
    }
}
