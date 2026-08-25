package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/split/config/*} — how long a split share is held before it is released.
 *
 * @param refundHoldSeconds how long the share is withheld so a refund can still claw it back
 */
public record SplitConfig(@JsonProperty("refund_hold_seconds") Integer refundHoldSeconds) {}
