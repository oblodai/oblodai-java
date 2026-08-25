package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code /v1/api-allowlist/*} — entries are CIDRs.
 *
 * @param enabled whether the allowlist is enforced
 * @param items the allowed CIDRs
 */
public record ApiAllowlist(
        @JsonProperty("enabled") Boolean enabled, @JsonProperty("items") List<String> items) {}
