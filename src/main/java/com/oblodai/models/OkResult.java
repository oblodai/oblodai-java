package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The bare acknowledgement a few endpoints answer with when there is nothing else to report.
 *
 * @param ok true when the call took effect
 */
public record OkResult(@JsonProperty("ok") Boolean ok) {}
