// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Error families behind the HTTP statuses the gateway returns.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == ErrorKind.INVALID} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class ErrorKind implements Vocabulary {

    /** {@code invalid} */
    public static final ErrorKind INVALID = new ErrorKind("invalid");

    /** {@code unauthorized} */
    public static final ErrorKind UNAUTHORIZED = new ErrorKind("unauthorized");

    /** {@code forbidden} */
    public static final ErrorKind FORBIDDEN = new ErrorKind("forbidden");

    /** {@code not_found} */
    public static final ErrorKind NOT_FOUND = new ErrorKind("not_found");

    /** {@code conflict} */
    public static final ErrorKind CONFLICT = new ErrorKind("conflict");

    /** {@code rate_limited} */
    public static final ErrorKind RATE_LIMITED = new ErrorKind("rate_limited");

    /** {@code unavailable} */
    public static final ErrorKind UNAVAILABLE = new ErrorKind("unavailable");

    /** {@code internal} */
    public static final ErrorKind INTERNAL = new ErrorKind("internal");

    private static final Map<String, ErrorKind> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<ErrorKind> VALUES = List.of(INVALID, UNAUTHORIZED, FORBIDDEN, NOT_FOUND,
            CONFLICT, RATE_LIMITED, UNAVAILABLE, INTERNAL);

    static {
        for (ErrorKind value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private ErrorKind(String wire) {
        this.wire = wire;
    }

    /** The exact string the API uses. */
    @JsonValue
    @Override
    public String wire() {
        return wire;
    }

    /** Whether this is one of the values this contract snapshot declares. */
    @Override
    public boolean isKnown() {
        return KNOWN.get(wire) == this;
    }

    /**
     * Decodes a wire value. A value outside this snapshot's vocabulary is kept as it arrived,
     * readable through {@link #wire()}.
     *
     * @param wire the string the API sent
     * @return the interned constant, or a new instance carrying the raw value; null for null
     */
    @JsonCreator
    public static ErrorKind of(String wire) {
        if (wire == null) return null;
        ErrorKind known = KNOWN.get(wire);
        return known != null ? known : new ErrorKind(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static ErrorKind from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ErrorKind value && value.wire.equals(wire);
    }

    @Override
    public int hashCode() {
        return wire.hashCode();
    }

    /** The observed wire value — including one this snapshot does not know. */
    @Override
    public String toString() {
        return wire;
    }
}
