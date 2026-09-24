package com.oblodai.kotlin

import com.oblodai.webhooks.WebhookEvent
import com.oblodai.webhooks.WebhookHeaders
import com.oblodai.webhooks.WebhookVerifier

/**
 * Verifies a webhook delivery whose headers arrived as a plain map - the shape most Kotlin web
 * frameworks hand you. Verify over the RAW bytes; a re-serialized body will not match.
 *
 * @param headers the request headers, matched case-insensitively
 * @param secret the endpoint secret
 * @param previousSecret the outgoing secret during a rotation overlap
 */
public fun ByteArray.verifyWebhook(
    headers: Map<String, String>,
    secret: String,
    previousSecret: String? = null,
): WebhookEvent {
    var options = WebhookVerifier.options(secret)
    if (previousSecret != null) options = options.previousSecret(previousSecret)
    return WebhookVerifier.verify(this, WebhookHeaders.of(headers), options)
}
