package com.oblodai.examples

import com.oblodai.Oblodai
import com.oblodai.OblodaiAsync
import com.oblodai.errors.OblodaiException
import com.oblodai.generated.models.PaymentRequest
import com.oblodai.kotlin.asFlow
import com.oblodai.kotlin.await
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * The same journey in Kotlin. There is no separate Kotlin SDK: these are extensions on the Java
 * client, so `await()` and `asFlow()` work against the very same types.
 *
 * ```
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.AcceptPaymentKotlinKt
 * ```
 */
fun main() = runBlocking {
    Oblodai.builder().build().use { client -> println(acceptPayment(client.async())) }
}

/** Creates an invoice and lists the latest ones; returns what happened, one line. */
suspend fun acceptPayment(oblodai: OblodaiAsync): String =
    try {
        val invoice =
            oblodai
                .payments()
                .create(
                    PaymentRequest.builder()
                        .amount("25")
                        .currency("USDT")
                        .orderId("order-1001")
                        .build()
                )
                .await()

        // Pages arrive as a cold flow: one request per page, and cancelling stops the walk.
        val recent = oblodai.payments().listHistory().asFlow().take(5).toList()
        "invoice ${invoice.uuid()} at ${invoice.url()}; last ${recent.size}: " +
            recent.joinToString { it.status().value() }
    } catch (e: OblodaiException) {
        "refused: ${e.message}"
    }
