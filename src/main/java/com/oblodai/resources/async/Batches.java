package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.BatchInfoRequest;
import com.oblodai.core.Transport;
import com.oblodai.errors.PermissionException;
import com.oblodai.models.BatchInfo;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    /** The gateway's verdict when a batch was created by the other key kind. */
    private static final String WRONG_KEY_KIND = "merchant.wrong_key_kind";

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
     * {@code POST /v1/batch/info} — status, counters and per-row outcomes.
     *
     * <p>The route accepts either key kind, but the core requires the kind that created the batch.
     * A payout batch signed with the payment key is therefore refused with
     * {@code merchant.wrong_key_kind}; rather than making the caller know which key made the batch,
     * that one failure is retried with the payout key when the client has one configured. A caller
     * who already asked for the payout key gets the error as it came.
     *
     * @param request which batch to read, and which page of its rows
     * @param options per-call options
     * @return a future of the batch's progress
     */
    public CompletableFuture<BatchInfo> info(BatchInfoRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_BATCH_INFO, request, options, BatchInfo.class)
                .handle(
                        (BatchInfo result, Throwable error) -> {
                            if (error == null) return CompletableFuture.completedFuture(result);
                            Throwable cause =
                                    error instanceof CompletionException && error.getCause() != null
                                            ? error.getCause()
                                            : error;
                            if (cause instanceof PermissionException permission
                                    && WRONG_KEY_KIND.equals(permission.code())
                                    && !options.isPreferPayoutKey()) {
                                return callAsync(
                                        Routes.POST_V1_BATCH_INFO,
                                        request,
                                        options.preferPayoutKey(true),
                                        BatchInfo.class);
                            }
                            return CompletableFuture.<BatchInfo>failedFuture(cause);
                        })
                .thenCompose(future -> future);
    }
}
