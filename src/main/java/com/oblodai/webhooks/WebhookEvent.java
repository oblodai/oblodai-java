package com.oblodai.webhooks;

import com.oblodai.core.Redaction;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.models.ConversionWebhook;
import com.oblodai.generated.models.PaymentWebhook;
import com.oblodai.generated.models.PayoutWebhook;
import com.oblodai.generated.models.WalletWebhook;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A verified webhook delivery body. The fields events carry have accessors here; the full, typed
 * event is the generated model of its kind: {@link #typed()} for any kind of the contract, or
 * {@link #asPayment()} for {@code type=payment}, {@link #asPayout()}, {@link #asWallet()}, {@link
 * #asConversion()}. Which kinds exist and which model each carries is the contract's ({@link
 * Facts#WEBHOOK_KINDS}, generated).
 *
 * <p>A {@code type} this SDK does not know is not an error: the event is returned with its raw
 * {@code type} and {@link #fields()}, so a receiver built against an older SDK still sees the
 * delivery ({@link WebhookVerifier#isKnownEvent} tells the two apart).
 */
public final class WebhookEvent {

    /** The {@code type} discriminators this SDK models: {@link Facts#KNOWN_WEBHOOK_KINDS}. */
    public static final List<String> KNOWN_KINDS = Facts.KNOWN_WEBHOOK_KINDS;

    private final Map<String, Object> fields;

    /**
     * @param fields the body as a JSON tree; it must carry a string {@code type}
     */
    public WebhookEvent(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(fields);
    }

    /** @return {@code payment}, {@code payout}, {@code wallet}, {@code conversion} or a newer kind */
    public String type() {
        return (String) fields.get("type");
    }

    /**
     * @return the identifier of the invoice, payout or wallet the event is about; null for a kind
     *     keyed otherwise (a conversion carries {@code id})
     */
    public String uuid() {
        return fields.get("uuid") instanceof String s ? s : null;
    }

    /** @return the merchant reference, or null */
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

    /** @return the typed payment event ({@code type=payment}) */
    public PaymentWebhook asPayment() {
        return as(PaymentWebhook.class);
    }

    /** @return the typed payout event ({@code type=payout}) */
    public PayoutWebhook asPayout() {
        return as(PayoutWebhook.class);
    }

    /** @return the typed wallet event ({@code type=wallet}) */
    public WalletWebhook asWallet() {
        return as(WalletWebhook.class);
    }

    /** @return the typed conversion event ({@code type=conversion}) */
    public ConversionWebhook asConversion() {
        return as(ConversionWebhook.class);
    }

    /**
     * @param model the generated model of a kind, e.g. {@code PaymentWebhook.class}
     * @param <T> the model
     * @return the event as that model
     * @throws IllegalStateException when the event is of another kind
     * @throws WebhookPayloadException when the body does not match the model
     */
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
        return "WebhookEvent(type=" + type() + ", uuid=" + uuid() + ", fields=" + Redaction.redact(fields) + ")";
    }
}
