package com.oblodai.contract;

/**
 * Which credential the gateway's gate expects on a route. Mirrors the constants of its API
 * conformance table.
 */
public enum RouteAuth {
    /** No credentials: payer-facing and catalog routes. */
    PUBLIC,
    /** The payment key pair. */
    PAYMENT,
    /** The payout key pair (money-out routes); falls back to the payment pair when none is set. */
    PAYOUT,
    /** Either key kind is accepted. */
    ANY,
    /** Merchant provisioning: unsigned, gated by an admin token on a self-hosted gateway. */
    ONBOARD
}
