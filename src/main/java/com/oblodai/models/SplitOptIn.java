package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/split/recipient/optin*} — whether this merchant accepts being a split recipient.
 *
 * @param enabled whether the merchant accepts on-platform split shares
 */
public record SplitOptIn(@JsonProperty("enabled") Boolean enabled) {}
