package com.oblodai.contract;

/**
 * One route of the gateway's merchant surface, as its conformance table declares it.
 *
 * @param method HTTP method, upper case
 * @param path path template; {@code {name}} segments are filled from path parameters
 * @param auth which credential the gateway's gate expects
 * @param idempotent wrapped in the gateway's idempotency layer: an {@code Idempotency-Key} is
 *     generated when the caller supplies none, and a re-send is deduplicated
 * @param safe read-only: a transport failure may be retried without risking a duplicate side effect
 * @param bare answers outside the JSON envelope (binary documents)
 * @param list how the route paginates, or {@code null} when it is not a list
 */
public record RouteSpec(
        String method,
        String path,
        RouteAuth auth,
        boolean idempotent,
        boolean safe,
        boolean bare,
        ListKind list) {

    /** {@code "POST /v1/payment"} — the key the contract snapshot uses. */
    public String key() {
        return method + " " + path;
    }

    @Override
    public String toString() {
        return key();
    }
}
