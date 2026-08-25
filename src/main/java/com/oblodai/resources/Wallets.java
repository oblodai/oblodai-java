package com.oblodai.resources;

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

/**
 * Static deposit wallets: one permanent address per customer, deposits reported as
 * {@code wallet.paid}.
 *
 * <p>Unlike an invoice, a wallet has no amount and no expiry: it keeps crediting whatever arrives
 * until you block it.
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
     * @return the wallet, with its permanent deposit address
     */
    public Wallet create(WalletRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet} — idempotent by {@code order_id}.
     *
     * @param request the wallet to create
     * @param options per-call options
     * @return the wallet, with its permanent deposit address
     */
    public Wallet create(WalletRequest request, RequestOptions options) {
        return call(Routes.POST_V1_WALLET, request, options, Wallet.class);
    }

    /**
     * {@code POST /v1/wallet/qr} — QR image of the wallet's deposit address.
     *
     * @param address the wallet's deposit address
     * @return the QR image and what it encodes
     */
    public WalletQr qr(String address) {
        return qr(address, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/qr}.
     *
     * @param address the wallet's deposit address
     * @param options per-call options
     * @return the QR image and what it encodes
     */
    public WalletQr qr(String address, RequestOptions options) {
        return call(
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
     * @return the address and its new blocked state
     */
    public WalletBlocked block(WalletBlockRequest request) {
        return block(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/block} — stop crediting an address.
     *
     * @param request which address to block, and why
     * @param options per-call options
     * @return the address and its new blocked state
     */
    public WalletBlocked block(WalletBlockRequest request, RequestOptions options) {
        return call(Routes.POST_V1_WALLET_BLOCK, request, options, WalletBlocked.class);
    }

    /**
     * {@code POST /v1/wallet/blocked-address-refund} — send funds that landed on a blocked address
     * back where they came from. Requires the payout key.
     *
     * @param request which deposit to send back, and to which address
     * @return the refund, as the payout it is
     */
    public Payout refundBlockedDeposit(WalletBlockedAddressRefundRequest request) {
        return refundBlockedDeposit(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/wallet/blocked-address-refund} — send funds off a blocked address back.
     *
     * @param request which deposit to send back, and to which address
     * @param options per-call options
     * @return the refund, as the payout it is
     */
    public Payout refundBlockedDeposit(
            WalletBlockedAddressRefundRequest request, RequestOptions options) {
        return call(Routes.POST_V1_WALLET_BLOCKED_ADDRESS_REFUND, request, options, Payout.class);
    }
}
