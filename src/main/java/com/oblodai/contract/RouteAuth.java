package com.oblodai.contract;

/**
 * Which credential the gateway's gate expects on a route. Mirrors the constants of its API
 * conformance table.
 */
public enum RouteAuth {
    /** No credentials: payer-facing and catalog routes. */
    PUBLIC,
    /** Signed with the merchant's API key — the one pair every signed route uses. */
    KEY,
    /** Merchant provisioning: unsigned, gated by an admin token on a self-hosted gateway. */
    ONBOARD
}
