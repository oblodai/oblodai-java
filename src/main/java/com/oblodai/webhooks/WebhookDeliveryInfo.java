package com.oblodai.webhooks;

import com.oblodai.generated.SigningProtocol;

/**
 * A verified delivery: the event, plus the advisory headers worth keeping. The header names are
 * the contract's, from the generated {@link SigningProtocol}.
 *
 * @param event the parsed event, verified against the raw bytes it arrived as
 * @param id {@value SigningProtocol#HEADER_WEBHOOK_ID} - stable across retries of the same
 *     DELIVERY; a resend of the same state is a new delivery with a new id, so deduplicate on
 *     {@code eventId}
 * @param eventId {@value SigningProtocol#HEADER_WEBHOOK_EVENT_ID} - the id of the STATE this
 *     delivery carries: the same for the original, every retry and every resend of that state; keep
 *     the ids you handled and skip repeats. Null from a gateway that does not send it
 * @param eventType {@value SigningProtocol#HEADER_WEBHOOK_EVENT} — the event name, one of the
 *     events of a kind in {@link com.oblodai.generated.Facts#WEBHOOK_KINDS}
 * @param eventTime {@value SigningProtocol#HEADER_WEBHOOK_EVENT_TIME} — unix seconds when the state
 *     change committed
 * @param sentAt {@value SigningProtocol#HEADER_WEBHOOK_TIMESTAMP} — unix seconds when this attempt
 *     was sent
 * @param isTest a rehearsal delivery ({@value WebhookVerifier#HEADER_TEST} {@code true}, or {@code
 *     test: true} in the signed body): signed exactly like a live one, but no money moved — never
 *     act on it
 */
public record WebhookDeliveryInfo(
        WebhookEvent event,
        String id,
        String eventId,
        String eventType,
        Long eventTime,
        long sentAt,
        boolean isTest) {}
