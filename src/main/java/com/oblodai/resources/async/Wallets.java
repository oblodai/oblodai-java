package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.WalletBlockRequest;
import com.oblodai.contract.requests.WalletBlockedAddressRefundRequest;
import com.oblodai.contract.requests.WalletQrRequest;
import com.oblodai.contract.requests.WalletRequest;
import com.oblodai.core.Transport;
import com.oblodai.models.Payout;
import com.oblodai.models.Wallet;
import com.oblodai.models.WalletBlocked;
import com.oblodai.models.WalletQr;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * Static deposit wallets: one permanent address per customer, deposits reported as
 * {@code wallet.paid}.
 *
 * <p>Unlike an invoice, a wallet has no amount and no expiry: it keeps crediting whatever arrives
 * until you block it.
 *
 * <p>This is the non-blocking form of {@link com.oblodai.resources.Wallets}: the same methods,
 * returning {@link CompletableFuture}.
 */
public final class Wallets extends Resource {

    /**
     * @param transport the engine to call through
     */
    public Wallets(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/wallet} — idempotent by {@code order_id}: asking twice for the same customer
     * returns the same address rather than minting a second one.
     *
     * @param request the wallet to create
     * @return a future of the wallet, with its permanent deposit address
     */
    public CompletableFuture<Wallet> create(WalletRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet} — idempotent by {@code order_id}.
     *
     * @param request the wallet to create
     * @param options per-call options
     * @return a future of the wallet, with its permanent deposit address
     */
    public CompletableFuture<Wallet> create(WalletRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_WALLET, request, options, Wallet.class);
    }

    /**
     * {@code POST /v1/wallet/qr} — QR image of the wallet's deposit address.
     *
     * @param address the wallet's deposit address
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<WalletQr> qr(String address) {
        return qr(address, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/qr}.
     *
     * @param address the wallet's deposit address
     * @param options per-call options
     * @return a future of the QR image and what it encodes
     */
    public CompletableFuture<WalletQr> qr(String address, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_WALLET_QR,
                new WalletQrRequest().address(address),
                options,
                WalletQr.class);
    }

    /**
     * {@code POST /v1/wallet/block} — stop crediting an address; later deposits wait for a refund
     * decision instead of being booked to the merchant's balance.
     *
     * @param request which address to block, and why
     * @return a future of the address and its new blocked state
     */
    public CompletableFuture<WalletBlocked> block(WalletBlockRequest request) {
        return block(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/block} — stop crediting an address.
     *
     * @param request which address to block, and why
     * @param options per-call options
     * @return a future of the address and its new blocked state
     */
    public CompletableFuture<WalletBlocked> block(
            WalletBlockRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_WALLET_BLOCK, request, options, WalletBlocked.class);
    }

    /**
     * {@code POST /v1/wallet/blocked-address-refund} — send funds that landed on a blocked address
     * back where they came from. Requires the payout key.
     *
     * @param request which deposit to send back, and to which address
     * @return a future of the refund, as the payout it is
     */
    public CompletableFuture<Payout> refundBlockedDeposit(
            WalletBlockedAddressRefundRequest request) {
        return refundBlockedDeposit(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/blocked-address-refund} — send funds off a blocked address back.
     *
     * @param request which deposit to send back, and to which address
     * @param options per-call options
     * @return a future of the refund, as the payout it is
     */
    public CompletableFuture<Payout> refundBlockedDeposit(
            WalletBlockedAddressRefundRequest request, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_WALLET_BLOCKED_ADDRESS_REFUND, request, options, Payout.class);
    }
}
