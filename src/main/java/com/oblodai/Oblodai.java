package com.oblodai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Logger;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.core.Transport;
import com.oblodai.resources.Account;
import com.oblodai.resources.Batches;
import com.oblodai.resources.Catalog;
import com.oblodai.resources.Documents;
import com.oblodai.resources.Merchants;
import com.oblodai.resources.PaymentLinks;
import com.oblodai.resources.Payments;
import com.oblodai.resources.PayoutLinks;
import com.oblodai.resources.Payouts;
import com.oblodai.resources.Refunds;
import com.oblodai.resources.Sandbox;
import com.oblodai.resources.Settings;
import com.oblodai.resources.Splits;
import com.oblodai.resources.Transfers;
import com.oblodai.resources.Wallets;
import com.oblodai.resources.Webhooks;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * The Oblodai API client. One instance per key pair; immutable and safe to share across threads.
 *
 * <pre>{@code
 * Oblodai oblodai = Oblodai.builder()
 *         .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
 *         .secret(System.getenv("OBLODAI_SECRET"))
 *         .build();
 *
 * Payment invoice = oblodai.payments().create(new PaymentRequest()
 *         .amount("25")            // amounts are decimal strings, never floats
 *         .currency("USDT")
 *         .network(Network.TRON)
 *         .orderId("order-1001")
 *         .urlCallback("https://shop.example/oblodai/webhook"));
 * }</pre>
 *
 * <p>Every method's optional last argument is a {@link RequestOptions}. Calls block; {@link #async()}
 * gives the same surface returning {@link java.util.concurrent.CompletableFuture} over the same
 * engine, connections and clock.
 *
 * <p>The client is {@link AutoCloseable}: closing it releases the HTTP client it created for itself.
 * A long-lived application does not need to — one client per key pair, kept for the life of the
 * process, is the intended shape — but a short-lived tool can use try-with-resources.
 */
public final class Oblodai implements AutoCloseable {

    /** Version of this SDK. */
    public static final String VERSION = "1.3.0";

    /** Where the client talks unless told otherwise. */
    public static final String DEFAULT_BASE_URL = "https://api.oblodai.com";

    private final Transport transport;
    private final boolean ownsHttpClient;
    private final Payments payments;
    private final Refunds refunds;
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

    Oblodai(Transport transport) {
        this(transport, false);
    }

    Oblodai(Transport transport, boolean ownsHttpClient) {
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

    /** A builder for a client; every option also has an environment fallback. */
    public static Builder builder() {
        return new Builder();
    }

    /** Invoices: create, look up, cancel, list, and the payer-facing checkout endpoints. */
    public Payments payments() {
        return payments;
    }

    /** Refunds and the resolution of underpaid invoices. Payout key. */
    public Refunds refunds() {
        return refunds;
    }

    /** Outgoing transfers to external addresses. Payout key. */
    public Payouts payouts() {
        return payouts;
    }

    /** Payout links (cheques): funds reserved now, claimed later. Payout key. */
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

    /** Internal, instant, fee-free moves between platform balances. Payout key. */
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

    /** Revenue splits: a share of every payment forwarded to a partner. Payout key. */
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

    /** The same API returning futures, over this client's engine, connections and clock. */
    public OblodaiAsync async() {
        return new OblodaiAsync(transport);
    }

    /** The transport, for advanced use: custom routes, tests, reading the learned clock skew. */
    public Transport transport() {
        return transport;
    }

    /**
     * Releases the HTTP client this client built for itself; one supplied through
     * {@link Builder#httpClient(HttpClient)} is left alone, being the caller's to manage. Calls in
     * flight are not cancelled.
     */
    @Override
    public void close() {
        if (ownsHttpClient) ClientSettings.closeHttpClient(transport.httpClient());
    }

    /**
     * Builds a client. Options that are not set fall back to the environment, then to a default.
     *
     * <table border="1">
     *   <caption>Environment fallbacks</caption>
     *   <tr><td>{@code OBLODAI_PUBLIC_ID} / {@code OBLODAI_SECRET}</td><td>payment key pair</td></tr>
     *   <tr><td>{@code OBLODAI_PAYOUT_PUBLIC_ID} / {@code OBLODAI_PAYOUT_SECRET}</td><td>payout key pair</td></tr>
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
         * @param publicId public id of the payment key ({@code X-Public-Id})
         * @return this
         */
        public Builder publicId(String publicId) {
            settings.publicId = publicId;
            return this;
        }

        /**
         * @param secret secret of the payment key; used to sign, never sent
         * @return this
         */
        public Builder secret(String secret) {
            settings.secret = secret;
            return this;
        }

        /**
         * The dedicated payout key. Live merchants get two key kinds, and money-out routes
         * ({@code payouts}, {@code refunds}, {@code payoutLinks}, {@code transfers}, {@code splits},
         * auto-withdraw, the IP allow-list, {@code webhooks().rotateSecret()}, sandbox faucet and
         * reset) need this one; a call with the wrong kind is a 403 {@code merchant.wrong_key_kind}.
         * Sandbox keys are both kinds at once, so a sandbox integration can leave this unset.
         *
         * @param publicId public id of the payout key
         * @param secret secret of the payout key
         * @return this
         */
        public Builder payoutKey(String publicId, String secret) {
            settings.payoutPublicId = publicId;
            settings.payoutSecret = secret;
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
         * @param httpClient the JDK client to send with; supply one to control proxies, TLS or
         *     connection pooling. Defaults to a client that never follows redirects. A client you
         *     supply is yours to shut down: {@link Oblodai#close()} only closes the default one
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
         * @param logger structured logger; the transport redacts sensitive values before this logger
         *     ever sees them
         * @return this
         */
        public Builder logger(Logger logger) {
            settings.logger = logger;
            return this;
        }

        /**
         * @param mapper JSON mapper to decode with; defaults to the SDK's own configuration
         * @return this
         */
        public Builder objectMapper(ObjectMapper mapper) {
            settings.mapper = mapper;
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
         * @param name header name; one the SDK owns (Accept, Content-Type, User-Agent, the signing
         *     headers, Idempotency-Key, X-Admin-Token) is refused rather than silently dropped
         * @param value header value; must be non-null, ASCII and free of line breaks
         * @return this
         * @throws com.oblodai.errors.ConfigException when the name or the value could not be sent
         */
        public Builder header(String name, String value) {
            settings.header(name, value);
            return this;
        }

        /**
         * Permits a plain-http base URL. Loopback hosts are permitted anyway, so a local gateway
         * needs nothing.
         *
         * @param allow whether to permit http elsewhere
         * @return this
         */
        public Builder allowInsecureBaseUrl(boolean allow) {
            settings.allowInsecureBaseUrl = allow;
            return this;
        }

        /**
         * The signing clock. The default learns the gateway's time when a 401 reveals skew.
         *
         * @param clock the clock to sign with
         * @return this
         */
        public Builder clock(SkewCorrectingClock clock) {
            settings.clock = clock;
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

        /** Builds the blocking client. */
        public Oblodai build() {
            Transport transport = settings.buildTransport();
            return new Oblodai(transport, settings.ownsHttpClient);
        }

        /** Builds the {@link java.util.concurrent.CompletableFuture} client. */
        public OblodaiAsync buildAsync() {
            Transport transport = settings.buildTransport();
            return new OblodaiAsync(transport, settings.ownsHttpClient);
        }
    }
}
