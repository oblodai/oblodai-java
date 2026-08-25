package com.oblodai.core;

/**
 * One API key pair.
 *
 * @param publicId public id, sent as {@code X-Public-Id}
 * @param secret secret the signature is made with; never sent
 */
public record Credentials(String publicId, String secret) {

    @Override
    public String toString() {
        return "Credentials[publicId=" + publicId + ", secret=***]";
    }
}
