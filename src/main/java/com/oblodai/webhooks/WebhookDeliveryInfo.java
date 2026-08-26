package com.oblodai.webhooks;

import com.oblodai.models.WebhookEvent;

/**
 * A verified delivery: the event, plus the advisory headers worth keeping.
 *
 * @param event the parsed event, verified against the raw bytes it arrived as
 * @param id {@code X-Webhook-Id} — stable across retries of the same delivery; use it as your
 *     deduplication key
 * @param eventType {@code X-Webhook-Event} — {@code invoice.&lt;status&gt;}, {@code
 *     payout.&lt;status&gt;} or {@code wallet.paid}
 * @param eventTime {@code X-Webhook-Event-Time} — unix seconds when the state change committed
 * @param sentAt {@code X-Webhook-Timestamp} — unix seconds when this attempt was sent
 * @param isTest a rehearsal delivery ({@code X-Webhook-Test: true}, or {@code test: true} in the
 *     signed body): signed exactly like a live one, but no money moved — never act on it
 */
public record WebhookDeliveryInfo(
        WebhookEvent event,
        String id,
        String eventType,
        Long eventTime,
        long sentAt,
        boolean isTest) {}
