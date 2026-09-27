package com.oblodai.webhooks;

import com.oblodai.generated.SigningProtocol;

/**
 * A verified delivery: the event, its signed dedupe key, and the delivery headers. The header names
 * are the contract's, from the generated {@link SigningProtocol}.
 *
 * <p>Only {@code event}, {@code eventKey}, {@code isTest} and {@code sentAt} are covered by the
 * signature. The {@code unverified*} components are copied from headers the gateway does not sign:
 * anyone replaying a captured delivery can change them. Use them for logging and correlation, never
 * for deduplication or for deciding whether money moved.
 *
 * @param event the parsed event, verified against the raw bytes it arrived as
 * @param eventKey the dedupe key, from the signed body only ({@link WebhookEvent#eventKey()}: its
 *     {@code event_id}, else {@code type:id:sequence}); identical for every retry and resend of one
 *     state
 * @param unverifiedDeliveryId {@value SigningProtocol#HEADER_WEBHOOK_ID}, NOT signed
 * @param unverifiedEventId {@value SigningProtocol#HEADER_WEBHOOK_EVENT_ID}, NOT signed; dedupe on
 *     {@code eventKey} instead
 * @param unverifiedEventType {@value SigningProtocol#HEADER_WEBHOOK_EVENT}, NOT signed
 * @param unverifiedEventTime {@value SigningProtocol#HEADER_WEBHOOK_EVENT_TIME}, NOT signed
 * @param unverifiedTestHeader {@value WebhookVerifier#HEADER_TEST} was {@code true}, NOT signed;
 *     never decide on it, read {@code isTest}
 * @param sentAt {@value SigningProtocol#HEADER_WEBHOOK_TIMESTAMP} — unix seconds when this attempt
 *     was sent (signed)
 * @param isTest a rehearsal delivery ({@code test: true} in the signed body): signed exactly like a
 *     live one, but no money moved — acknowledge it and do nothing
 */
public record WebhookDeliveryInfo(
        WebhookEvent event,
        String eventKey,
        String unverifiedDeliveryId,
        String unverifiedEventId,
        String unverifiedEventType,
        Long unverifiedEventTime,
        boolean unverifiedTestHeader,
        long sentAt,
        boolean isTest) {}
