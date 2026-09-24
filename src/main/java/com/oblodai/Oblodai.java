package com.oblodai;

import com.oblodai.core.Hooks;
import com.oblodai.core.Logger;
import com.oblodai.core.RawResponse;
import com.oblodai.core.RequestInfo;
import com.oblodai.core.ResponseInfo;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.core.Sleeper;
import com.oblodai.core.Transport;
import com.oblodai.generated.resources.Account;
import com.oblodai.generated.resources.ApiAllowlist;
import com.oblodai.generated.resources.Batches;
import com.oblodai.generated.resources.Checkout;
import com.oblodai.generated.resources.Documents;
import com.oblodai.generated.resources.PaymentLinks;
import com.oblodai.generated.resources.Payments;
import com.oblodai.generated.resources.PayoutLinks;
import com.oblodai.generated.resources.Payouts;
import com.oblodai.generated.resources.Referrals;
import com.oblodai.generated.resources.Refunds;
import com.oblodai.generated.resources.Sandbox;
import com.oblodai.generated.resources.Settings;
import com.oblodai.generated.resources.Splits;
import com.oblodai.generated.resources.Wallets;
import com.oblodai.generated.resources.Webhooks;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Oblodai API client. One instance per key pair; immutable and safe to share across threads.
 *
 * <pre>{@code
 * Oblodai oblodai = Oblodai.builder()
 *         .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
 *         .secret(System.getenv("OBLODAI_SECRET"))
 *         .build();
 *
 * PaymentView invoice = oblodai.payments().create(PaymentRequest.builder()
 *         .amount("25")                 // a decimal string or a BigDecimal, never a double
 *         .currency("USDT")
 *         .orderId("order-1001")
 *         .build());
 * }</pre>
 *
 * <p>Every method's last argument is an optional {@link RequestOptions}. Calls block; {@link
 * #async()} gives the same surface returning {@link java.util.concurrent.CompletableFuture} over the
 * same engine, connections and clock. {@link #withOptions(RequestOptions)} makes a copy with other
 * defaults, {@link #withRawResponse(Function)} exposes the HTTP side of a call, and {@link #jobs()}
 * follows long-running operations.
 *
 * <p>The client is {@link AutoCloseable}: closing it releases the HTTP client it created for itself.
 */
public final class Oblodai implements AutoCloseable {

    /** Version of this SDK. */
    public static final String VERSION = "2.0.0";

    /** Where the client talks unless told otherwise. */
    public static final String DEFAULT_BASE_URL = "https://api.oblodai.com";

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
    private final Jobs jobs;

    Oblodai(Transport transport, boolean ownsHttpClient) {
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
        this.jobs = new Jobs(transport);
    }

    /** @return a builder for a client; every option also has an environment fallback */
    public static Builder builder() {
        return new Builder();
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

    /** @return waiters for long-running operations: batches and document jobs */
    public Jobs jobs() {
        return jobs;
    }

    /** @return the same API returning futures, over this client's engine, connections and clock */
    public OblodaiAsync async() {
        return new OblodaiAsync(transport, false);
    }

    /**
     * A copy of this client whose calls use other defaults; the connections, the clock and the
     * credentials are shared. {@code timeout} replaces the per-attempt timeout, {@code maxRetries}
     * the retry count, {@code extraHeaders} are merged over the client's headers.
     *
     * @param defaults the new defaults; an idempotency key or a request id names one call and is
     *     refused here
     * @return the copy; closing it leaves the shared HTTP client open
     */
    public Oblodai withOptions(RequestOptions defaults) {
        return new Oblodai(ClientSettings.derive(transport, defaults), false);
    }

    /**
     * Runs calls on a copy of this client and returns the value of {@code call} together with the
     * HTTP side of the last successful call it made: status, headers, request id.
     *
     * <pre>{@code
     * ApiResponse<PaymentView> r = oblodai.withRawResponse(c -> c.payments().create(request));
     * log.info("invoice {} (request {})", r.value().uuid(), r.requestId());
     * }</pre>
     *
     * @param call what to run, on the copy it is given
     * @param <T> its value
     * @return the value and the raw answer
     * @throws IllegalStateException when {@code call} made no call
     */
    public <T> ApiResponse<T> withRawResponse(Function<Oblodai, T> call) {
        AtomicReference<RawResponse> last = new AtomicReference<>();
        T value = call.apply(new Oblodai(transport.observing(last::set), false));
        RawResponse raw = last.get();
        if (raw == null) {
            throw new IllegalStateException("withRawResponse: the function made no API call");
        }
        return new ApiResponse<>(value, raw);
    }

    /** @return the transport, for advanced use: custom routes, tests, the learned clock skew */
    public Transport transport() {
        return transport;
    }

    /**
     * Releases the HTTP client this client built for itself; one supplied through {@link
     * Builder#httpClient(HttpClient)} is left alone. Calls in flight are not cancelled.
     */
    @Override
    public void close() {
        if (ownsHttpClient) {
            ClientSettings.closeHttpClient(transport.httpClient());
        }
    }

    /**
     * Builds a client. Options that are not set fall back to the environment, then to a default.
     *
     * <table border="1">
     *   <caption>Environment fallbacks</caption>
     *   <tr><td>{@code OBLODAI_PUBLIC_ID} / {@code OBLODAI_SECRET}</td><td>the API key pair</td></tr>
     *   <tr><td>{@code OBLODAI_BASE_URL}</td><td>API origin</td></tr>
     *   <tr><td>{@code OBLODAI_ADMIN_TOKEN}</td><td>admin token of a self-hosted gateway</td></tr>
     *   <tr><td>{@code OBLODAI_ALLOW_INSECURE=1}</td><td>permit a plain-http base URL</td></tr>
     *   <tr><td>{@code OBLODAI_LOG=debug|info|warn|error}</td><td>log to stderr</td></tr>
     * </table>
     */
    public static final class Builder {

        private final ClientSettings settings = new ClientSettings();

        Builder() {}

        /**
         * @param publicId public id of the merchant's API key ({@code X-Public-Id})
         * @return this
         */
        public Builder publicId(String publicId) {
            settings.publicId = publicId;
            return this;
        }

        /**
         * @param secret secret of the merchant's API key; used to sign, never sent
         * @return this
         */
        public Builder secret(String secret) {
            settings.secret = secret;
            return this;
        }

        /**
         * @param adminToken admin token of a self-hosted gateway; only merchant provisioning sends it
         * @return this
         */
        public Builder adminToken(String adminToken) {
            settings.adminToken = adminToken;
            return this;
        }

        /**
         * @param baseUrl API origin; a path prefix is kept ({@code https://gw.example/oblodai})
         * @return this
         */
        public Builder baseUrl(String baseUrl) {
            settings.baseUrl = baseUrl;
            return this;
        }

        /**
         * @param httpClient the JDK client to send with, to control proxies, TLS or pooling. The
         *     default never follows redirects. A client you supply is yours to shut down
         * @return this
         */
        public Builder httpClient(HttpClient httpClient) {
            settings.httpClient = httpClient;
            return this;
        }

        /**
         * @param retry retry policy; {@link RetryOptions#none()} disables retrying
         * @return this
         */
        public Builder retry(RetryOptions retry) {
            settings.retry = retry;
            return this;
        }

        /**
         * @param maxRetries retries after the first attempt, keeping the rest of the policy
         * @return this
         */
        public Builder maxRetries(int maxRetries) {
            settings.retry = settings.retry.withMaxRetries(maxRetries);
            return this;
        }

        /**
         * @param logger structured logger; sensitive values are redacted before it sees them
         * @return this
         */
        public Builder logger(Logger logger) {
            settings.logger = logger;
            return this;
        }

        /**
         * @param timeout per-attempt timeout, default 30 s
         * @return this
         */
        public Builder timeout(Duration timeout) {
            settings.timeoutMs = timeout.toMillis();
            return this;
        }

        /**
         * @param deadline overall budget per call, retries and pauses included, default 90 s
         * @return this
         */
        public Builder deadline(Duration deadline) {
            settings.deadlineMs = deadline.toMillis();
            return this;
        }

        /**
         * A header to send on every request.
         *
         * @param name header name; one the SDK owns is refused rather than silently dropped
         * @param value header value; non-null, ASCII, free of line breaks
         * @return this
         * @throws com.oblodai.errors.ConfigException when the name or the value could not be sent
         */
        public Builder header(String name, String value) {
            settings.header(name, value);
            return this;
        }

        /**
         * @param hook called before every attempt is sent (the signature and the admin token
         *     redacted)
         * @return this
         */
        public Builder onRequest(Consumer<RequestInfo> hook) {
            settings.hooks = settings.hooks.withOnRequest(hook);
            return this;
        }

        /**
         * @param hook called when every attempt ends: status, headers, time taken, error
         * @return this
         */
        public Builder onResponse(Consumer<ResponseInfo> hook) {
            settings.hooks = settings.hooks.withOnResponse(hook);
            return this;
        }

        /**
         * @param hooks both hooks at once
         * @return this
         */
        public Builder hooks(Hooks hooks) {
            settings.hooks = hooks == null ? Hooks.NONE : hooks;
            return this;
        }

        /**
         * Permits a plain-http base URL. Loopback hosts are permitted anyway.
         *
         * @param allow whether to permit http elsewhere
         * @return this
         */
        public Builder allowInsecureBaseUrl(boolean allow) {
            settings.allowInsecureBaseUrl = allow;
            return this;
        }

        /**
         * @param clock the signing clock; the default learns the gateway's time on a skew failure
         * @return this
         */
        public Builder clock(SkewCorrectingClock clock) {
            settings.clock = clock;
            return this;
        }

        /**
         * @param sleeper how to wait between attempts and job polls; for tests
         * @return this
         */
        public Builder sleeper(Sleeper sleeper) {
            settings.sleeper = sleeper;
            return this;
        }

        /**
         * Replaces the environment the fallbacks read, for tests.
         *
         * @param environment the variables to read
         * @return this
         */
        public Builder environment(Map<String, String> environment) {
            settings.environment = environment == null ? Map.of() : environment;
            return this;
        }

        /** @return the blocking client */
        public Oblodai build() {
            Transport transport = settings.buildTransport();
            return new Oblodai(transport, settings.ownsHttpClient);
        }

        /** @return the {@link java.util.concurrent.CompletableFuture} client */
        public OblodaiAsync buildAsync() {
            Transport transport = settings.buildTransport();
            return new OblodaiAsync(transport, settings.ownsHttpClient);
        }
    }
}
