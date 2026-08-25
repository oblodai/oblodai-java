package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.BatchInfoRequest;
import com.oblodai.core.Transport;
import com.oblodai.errors.PermissionException;
import com.oblodai.models.BatchInfo;

/**
 * Progress of asynchronous batches (payment, refund, payout, transfer, payout-link).
 *
 * <p>Every asynchronous batch route answers with a ticket rather than results; this namespace turns
 * that ticket into status, counters and per-row outcomes.
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
     * @return the batch's progress
     */
    public BatchInfo info(BatchInfoRequest request) {
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
     * @return the batch's progress
     */
    public BatchInfo info(BatchInfoRequest request, RequestOptions options) {
        try {
            return call(Routes.POST_V1_BATCH_INFO, request, options, BatchInfo.class);
        } catch (PermissionException e) {
            if (!WRONG_KEY_KIND.equals(e.code()) || options.isPreferPayoutKey()) throw e;
            return call(
                    Routes.POST_V1_BATCH_INFO,
                    request,
                    options.preferPayoutKey(true),
                    BatchInfo.class);
        }
    }
}
