// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Error families behind the HTTP statuses the gateway returns.
 *
 * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than failing, so a
 * gateway that grows its vocabulary cannot break a deployed client.
 */
public enum ErrorKind {

    /** {@code invalid} */
    INVALID("invalid"),

    /** {@code unauthorized} */
    UNAUTHORIZED("unauthorized"),

    /** {@code forbidden} */
    FORBIDDEN("forbidden"),

    /** {@code not_found} */
    NOT_FOUND("not_found"),

    /** {@code conflict} */
    CONFLICT("conflict"),

    /** {@code rate_limited} */
    RATE_LIMITED("rate_limited"),

    /** {@code unavailable} */
    UNAVAILABLE("unavailable"),

    /** {@code internal} */
    INTERNAL("internal"),

    /** A value outside this snapshot's vocabulary. Serializes as an empty string. */
    UNKNOWN("");

    private final String wire;

    ErrorKind(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    public String wire() {
        return wire;
    }

    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */
    @JsonCreator
    public static ErrorKind from(String wire) {
        if (wire == null) return null;
        for (ErrorKind value : values()) {
            if (value.wire.equals(wire)) return value;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wire;
    }
}
