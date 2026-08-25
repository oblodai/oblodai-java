package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.TransferBatchRequest;
import com.oblodai.contract.requests.TransferToPersonalRequest;
import com.oblodai.contract.requests.TransferToUserRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.TransferToPersonal;
import com.oblodai.models.TransferToUser;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Internal, instant, fee-free moves between platform balances. Payout key.
 *
 * <p>Nothing here touches a blockchain: the funds never leave the platform, so there is no network
 * fee and no confirmation to wait for.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Transfers}: the same methods,
 * returning {@link CompletableFuture}.
 */
public final class Transfers extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Transfers(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/transfer/to-personal} — business balance to the owner's personal wallet.
     * Needs an owner link on the merchant.
     *
     * @param request how much to move, and in which asset
     * @return a future of the completed transfer
     */
    public CompletableFuture<TransferToPersonal> toPersonal(TransferToPersonalRequest request) {
        return toPersonal(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/to-personal} — business balance to the owner's personal wallet.
     *
     * @param request how much to move, and in which asset
     * @param options per-call options
     * @return a future of the completed transfer
     */
    public CompletableFuture<TransferToPersonal> toPersonal(
            TransferToPersonalRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_TRANSFER_TO_PERSONAL, request, options, TransferToPersonal.class);
    }

    /**
     * {@code POST /v1/transfer/to-user} — business balance to another platform user's personal
     * wallet. {@code amount} and {@code currency} are required.
     *
     * @param request the recipient, the amount and the asset
     * @return a future of the completed transfer
     */
    public CompletableFuture<TransferToUser> toUser(TransferToUserRequest request) {
        return toUser(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/to-user} — business balance to another platform user.
     *
     * @param request the recipient, the amount and the asset
     * @param options per-call options
     * @return a future of the completed transfer
     */
    public CompletableFuture<TransferToUser> toUser(
            TransferToUserRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_TRANSFER_TO_USER, request, options, TransferToUser.class);
    }

    /**
     * {@code POST /v1/transfer/batch} — asynchronous batch of {@code toUser} transfers; poll
     * {@code batches().info(...)}. {@code order_id} is required on every item.
     *
     * @param request the transfers to submit
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(TransferBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/batch} — asynchronous batch of {@code toUser} transfers.
     *
     * @param request the transfers to submit
     * @param options per-call options
     * @return a future of the batch ticket
     */
    public CompletableFuture<BatchSubmitted> batch(
            TransferBatchRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_TRANSFER_BATCH, request, options, BatchSubmitted.class);
    }
}
