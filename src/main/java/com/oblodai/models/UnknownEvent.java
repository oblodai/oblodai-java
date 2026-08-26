package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An event whose {@code type} this SDK snapshot does not know.
 *
 * <p>A gateway that starts delivering a new kind of event must not break a deployed receiver, so an
 * unrecognised {@code type} decodes to this instead of throwing: the raw discriminator is on
 * {@link #type()}, the fields every event carries are decoded as usual, and anything else stays
 * available under {@link #fields()}. {@code isTestEvent} and {@code isStale} work on it like on any
 * other event.
 */
public final class UnknownEvent implements WebhookEvent {

    private final String type;
    private final String uuid;
    private final String orderId;
    private final Boolean isFinal;
    private final String eventAt;
    private final Long sequence;
    private final String txid;
    private final Boolean test;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    /**
     * @param type the raw discriminator exactly as it arrived
     * @param uuid the identifier of the object the event is about
     * @param orderId merchant reference, when the event carries one
     * @param isFinal true once the status can no longer change
     * @param eventAt when the state change was committed, RFC 3339 UTC
     * @param sequence global, increasing sequence number of the event
     * @param txid the on-chain transaction id, when the event carries one
     * @param test true ONLY on a rehearsal delivery
     */
    public UnknownEvent(
            @JsonProperty("type") String type,
            @JsonProperty("uuid") String uuid,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("is_final") Boolean isFinal,
            @JsonProperty("event_at") String eventAt,
            @JsonProperty("sequence") Long sequence,
            @JsonProperty("txid") String txid,
            @JsonProperty("test") Boolean test) {
        this.type = type;
        this.uuid = uuid;
        this.orderId = orderId;
        this.isFinal = isFinal;
        this.eventAt = eventAt;
        this.sequence = sequence;
        this.txid = txid;
        this.test = test;
    }

    @Override
    @JsonProperty("type")
    public String type() {
        return type;
    }

    @Override
    @JsonProperty("uuid")
    public String uuid() {
        return uuid;
    }

    @Override
    @JsonProperty("order_id")
    public String orderId() {
        return orderId;
    }

    @Override
    @JsonProperty("is_final")
    public Boolean isFinal() {
        return isFinal;
    }

    @Override
    @JsonProperty("event_at")
    public String eventAt() {
        return eventAt;
    }

    @Override
    @JsonProperty("sequence")
    public Long sequence() {
        return sequence;
    }

    @Override
    @JsonProperty("txid")
    public String txid() {
        return txid;
    }

    @Override
    @JsonProperty("test")
    public Boolean test() {
        return test;
    }

    /** Every field of the delivered body this snapshot has no accessor for. */
    @JsonAnyGetter
    public Map<String, Object> fields() {
        return fields;
    }

    @JsonAnySetter
    void put(String name, Object value) {
        fields.put(name, value);
    }

    @Override
    @JsonIgnore
    public String toString() {
        return "UnknownEvent[type=" + type + ", uuid=" + uuid + ", sequence=" + sequence + "]";
    }
}
