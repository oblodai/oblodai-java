// GENERATED FILE — do not edit. Source: contract/contract.json (core 7ec04293c426).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Who paid the network fee, as reported on a priced result.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == FeeBearerResult.RECIPIENT} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class FeeBearerResult implements Vocabulary {

    /** {@code recipient} */
    public static final FeeBearerResult RECIPIENT = new FeeBearerResult("recipient");

    /** {@code merchant} */
    public static final FeeBearerResult MERCHANT = new FeeBearerResult("merchant");

    /** {@code gateway} */
    public static final FeeBearerResult GATEWAY = new FeeBearerResult("gateway");

    private static final Map<String, FeeBearerResult> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<FeeBearerResult> VALUES = List.of(RECIPIENT, MERCHANT, GATEWAY);

    static {
        for (FeeBearerResult value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private FeeBearerResult(String wire) {
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
    public static FeeBearerResult of(String wire) {
        if (wire == null) return null;
        FeeBearerResult known = KNOWN.get(wire);
        return known != null ? known : new FeeBearerResult(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static FeeBearerResult from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FeeBearerResult value && value.wire.equals(wire);
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
