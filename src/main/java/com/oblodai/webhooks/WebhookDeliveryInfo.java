package com.oblodai.webhooks;

/**
 * A verified delivery: the event, plus the advisory headers worth keeping.
 *
 * @param event the parsed event, verified against the raw bytes it arrived as
 * @param id {@code X-Webhook-Id} - stable across retries of the same DELIVERY; a resend of the
 *     same state is a new delivery with a new id, so deduplicate on {@code eventId}
 * @param eventId {@code X-Webhook-Event-Id} - the id of the STATE this delivery carries: the same
 *     for the original, every retry and every resend of that state; keep the ids you handled and
 *     skip repeats. Null from a gateway that does not send it
 * @param eventType {@code X-Webhook-Event} — the event name, one of the events of a kind in {@link
 *     com.oblodai.generated.Facts#WEBHOOK_KINDS}
 * @param eventTime {@code X-Webhook-Event-Time} — unix seconds when the state change committed
 * @param sentAt {@code X-Webhook-Timestamp} — unix seconds when this attempt was sent
 * @param isTest a rehearsal delivery ({@code X-Webhook-Test: true}, or {@code test: true} in the
 *     signed body): signed exactly like a live one, but no money moved — never act on it
 */
public record WebhookDeliveryInfo(
        WebhookEvent event,
        String id,
        String eventId,
        String eventType,
        Long eventTime,
        long sentAt,
        boolean isTest) {}
