package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oblodai.contract.Network;

/**
 * {@code /v1/split/rule} and {@code /v1/split/rule/list} items — a standing share of every payment
 * routed to a partner. {@code POST /v1/split/rule} answers with the id and percent only.
 *
 * @param ruleId the rule's identifier
 * @param percent share of every payment, in percent, as a decimal string
 * @param active whether the rule is applied right now
 * @param address the off-platform address the share is sent to
 * @param network the network the share is sent on
 * @param merchantId set for on-platform partner rules (reversible on refund)
 * @param note the merchant's own note on the rule
 * @param reversible whether the share is clawed back when the payment is refunded
 */
public record SplitRule(
        @JsonProperty("rule_id") String ruleId,
        @JsonProperty("percent") String percent,
        @JsonProperty("active") Boolean active,
        @JsonProperty("address") String address,
        @JsonProperty("network") Network network,
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("note") String note,
        @JsonProperty("reversible") Boolean reversible) {}
