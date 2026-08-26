package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An API key pair as minted by onboarding. The secret is shown once.
 *
 * @param publicId the public identifier of the key, safe to store and to log
 * @param secret the secret half of the pair; it is shown once, at minting, and never again.
 *     Readable here, redacted in {@code toString()} and in JSON
 * @param kind {@code api} — the unified key kind current merchants receive
 */
public record ApiKeyPair(
        @JsonProperty("public_id") String publicId,
        @JsonProperty("secret") @JsonSerialize(using = Secrets.RedactingSerializer.class)
                String secret,
        @JsonProperty("kind") String kind) {

    @Override
    public String toString() {
        return "ApiKeyPair[publicId=" + publicId + ", secret=" + Secrets.describe(secret) + ", kind="
                + kind + "]";
    }
}
