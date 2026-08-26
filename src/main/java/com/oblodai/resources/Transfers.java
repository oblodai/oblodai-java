package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.TransferBatchRequest;
import com.oblodai.contract.requests.TransferToPersonalRequest;
import com.oblodai.contract.requests.TransferToUserRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.TransferToPersonal;
import com.oblodai.models.TransferToUser;

/**
 * Internal, instant, fee-free moves between platform balances.
 *
 * <p>Nothing here touches a blockchain: the funds never leave the platform, so there is no network
 * fee and no confirmation to wait for.
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
     * @return the completed transfer
     */
    public TransferToPersonal toPersonal(TransferToPersonalRequest request) {
        return toPersonal(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/to-personal} — business balance to the owner's personal wallet.
     *
     * <p>Errors worth branching on: {@code transfer.bad_amount} — the amount is not valid;
     * {@code merchant.no_owner} — the merchant has no owner to pay;
     * {@code merchant.no_personal_wallet} — the owner has no personal wallet yet.
     *
     * @param request how much to move, and in which asset
     * @param options per-call options
     * @return the completed transfer
     */
    public TransferToPersonal toPersonal(
            TransferToPersonalRequest request, RequestOptions options) {
        return call(
                Routes.POST_V1_TRANSFER_TO_PERSONAL, request, options, TransferToPersonal.class);
    }

    /**
     * {@code POST /v1/transfer/to-user} — business balance to another platform user's personal
     * wallet. {@code amount} and {@code currency} are required.
     *
     * @param request the recipient, the amount and the asset
     * @return the completed transfer
     */
    public TransferToUser toUser(TransferToUserRequest request) {
        return toUser(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/to-user} — business balance to another platform user.
     *
     * <p>Errors worth branching on: {@code transfer.no_recipient} — no recipient was given;
     * {@code transfer.bad_recipient} — the recipient identifier is malformed;
     * {@code transfer.recipient_not_found} — no platform user answers to it;
     * {@code transfer.bad_amount} — the amount is not valid;
     * {@code sandbox.transfer_not_available} — a test key cannot fund a transfer to a real user.
     *
     * @param request the recipient, the amount and the asset
     * @param options per-call options
     * @return the completed transfer
     */
    public TransferToUser toUser(TransferToUserRequest request, RequestOptions options) {
        return call(Routes.POST_V1_TRANSFER_TO_USER, request, options, TransferToUser.class);
    }

    /**
     * {@code POST /v1/transfer/batch} — asynchronous batch of {@code toUser} transfers; poll
     * {@code batches().info(...)}. {@code order_id} is required on every item.
     *
     * @param request the transfers to submit
     * @return the batch ticket
     */
    public BatchSubmitted batch(TransferBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/transfer/batch} — asynchronous batch of {@code toUser} transfers.
     *
     * <p>Errors worth branching on: {@code batch.empty} — nothing to submit;
     * {@code batch.too_large} — over the 5000-item cap; {@code batch.order_id_required} — an item
     * carries no {@code order_id}; {@code batch.duplicate_order_id} — two items share one
     * {@code order_id}; {@code batch.bad_recipient} — an item names a recipient the gateway cannot
     * resolve.
     *
     * @param request the transfers to submit
     * @param options per-call options
     * @return the batch ticket
     */
    public BatchSubmitted batch(TransferBatchRequest request, RequestOptions options) {
        return call(Routes.POST_V1_TRANSFER_BATCH, request, options, BatchSubmitted.class);
    }
}
