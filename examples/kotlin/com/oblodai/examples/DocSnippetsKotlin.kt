package com.oblodai.examples

import com.oblodai.Oblodai
import com.oblodai.contract.Network
import com.oblodai.kotlin.asFlow
import com.oblodai.kotlin.await
import com.oblodai.kotlin.payment
import kotlinx.coroutines.flow.take

/**
 * The Kotlin snippet README.md and README.ru.md show, compiled.
 *
 * Same contract as [DocSnippets] on the Java side: the body opens with the README block, statement
 * for statement, and `DocSnippetsTest` fails the build if the two ever drift apart.
 */
internal suspend fun kotlinQuickStart(id: String, secret: String) {
    val oblodai = Oblodai.builder().publicId(id).secret(secret).build().async()

    val invoice = oblodai.payments().create(payment {
        amount("25"); currency("USDT"); network(Network.TRON); orderId("order-1001")
    }).await()

    oblodai.payments().history().asFlow().take(100).collect { println(it.uuid()) }

    println(invoice.uuid())
}
