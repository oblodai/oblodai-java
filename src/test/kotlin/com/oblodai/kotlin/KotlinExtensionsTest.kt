package com.oblodai.kotlin

import com.oblodai.Oblodai
import com.oblodai.contract.Network
import com.oblodai.errors.NotFoundException
import com.oblodai.support.MockHttpClient
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The Kotlin half: the same client, with coroutines and builders on top. */
class KotlinExtensionsTest {

    private fun client(http: MockHttpClient): Oblodai =
        Oblodai.builder()
            .publicId("pk")
            .secret("s")
            .baseUrl("https://api.test")
            .httpClient(http)
            .environment(emptyMap())
            .build()

    @Test
    fun `the request DSL builds the same body the fluent setters do`() {
        val http = MockHttpClient().ok("""{"uuid":"u","order_id":"order-1001"}""")
        val invoice =
            client(http)
                .payments()
                .create(
                    payment {
                        amount("25")
                        currency("USDT")
                        network(Network.TRON)
                        orderId("order-1001")
                    }
                )

        assertEquals("u", invoice.uuid())
        val body = http.onlyCall().body()
        assertTrue(body.contains("\"amount\":\"25\""), body)
        assertTrue(body.contains("\"network\":\"tron\""), body)
        assertTrue(body.contains("\"order_id\":\"order-1001\""), body)
    }

    @Test
    fun `await suspends and unwraps the SDK's own exception`() = runBlocking {
        val http =
            MockHttpClient()
                .ok("""{"balance":{"merchant":[]}}""")
                .apiError(404, """{"code":"payment.not_found","retryable":false}""")
        val async = client(http).async()

        assertEquals(0, async.account().balance().await().balance().merchant().size)

        val error = assertFailsWith<NotFoundException> { async.payments().info("missing").await() }
        assertEquals("payment.not_found", error.code())
    }

    @Test
    fun `a pager becomes a flow and a sequence`() = runBlocking {
        fun page(items: String, offset: Int, more: Boolean) =
            """{"items":$items,"paginate":{"total":3,"per_page":2,"offset":$offset,"has_pages":$more}}"""

        val http =
            MockHttpClient()
                .ok(page("""[{"uuid":"a"},{"uuid":"b"}]""", 0, true))
                .ok(page("""[{"uuid":"c"}]""", 2, false))
        val uuids =
            client(http).async().payments().history().asFlow().toList().map { it.uuid() }
        assertEquals(listOf("a", "b", "c"), uuids)

        val blocking =
            MockHttpClient().ok(page("""[{"uuid":"a"},{"uuid":"b"}]""", 0, false))
        assertEquals(
            listOf("a", "b"),
            client(blocking).payments().history().asSequence().map { it.uuid() }.toList(),
        )
    }

    @Test
    fun `a flow delivers every item of a large result set, not a bufferful`() = runBlocking {
        // Five pages of two hundred. The previous callbackFlow implementation handed items to a
        // 64-slot channel with trySend and dropped everything that did not fit.
        val http = MockHttpClient()
        var uuid = 0
        repeat(5) { pageIndex ->
            val items = (1..200).joinToString(",") { """{"uuid":"u${uuid++}"}""" }
            http.ok(
                """{"items":[$items],"paginate":{"total":1000,"per_page":200,""" +
                    """"offset":${pageIndex * 200},"has_pages":${pageIndex < 4}}}"""
            )
        }

        val collected = client(http).async().payments().history().asFlow().toList()

        assertEquals(1000, collected.size)
        assertEquals("u0", collected.first().uuid())
        assertEquals("u999", collected.last().uuid())
        assertEquals(5, http.calls().size, "one request per page, pulled by the collector")
    }

    @Test
    fun `a flow stops walking when the collector stops collecting`() = runBlocking {
        val http = MockHttpClient()
        repeat(2) { pageIndex ->
            val items = (1..10).joinToString(",") { """{"uuid":"u$it"}""" }
            http.ok(
                """{"items":[$items],"paginate":{"total":100,"per_page":10,""" +
                    """"offset":${pageIndex * 10},"has_pages":true}}"""
            )
        }

        val firstThree = client(http).async().payments().history().asFlow().take(3).toList()

        assertEquals(3, firstThree.size)
        assertEquals(1, http.calls().size, "a cancelled collection asks for no further page")
    }

    @Test
    fun `a webhook verifies from a plain header map`() {
        val secret = "whsec"
        // A fresh timestamp: the default ±300 s freshness window applies, as it would in production.
        val timestamp = System.currentTimeMillis() / 1000
        val body =
            """{"type":"payment","uuid":"u1","order_id":"o","status":"paid","is_final":true,""" +
                """"sequence":7,"event_at":"2026-01-01T00:00:00Z"}"""
        val headers =
            mapOf(
                "X-Webhook-Timestamp" to timestamp.toString(),
                "X-Webhook-Signature" to
                    com.oblodai.core.Signing.signWebhook(secret, timestamp, body),
            )

        val event =
            com.oblodai.webhooks.WebhookVerifier.verify(
                body.toByteArray(),
                com.oblodai.webhooks.WebhookHeaders.of(headers),
                com.oblodai.webhooks.WebhookVerifier.options(secret),
            )
        assertEquals("payment", event.type())
        assertEquals("u1", body.toByteArray().verifyWebhook(headers, secret).uuid())
    }
}
