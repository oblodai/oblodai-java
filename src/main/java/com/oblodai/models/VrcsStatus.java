package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/vrcs} — volatility risk control: auto-convert volatile deposits to USDT.
 *
 * @param enabled whether the control is on
 */
public record VrcsStatus(@JsonProperty("enabled") Boolean enabled) {}
