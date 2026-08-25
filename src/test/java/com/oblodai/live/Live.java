package com.oblodai.live;

import com.oblodai.Oblodai;
import com.oblodai.contract.requests.MerchantsRequest;
import com.oblodai.models.SandboxStore;

/**
 * Wiring for the live tests: where the gateway is, and how to get a sandbox key from it.
 *
 * <p>Onboarding uses the SDK's own unsigned provisioning routes, so the journey starts exactly where
 * a new integrator starts — with no credentials at all.
 */
final class Live {

    private Live() {}

    /** The gateway under test; the live tests are skipped when this is unset. */
    static String baseUrl() {
        String url = System.getenv("OBLODAI_LIVE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OBLODAI_LIVE_URL is not set");
        }
        return url;
    }

    /** A receiver the gateway can deliver webhooks to, when the stand has one. */
    static String hookUrl() {
        String url = System.getenv("OBLODAI_LIVE_HOOK_URL");
        return url == null || url.isBlank() ? "http://127.0.0.1:8096/hook" : url;
    }

    /** A client with no credentials, for the payer-facing and catalog routes. */
    static Oblodai publicClient() {
        return Oblodai.builder().baseUrl(baseUrl()).allowInsecureBaseUrl(true).build();
    }

    /**
     * Onboards a fresh merchant, opens its sandbox store and returns a client holding the sandbox
     * key. A sandbox key is both key kinds at once, so one client can drive the whole journey.
     *
     * @param prefix a label that ends up in the merchant's email address
     * @return a client for the new sandbox store
     */
    static Oblodai sandboxClient(String prefix) {
        Oblodai provisioning = publicClient();
        String merchantId =
                provisioning
                        .merchants()
                        .create(
                                new MerchantsRequest()
                                        .email(prefix + "-" + System.currentTimeMillis() + "@example.com")
                                        .name("SDK live"))
                        .merchantId();
        SandboxStore sandbox = provisioning.merchants().createSandbox(merchantId);
        return Oblodai.builder()
                .baseUrl(baseUrl())
                .allowInsecureBaseUrl(true)
                .publicId(sandbox.apiKey().publicId())
                .secret(sandbox.apiKey().secret())
                .build();
    }
}
