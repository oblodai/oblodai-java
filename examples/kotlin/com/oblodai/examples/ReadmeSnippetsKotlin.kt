package com.oblodai.examples

import com.oblodai.Oblodai
import com.oblodai.generated.models.PaymentRequest
import com.oblodai.kotlin.asFlow
import com.oblodai.kotlin.await
import kotlinx.coroutines.flow.toList

/** The Kotlin block of README.md and README.ru.md, compiled and run by ReadmeExamplesTest. */
suspend fun readmeKotlin(oblodai: Oblodai): Int {
    val async = oblodai.async()
    val invoice = async.payments().create(
        PaymentRequest.builder().amount("25").currency("USDT").orderId("order-1001").build()
    ).await()                                          // suspends; cancelling cancels the call
    val all = async.payments().listHistory().asFlow().toList()
    println("${invoice.uuid()}: ${all.size} invoices")
    return all.size
}
