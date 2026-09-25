package com.oblodai.core;

/**
 * One API route, as the generated route table ({@code com.oblodai.generated.Routes}) declares it.
 * The transport reads every decision about a call from here: whether it is signed, whether the
 * gateway deduplicates it, whether re-sending it is safe, and what its answer looks like.
 *
 * @param operationId the OpenAPI {@code operationId}, e.g. {@code createPayment}
 * @param method HTTP method
 * @param path path template, e.g. {@code /v1/pay/{id}}
 * @param auth {@code public} (unsigned), {@code key} (signed with the API key) or {@code onboard}
 *     (the admin token of a self-hosted gateway)
 * @param idempotent the gateway deduplicates this route by {@value Signing#HEADER_IDEMPOTENCY_KEY}
 * @param safe re-sending without a key cannot duplicate a side effect
 * @param bare the answer is a file, not a JSON envelope
 * @param listKind {@code "paged"} for an {@code {items, paginate}} list, else {@code null}
 */
public record RouteSpec(
        String operationId,
        String method,
        String path,
        String auth,
        boolean idempotent,
        boolean safe,
        boolean bare,
        String listKind) {

    /** Routes nothing needs to sign. */
    public static final String AUTH_PUBLIC = "public";

    /** Routes signed with the merchant's API key. */
    public static final String AUTH_KEY = "key";

    /** Merchant provisioning, sent with the admin token. */
    public static final String AUTH_ONBOARD = "onboard";

    /** The one list shape the SDK pages through. */
    public static final String LIST_PAGED = "paged";

    /** @return {@code METHOD /path}, for messages and logs */
    public String label() {
        return method + " " + path;
    }

    /** @return whether the answer is an {@code {items, paginate}} list */
    public boolean paged() {
        return LIST_PAGED.equals(listKind);
    }
}
