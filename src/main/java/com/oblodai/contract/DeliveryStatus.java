// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * State of one webhook delivery.
 *
 * <p>An open vocabulary. The values below are the ones this contract snapshot knows, and they are
 * interned: {@code status == DeliveryStatus.PENDING} works. A value the
 * gateway starts sending that is not among them decodes to an instance carrying that exact
 * string — {@link #wire()} tells you what it was, {@link #isKnown()} that it is new — so a
 * gateway that grows its vocabulary neither breaks a deployed client nor hides what it said.
 */
public final class DeliveryStatus implements Vocabulary {

    /** {@code pending} */
    public static final DeliveryStatus PENDING = new DeliveryStatus("pending");

    /** {@code delivered} */
    public static final DeliveryStatus DELIVERED = new DeliveryStatus("delivered");

    /** {@code dead} */
    public static final DeliveryStatus DEAD = new DeliveryStatus("dead");

    private static final Map<String, DeliveryStatus> KNOWN = new LinkedHashMap<>();

    /** Every value this snapshot knows, in the gateway's own order. */
    public static final List<DeliveryStatus> VALUES = List.of(PENDING, DELIVERED, DEAD);

    static {
        for (DeliveryStatus value : VALUES) KNOWN.put(value.wire, value);
    }

    private final String wire;

    private DeliveryStatus(String wire) {
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
    public static DeliveryStatus of(String wire) {
        if (wire == null) return null;
        DeliveryStatus known = KNOWN.get(wire);
        return known != null ? known : new DeliveryStatus(wire);
    }

    /**
     * Alias of {@link #of(String)}.
     *
     * @param wire the string the API sent
     * @return the value
     */
    public static DeliveryStatus from(String wire) {
        return of(wire);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeliveryStatus value && value.wire.equals(wire);
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
