package com.oblodai;

import com.oblodai.core.RawResponse;
import com.oblodai.core.Transport;
import com.oblodai.generated.resources.async.Account;
import com.oblodai.generated.resources.async.ApiAllowlist;
import com.oblodai.generated.resources.async.Batches;
import com.oblodai.generated.resources.async.Checkout;
import com.oblodai.generated.resources.async.CliLogin;
import com.oblodai.generated.resources.async.Documents;
import com.oblodai.generated.resources.async.PaymentLinks;
import com.oblodai.generated.resources.async.Payments;
import com.oblodai.generated.resources.async.PayoutLinks;
import com.oblodai.generated.resources.async.Payouts;
import com.oblodai.generated.resources.async.Referrals;
import com.oblodai.generated.resources.async.Refunds;
import com.oblodai.generated.resources.async.Sandbox;
import com.oblodai.generated.resources.async.Settings;
import com.oblodai.generated.resources.async.Splits;
import com.oblodai.generated.resources.async.Wallets;
import com.oblodai.generated.resources.async.Webhooks;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The same API as {@link Oblodai}, returning {@link CompletableFuture}. It runs over the same
 * engine, connections, retry policy and learned clock skew.
 *
 * <p>Build one with {@code Oblodai.builder()....buildAsync()}, or take one from a blocking client
 * with {@link Oblodai#async()}; {@link #blocking()} goes back the other way.
 *
 * <pre>{@code
 * oblodai.async().payments()
 *         .create(PaymentRequest.builder().amount("25").currency("USDT").orderId("order-1001").build())
 *         .thenAccept(invoice -> System.out.println(invoice.uuid()));
 * }</pre>
 *
 * <p>Every future fails with the {@link com.oblodai.errors.OblodaiException} the blocking client
 * would have thrown; cancelling one cancels the HTTP exchange in flight.
 */
public final class OblodaiAsync implements AutoCloseable {

    private final Transport transport;
    private final boolean ownsHttpClient;
    private final Payments payments;
    private final PaymentLinks paymentLinks;
    private final Refunds refunds;
    private final Payouts payouts;
    private final PayoutLinks payoutLinks;
    private final Batches batches;
    private final Splits splits;
    private final Wallets wallets;
    private final Account account;
    private final Webhooks webhooks;
    private final Settings settings;
    private final ApiAllowlist apiAllowlist;
    private final Referrals referrals;
    private final Documents documents;
    private final Checkout checkout;
    private final Sandbox sandbox;
    private final CliLogin cliLogin;
    private final AsyncJobs jobs;

    OblodaiAsync(Transport transport, boolean ownsHttpClient) {
        this.transport = transport;
        this.ownsHttpClient = ownsHttpClient;
        this.payments = new Payments(transport);
        this.paymentLinks = new PaymentLinks(transport);
        this.refunds = new Refunds(transport);
        this.payouts = new Payouts(transport);
        this.payoutLinks = new PayoutLinks(transport);
        this.batches = new Batches(transport);
        this.splits = new Splits(transport);
        this.wallets = new Wallets(transport);
        this.account = new Account(transport);
        this.webhooks = new Webhooks(transport);
        this.settings = new Settings(transport);
        this.apiAllowlist = new ApiAllowlist(transport);
        this.referrals = new Referrals(transport);
        this.documents = new Documents(transport);
        this.checkout = new Checkout(transport);
        this.sandbox = new Sandbox(transport);
        this.cliLogin = new CliLogin(transport);
        this.jobs = new AsyncJobs(transport);
    }

    /** @return Invoices: create, look up, cancel, list, resolve. */
    public Payments payments() {
        return payments;
    }

    /** @return Reusable payment links: each checkout spawns an invoice. */
    public PaymentLinks paymentLinks() {
        return paymentLinks;
    }

    /** @return Refunds of payments and of blocked wallets. */
    public Refunds refunds() {
        return refunds;
    }

    /** @return Outgoing transfers, internal transfers and their fees. */
    public Payouts payouts() {
        return payouts;
    }

    /** @return Payout links (cheques): funds reserved now, claimed later. */
    public PayoutLinks payoutLinks() {
        return payoutLinks;
    }

    /** @return Asynchronous batches of payments, payouts and refunds; follow them with {@link #jobs()}. */
    public Batches batches() {
        return batches;
    }

    /** @return Revenue splits: a share of every payment forwarded to a partner. */
    public Splits splits() {
        return splits;
    }

    /** @return Static deposit wallets: one permanent address per customer. */
    public Wallets wallets() {
        return wallets;
    }

    /** @return Balances, the account summary and exchange rates. */
    public Account account() {
        return account;
    }

    /** @return Webhook endpoints, test deliveries and the delivery log. */
    public Webhooks webhooks() {
        return webhooks;
    }

    /** @return Merchant-level configuration exposed over the API. */
    public Settings settings() {
        return settings;
    }

    /** @return The IP allowlist of the API key. */
    public ApiAllowlist apiAllowlist() {
        return apiAllowlist;
    }

    /** @return The referral programme. */
    public Referrals referrals() {
        return referrals;
    }

    /** @return PDF and CSV documents, and document export jobs. */
    public Documents documents() {
        return documents;
    }

    /** @return The payer-facing checkout: public, no credentials. */
    public Checkout checkout() {
        return checkout;
    }

    /** @return Developer sandbox: fake money, simulated deposits, a webhook inspector. */
    public Sandbox sandbox() {
        return sandbox;
    }

    /**
     * @return Browser login of the {@code oblodai} CLI (OAuth 2.0 device authorization, RFC 8628) and
     *     logout of its key.
     */
    public CliLogin cliLogin() {
        return cliLogin;
    }

    /** @return waiters for long-running operations: batches and document jobs */
    public AsyncJobs jobs() {
        return jobs;
    }

    /** @return the blocking API over the same engine */
    public Oblodai blocking() {
        return new Oblodai(transport, false);
    }

    /**
     * A copy of this client whose calls use other defaults; see {@link
     * Oblodai#withOptions(RequestOptions)}.
     *
     * @param defaults the new defaults
     * @return the copy
     */
    public OblodaiAsync withOptions(RequestOptions defaults) {
        return new OblodaiAsync(ClientSettings.derive(transport, defaults), false);
    }

    /**
     * Runs calls on a copy of this client and completes with the value of {@code call} together
     * with the HTTP side of the last successful call it made.
     *
     * @param call what to run, on the copy it is given
     * @param <T> its value
     * @return a future of the value and the raw answer
     */
    public <T> CompletableFuture<ApiResponse<T>> withRawResponse(
            Function<OblodaiAsync, CompletableFuture<T>> call) {
        AtomicReference<RawResponse> last = new AtomicReference<>();
        return call.apply(new OblodaiAsync(transport.observing(last::set), false))
                .thenApply(
                        value -> {
                            RawResponse raw = last.get();
                            if (raw == null) {
                                throw new IllegalStateException(
                                        "withRawResponse: the function made no API call");
                            }
                            return new ApiResponse<>(value, raw);
                        });
    }

    /** @return the transport, for advanced use */
    public Transport transport() {
        return transport;
    }

    /** Releases the HTTP client this client built for itself. */
    @Override
    public void close() {
        if (ownsHttpClient) {
            ClientSettings.closeHttpClient(transport.httpClient());
        }
    }
}
