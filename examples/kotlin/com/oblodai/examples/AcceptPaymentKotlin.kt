package com.oblodai.examples

import com.oblodai.Oblodai
import com.oblodai.contract.Network
import com.oblodai.errors.OblodaiException
import com.oblodai.kotlin.asFlow
import com.oblodai.kotlin.await
import com.oblodai.kotlin.payment
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * The same journey in Kotlin. There is no separate Kotlin SDK: these are extensions on the Java
 * client, so `await()` and the request builders work against the very same types.
 *
 * ```
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.AcceptPaymentKotlinKt
 * ```
 */
fun main() = runBlocking {
    val oblodai = Oblodai.builder().build().async()

    try {
        val invoice =
            oblodai
                .payments()
                .create(
                    payment {
                        amount("25")
                        currency("USDT")
                        network(Network.TRON)
                        orderId("order-${System.currentTimeMillis()}")
                    }
                )
                .await()

        println("invoice  ${invoice.uuid()}")
        println("pay page ${invoice.url()}")
        println("due      ${invoice.payerAmount()} ${invoice.payerCurrency()}")

        // Pages arrive as a cold flow: one request per page, and cancelling stops the walk.
        val recent = oblodai.payments().history().asFlow().take(5).toList()
        println("last ${recent.size} invoices: " + recent.joinToString { it.status().wire() })
    } catch (e: OblodaiException) {
        System.err.println("refused: ${e.code()} (${e.message}) request ${e.requestId()}")
    }
}
