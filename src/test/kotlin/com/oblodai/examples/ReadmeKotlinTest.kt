package com.oblodai.examples

import com.oblodai.support.Clients
import com.oblodai.support.Fixtures
import com.oblodai.support.MockHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Kotlin README block and the Kotlin example run against a scripted gateway. */
class ReadmeKotlinTest {

    @Test
    fun `the Kotlin README block runs`() = runBlocking {
        val http =
            MockHttpClient()
                .ok(Fixtures.payment("k1"))
                .ok(Fixtures.page(Fixtures.payments("a", "b"), 0, false))
        assertEquals(2, readmeKotlin(Clients.client(http)))
    }

    @Test
    fun `the Kotlin example runs`() = runBlocking {
        val http =
            MockHttpClient()
                .ok(Fixtures.payment("k1"))
                .ok(Fixtures.page(Fixtures.payments("a"), 0, false))
        val line = acceptPayment(Clients.client(http).async())
        assertTrue(line.startsWith("invoice k1 at https://pay.example/k1; last 1: created"), line)
    }
}
