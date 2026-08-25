package com.oblodai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.contract.ContractVersion;
import com.oblodai.core.Credentials;
import com.oblodai.core.Json;
import com.oblodai.core.Logger;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.core.Transport;
import com.oblodai.errors.ConfigException;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
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
 */
public final class Oblodai {

    /** Version of this SDK. */
    public static final String VERSION = "1.3.0";

    /** Where the client talks unless told otherwise. */
    public static final String DEFAULT_BASE_URL = "https://api.oblodai.com";

    private final Transport transport;
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
        this.transport = transport;
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

        private String publicId;
        private String secret;
        private String payoutPublicId;
        private String payoutSecret;
        private String adminToken;
        private String baseUrl;
        private HttpClient httpClient;
        private RetryOptions retry = RetryOptions.DEFAULT;
        private Logger logger;
        private ObjectMapper mapper;
        private SkewCorrectingClock clock;
        private long timeoutMs = 30_000;
        private long deadlineMs = 90_000;
        private Boolean allowInsecureBaseUrl;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Map<String, String> environment = System.getenv();

        Builder() {}

        /**
         * @param publicId public id of the payment key ({@code X-Public-Id})
         * @return this
         */
        public Builder publicId(String publicId) {
            this.publicId = publicId;
            return this;
        }

        /**
         * @param secret secret of the payment key; used to sign, never sent
         * @return this
         */
        public Builder secret(String secret) {
            this.secret = secret;
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
            this.payoutPublicId = publicId;
            this.payoutSecret = secret;
            return this;
        }

        /**
         * @param adminToken admin token of a self-hosted gateway; only merchant provisioning sends it
         * @return this
         */
        public Builder adminToken(String adminToken) {
            this.adminToken = adminToken;
            return this;
        }

        /**
         * @param baseUrl API origin; a path prefix is kept ({@code https://gw.example/oblodai})
         * @return this
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * @param httpClient the JDK client to send with; supply one to control proxies, TLS or
         *     connection pooling. Defaults to a client that never follows redirects.
         * @return this
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * @param retry retry policy; {@link RetryOptions#none()} disables retrying
         * @return this
         */
        public Builder retry(RetryOptions retry) {
            this.retry = retry;
            return this;
        }

        /**
         * @param logger structured logger; values under sensitive keys are redacted for it
         * @return this
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * @param mapper JSON mapper to decode with; defaults to the SDK's own configuration
         * @return this
         */
        public Builder objectMapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * @param timeout per-attempt timeout, default 30 s
         * @return this
         */
        public Builder timeout(Duration timeout) {
            this.timeoutMs = timeout.toMillis();
            return this;
        }

        /**
         * @param deadline overall budget per call, retries and pauses included, default 90 s
         * @return this
         */
        public Builder deadline(Duration deadline) {
            this.deadlineMs = deadline.toMillis();
            return this;
        }

        /**
         * @param name header name
         * @param value header value; a name that collides with a signed header is dropped
         * @return this
         */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
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
            this.allowInsecureBaseUrl = allow;
            return this;
        }

        /**
         * The signing clock. The default learns the gateway's time when a 401 reveals skew.
         *
         * @param clock the clock to sign with
         * @return this
         */
        public Builder clock(SkewCorrectingClock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Replaces the environment the fallbacks read, for tests.
         *
         * @param environment the variables to read
         * @return this
         */
        public Builder environment(Map<String, String> environment) {
            this.environment = environment == null ? Map.of() : environment;
            return this;
        }

        /** Builds the blocking client. */
        public Oblodai build() {
            return new Oblodai(buildTransport());
        }

        /** Builds the {@link java.util.concurrent.CompletableFuture} client. */
        public OblodaiAsync buildAsync() {
            return new OblodaiAsync(buildTransport());
        }

        private Transport buildTransport() {
            String resolvedBaseUrl = firstSet(baseUrl, env("OBLODAI_BASE_URL"), DEFAULT_BASE_URL);
            while (resolvedBaseUrl.endsWith("/")) {
                resolvedBaseUrl = resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1);
            }
            boolean allowInsecure =
                    allowInsecureBaseUrl != null
                            ? allowInsecureBaseUrl
                            : "1".equals(env("OBLODAI_ALLOW_INSECURE"));
            assertBaseUrl(resolvedBaseUrl, allowInsecure);

            String id = firstSet(publicId, env("OBLODAI_PUBLIC_ID"), null);
            String key = firstSet(secret, env("OBLODAI_SECRET"), null);
            if ((id == null) != (key == null)) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG,
                        "publicId and secret must be provided together (or set both OBLODAI_PUBLIC_ID"
                                + " and OBLODAI_SECRET)",
                        null);
            }
            String payoutId = firstSet(payoutPublicId, env("OBLODAI_PAYOUT_PUBLIC_ID"), null);
            String payoutKey = firstSet(payoutSecret, env("OBLODAI_PAYOUT_SECRET"), null);
            if ((payoutId == null) != (payoutKey == null)) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG,
                        "the payout key's publicId and secret must be provided together",
                        null);
            }

            Logger resolvedLogger = logger != null ? logger : loggerFromEnvironment();
            ObjectMapper resolvedMapper = mapper != null ? mapper : Json.mapper();
            HttpClient client =
                    httpClient != null
                            ? httpClient
                            : HttpClient.newBuilder()
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(10))
                                    .build();

            return new Transport(
                    new Transport.Config(
                            resolvedBaseUrl,
                            id == null ? null : new Credentials(id, key),
                            payoutId == null ? null : new Credentials(payoutId, payoutKey),
                            client,
                            retry,
                            clock != null ? clock : new SkewCorrectingClock(),
                            resolvedLogger,
                            timeoutMs,
                            deadlineMs,
                            Map.copyOf(headers),
                            firstSet(adminToken, env("OBLODAI_ADMIN_TOKEN"), null),
                            userAgent(),
                            resolvedMapper));
        }

        private Logger loggerFromEnvironment() {
            String level = env("OBLODAI_LOG");
            if (level == null) return Logger.noop();
            try {
                return Logger.console(Logger.Level.valueOf(level.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return Logger.noop();
            }
        }

        private String env(String name) {
            String value = environment == null ? null : environment.get(name);
            return value == null || value.isEmpty() ? null : value;
        }

        private static String firstSet(String a, String b, String fallback) {
            if (a != null && !a.isEmpty()) return a;
            if (b != null && !b.isEmpty()) return b;
            return fallback;
        }

        private static String userAgent() {
            return "oblodai-java/"
                    + VERSION
                    + " (contract "
                    + ContractVersion.HASH.substring(0, 12)
                    + "; jdk "
                    + System.getProperty("java.version", "?")
                    + ")";
        }

        private static void assertBaseUrl(String baseUrl, boolean allowInsecure) {
            URI parsed;
            try {
                parsed = new URI(baseUrl);
            } catch (Exception e) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG, "baseUrl is not a valid URL: " + baseUrl, "baseUrl");
            }
            if (parsed.getScheme() == null || parsed.getHost() == null) {
                throw new ConfigException(
                        ConfigException.BAD_CONFIG, "baseUrl is not a valid URL: " + baseUrl, "baseUrl");
            }
            if (parsed.getScheme().equals("https")) return;
            String host = parsed.getHost();
            boolean loopback =
                    host.equals("localhost")
                            || host.equals("127.0.0.1")
                            || host.equals("[::1]")
                            || host.equals("::1");
            if (parsed.getScheme().equals("http") && (allowInsecure || loopback)) return;
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "baseUrl must use https (got "
                            + parsed.getScheme()
                            + "://"
                            + host
                            + "); set allowInsecureBaseUrl(true) for a local gateway",
                    "baseUrl");
        }
    }
}
