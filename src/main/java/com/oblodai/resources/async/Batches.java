package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.BatchInfoRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchInfo;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Progress of asynchronous batches (payment, refund, payout, transfer, payout-link).
 *
 * <p>Every asynchronous batch route answers with a ticket rather than results; this namespace turns
 * that ticket into status, counters and per-row outcomes.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Batches}: the same methods,
 * returning {@link CompletableFuture}.
 */
public final class Batches extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Batches(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/batch/info} — status, counters and per-row outcomes.
     *
     * @param request which batch to read, and which page of its rows
     * @return a future of the batch's progress
     */
    public CompletableFuture<BatchInfo> info(BatchInfoRequest request) {
        return info(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/batch/info} — status, counters and per-row outcomes. Reads any batch the
     * merchant created, whichever route created it.
     *
     * @param request which batch to read, and which page of its rows
     * @param options per-call options
     * @return a future of the batch's progress
     */
    public CompletableFuture<BatchInfo> info(BatchInfoRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_BATCH_INFO, request, options, BatchInfo.class);
    }
}
