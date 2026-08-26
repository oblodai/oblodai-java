package com.oblodai;

import com.oblodai.core.Transport;
import com.oblodai.resources.async.Account;
import com.oblodai.resources.async.Batches;
import com.oblodai.resources.async.Catalog;
import com.oblodai.resources.async.Documents;
import com.oblodai.resources.async.Merchants;
import com.oblodai.resources.async.PaymentLinks;
import com.oblodai.resources.async.Payments;
import com.oblodai.resources.async.PayoutLinks;
import com.oblodai.resources.async.Payouts;
import com.oblodai.resources.async.Refunds;
import com.oblodai.resources.async.Sandbox;
import com.oblodai.resources.async.Settings;
import com.oblodai.resources.async.Splits;
import com.oblodai.resources.async.Transfers;
import com.oblodai.resources.async.Wallets;
import com.oblodai.resources.async.Webhooks;

/**
 * The same API as {@link Oblodai}, returning {@link java.util.concurrent.CompletableFuture}. It
 * runs over the same engine, the same connections, the same retry policy and the same learned
 * clock skew — a blocking and an asynchronous client built from one transport share all of it.
 *
 * <p>Build one with {@code Oblodai.builder()....buildAsync()}, or take one from an existing
 * blocking client with {@link Oblodai#async()}; {@link #blocking()} goes back the other way.
 *
 * <pre>{@code
 * OblodaiAsync oblodai = Oblodai.builder()
 *         .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
 *         .secret(System.getenv("OBLODAI_SECRET"))
 *         .buildAsync();
 *
 * oblodai.payments()
 *         .create(new PaymentRequest()
 *                 .amount("25")            // amounts are decimal strings, never floats
 *                 .currency("USDT")
 *                 .network(Network.TRON)
 *                 .orderId("order-1001"))
 *         .thenAccept(invoice -> System.out.println(invoice.uuid()))
 *         .join();
 * }</pre>
 *
 * <p>Every future fails with an {@link com.oblodai.errors.OblodaiException} as its cause, the same
 * exception the blocking client would have thrown.
 *
 * <p>Immutable and safe to share across threads.
 */
public final class OblodaiAsync implements AutoCloseable {

    private final Transport transport;
    private final Payments payments;
    private final Refunds refunds;
    private final boolean ownsHttpClient;
    private final Payouts payouts;
    private final PayoutLinks payoutLinks;
    private final PaymentLinks paymentLinks;
    private final Batches batches;
    private final Transfers transfers;
    private final Wallets wallets;
    private final Webhooks webhooks;
    private final Documents documents;
    private final Splits splits;
    private final Settings settings;
    private final Account account;
    private final Catalog catalog;
    private final Sandbox sandbox;
    private final Merchants merchants;

    OblodaiAsync(Transport transport) {
        this(transport, false);
    }

    OblodaiAsync(Transport transport, boolean ownsHttpClient) {
        this.transport = transport;
        this.ownsHttpClient = ownsHttpClient;
        this.payments = new Payments(transport);
        this.refunds = new Refunds(transport);
        this.payouts = new Payouts(transport);
        this.payoutLinks = new PayoutLinks(transport);
        this.paymentLinks = new PaymentLinks(transport);
        this.batches = new Batches(transport);
        this.transfers = new Transfers(transport);
        this.wallets = new Wallets(transport);
        this.webhooks = new Webhooks(transport);
        this.documents = new Documents(transport);
        this.splits = new Splits(transport);
        this.settings = new Settings(transport);
        this.account = new Account(transport);
        this.catalog = new Catalog(transport);
        this.sandbox = new Sandbox(transport);
        this.merchants = new Merchants(transport);
    }

    /** Invoices: create, look up, cancel, list, and the payer-facing checkout endpoints. */
    public Payments payments() {
        return payments;
    }

    /** Refunds and the resolution of underpaid invoices. */
    public Refunds refunds() {
        return refunds;
    }

    /** Outgoing transfers to external addresses. */
    public Payouts payouts() {
        return payouts;
    }

    /** Payout links (cheques): funds reserved now, claimed later. */
    public PayoutLinks payoutLinks() {
        return payoutLinks;
    }

    /** Reusable payment links: each checkout spawns an invoice. */
    public PaymentLinks paymentLinks() {
        return paymentLinks;
    }

    /** Progress of asynchronous batches. */
    public Batches batches() {
        return batches;
    }

    /** Internal, instant, fee-free moves between platform balances. */
    public Transfers transfers() {
        return transfers;
    }

    /** Static deposit wallets: one permanent address per customer. */
    public Wallets wallets() {
        return wallets;
    }

    /** Webhook endpoint management and delivery inspection. */
    public Webhooks webhooks() {
        return webhooks;
    }

    /** Generated PDF and CSV documents. */
    public Documents documents() {
        return documents;
    }

    /** Revenue splits: a share of every payment forwarded to a partner. */
    public Splits splits() {
        return splits;
    }

    /** Merchant-level configuration exposed over the API. */
    public Settings settings() {
        return settings;
    }

    /** Balances and account-level facts. */
    public Account account() {
        return account;
    }

    /** Public reference data: currencies and exchange rates. No credentials needed. */
    public Catalog catalog() {
        return catalog;
    }

    /** Developer sandbox: fake money, simulated deposits, a webhook inspector. */
    public Sandbox sandbox() {
        return sandbox;
    }

    /** Merchant provisioning, for platforms that onboard merchants themselves. */
    public Merchants merchants() {
        return merchants;
    }

    /** The same API blocking, over this client's engine, connections and clock. */
    public Oblodai blocking() {
        return new Oblodai(transport);
    }

    /**
     * Releases the HTTP client this client built for itself; one supplied through
     * {@code httpClient(...)} is left alone. Calls in flight are not cancelled.
     */
    @Override
    public void close() {
        if (ownsHttpClient) ClientSettings.closeHttpClient(transport.httpClient());
    }

    /** The transport, for advanced use: custom routes, tests, reading the learned clock skew. */
    public Transport transport() {
        return transport;
    }
}
