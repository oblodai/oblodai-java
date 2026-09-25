package com.oblodai.webhooks;

import com.oblodai.core.Redaction;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.WebhookKinds;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A verified webhook delivery body. The accessors here read the fields every kind carries ({@code
 * sequence}, {@code is_final}, {@code test}: the generator refuses a contract whose webhook body
 * lacks one), the object's id ({@link #objectId()}) and a few fields most kinds carry (null where a
 * kind has none). The full, typed event is the generated model of its kind: {@link #typed()} for any kind of the contract, or the
 * accessor of one kind, {@code as<Kind>()} (e.g. {@code asPayment()}), generated per kind in {@link
 * WebhookKinds}. Which kinds exist and which model each carries is the contract's ({@link
 * Facts#WEBHOOK_KINDS}, generated).
 *
 * <p>A {@code type} this SDK does not know is not an error: the event is returned with its raw
 * {@code type} and {@link #fields()}, so a receiver built against an older SDK still sees the
 * delivery ({@link WebhookVerifier#isKnownEvent} tells the two apart).
 */
public final class WebhookEvent implements WebhookKinds {

    /** The {@code type} discriminators this SDK models: {@link Facts#KNOWN_WEBHOOK_KINDS}. */
    public static final List<String> KNOWN_KINDS = Facts.KNOWN_WEBHOOK_KINDS;

    private final Map<String, Object> fields;

    /**
     * @param fields the body as a JSON tree; it must carry a string {@code type}
     */
    public WebhookEvent(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(fields);
    }

    /** @return a kind of {@link Facts#KNOWN_WEBHOOK_KINDS}, or a newer kind this SDK does not know */
    public String type() {
        return (String) fields.get("type");
    }

    /**
     * The id of the object the event is about: the body field its kind names ({@link
     * Facts.WebhookKind#idField()}, as the contract declares it). Key per-object state — the last
     * {@link #sequence()} — on it together with {@link #type()}.
     *
     * @return the object's id; null for a kind this SDK does not know (which field identifies its
     *     object is not guessed), or a body without a string there
     */
    public String objectId() {
        Facts.WebhookKind kind = Facts.WEBHOOK_KINDS.get(type());
        String field = kind == null ? null : kind.idField();
        return field != null && fields.get(field) instanceof String s ? s : null;
    }

    /**
     * The body's {@code uuid} field, kept for compatibility. Not every kind names its object by
     * {@code uuid}: use {@link #objectId()} for the id of the object of any kind.
     *
     * @return the {@code uuid} field, or null when the body has none
     */
    public String uuid() {
        return fields.get("uuid") instanceof String s ? s : null;
    }

    /**
     * @return the merchant reference where the kind's body has one (see its model in {@link
     *     Facts#WEBHOOK_KINDS}), else null
     */
    public String orderId() {
        return fields.get("order_id") instanceof String s ? s : null;
    }

    /** @return true once the status can no longer change, null when absent */
    public Boolean isFinal() {
        return fields.get("is_final") instanceof Boolean b ? b : null;
    }

    /** @return when the state change committed (RFC 3339), or null */
    public String eventAt() {
        return fields.get("event_at") instanceof String s ? s : null;
    }

    /**
     * @return the global, increasing sequence (gaps are normal); a lower sequence arriving later is
     *     stale. Null when absent
     */
    public Long sequence() {
        return fields.get("sequence") instanceof Number n ? n.longValue() : null;
    }

    /** @return the on-chain transaction id, or null */
    public String txid() {
        return fields.get("txid") instanceof String s ? s : null;
    }

    /**
     * @return true ONLY on rehearsal deliveries (test webhooks, sandbox): signed exactly like live
     *     ones, but no money moved
     */
    public boolean test() {
        return Boolean.TRUE.equals(fields.get("test"));
    }

    /** @return the whole body, as received */
    public Map<String, Object> fields() {
        return fields;
    }

    /**
     * @return the event as the generated model of its kind ({@link Facts#WEBHOOK_KINDS}), or null
     *     for a kind this SDK does not know
     * @throws WebhookPayloadException when the body does not match the model of its kind
     */
    public Object typed() {
        Facts.WebhookKind kind = Facts.WEBHOOK_KINDS.get(type());
        return kind == null ? null : parse(kind.kind(), kind.parse());
    }

    /**
     * @param model the generated model of a kind ({@link Facts.WebhookKind#model()})
     * @param <T> the model
     * @return the event as that model
     * @throws IllegalStateException when the event is of another kind
     * @throws WebhookPayloadException when the body does not match the model
     */
    @Override
    public <T> T as(Class<T> model) {
        String kind = Facts.kindOf(model);
        if (kind == null) {
            throw new IllegalArgumentException(model.getName() + " is not the model of a webhook kind");
        }
        if (!kind.equals(type())) {
            throw new IllegalStateException("a " + type() + " event is not a " + kind + " event");
        }
        return model.cast(parse(kind, Facts.WEBHOOK_KINDS.get(kind).parse()));
    }

    private Object parse(String kind, Function<Object, ?> parse) {
        try {
            return parse.apply(fields);
        } catch (IllegalArgumentException e) {
            throw new WebhookPayloadException(
                    "the " + kind + " event does not match the contract (" + e.getMessage() + ")");
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WebhookEvent that && fields.equals(that.fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public String toString() {
        return "WebhookEvent(type=" + type() + ", objectId=" + objectId() + ", fields=" + Redaction.redact(fields) + ")";
    }
}
