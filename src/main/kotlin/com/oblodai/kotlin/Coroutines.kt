package com.oblodai.kotlin

import com.oblodai.core.AsyncPager
import com.oblodai.core.Page
import com.oblodai.core.Pager
import com.oblodai.errors.OblodaiException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coroutine ergonomics over the asynchronous client. The Java SDK is the Kotlin SDK: these are
 * extensions on the very same types, so nothing here is a second implementation to keep in step.
 *
 * ```kotlin
 * val oblodai = Oblodai.builder().publicId(id).secret(secret).build()
 * val invoice = oblodai.async().payments().create(payment {
 *     amount("25"); currency("USDT"); network(Network.TRON); orderId("order-1001")
 * }).await()
 * ```
 */

/**
 * Suspends until the call finishes, and throws the SDK's own [OblodaiException] rather than the
 * [CompletionException] wrapper a blocking `join()` would hand you. Cancelling the coroutine
 * cancels the call.
 */
public suspend fun <T> CompletableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        whenComplete { value, failure ->
            when (failure) {
                null -> continuation.resume(value)
                else -> continuation.resumeWithException(failure.unwrapped())
            }
        }
        continuation.invokeOnCancellation { cancel(true) }
    }

private fun Throwable.unwrapped(): Throwable {
    var cause: Throwable = this
    while ((cause is CompletionException || cause is java.util.concurrent.ExecutionException) &&
        cause.cause != null
    ) {
        cause = cause.cause!!
    }
    return cause
}

/** The first page of a list route, without blocking a thread. */
public suspend fun <T> AsyncPager<T>.firstPageAwait(): Page<T> = firstPage().await()

/**
 * Every item of a list route as a cold [Flow], one page fetched at a time.
 *
 * The walk is pull-based: a page is requested only once the collector has consumed the previous one,
 * and `emit` suspends until the collector takes each item. Nothing is buffered, so a slow collector
 * slows the walk down instead of losing items to a full channel. Cancelling the collection stops
 * after the page in flight.
 */
public fun <T : Any> AsyncPager<T>.asFlow(): Flow<T> = flow {
    var at = offset()
    while (true) {
        val page = page(limit(), at).await()
        val items = page.items()
        for (item in items) emit(item)
        val paginate = page.paginate()
        if (items.isEmpty() || paginate == null || paginate.hasPages() != true) break
        at += items.size
    }
}

/** Every item of a blocking pager as a Kotlin [Sequence], one page at a time. */
public fun <T> Pager<T>.asSequence(): Sequence<T> = Sequence { iterator() }

/** Cancels the call underlying a future the SDK returned. */
public fun CompletableFuture<*>.cancelCall(): Boolean = cancel(true)
