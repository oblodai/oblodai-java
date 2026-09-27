package com.oblodai;

import com.oblodai.core.Credentials;
import com.oblodai.core.Hooks;
import com.oblodai.core.Json;
import com.oblodai.core.Logger;
import com.oblodai.core.RequestBuilder;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.SkewCorrectingClock;
import com.oblodai.core.Sleeper;
import com.oblodai.core.Transport;
import com.oblodai.errors.ConfigException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Everything {@link Oblodai.Builder} collects, and the one place that turns it into a
 * {@link Transport}: option resolution, the environment fallbacks and the base-URL rules live here
 * so the builder itself stays a list of setters.
 */
final class ClientSettings {

    /** The deprecated admin token is warned about once per process. */
    private static final java.util.concurrent.atomic.AtomicBoolean ADMIN_TOKEN_WARNED =
            new java.util.concurrent.atomic.AtomicBoolean();

    String publicId;
    String secret;
    String adminToken;
    String baseUrl;
    HttpClient httpClient;
    RetryOptions retry = RetryOptions.DEFAULT;
    Logger logger;
    Hooks hooks = Hooks.NONE;
    Sleeper sleeper;
    SkewCorrectingClock clock;
    long timeoutMs = 30_000;
    long deadlineMs = 90_000;
    Boolean allowInsecureBaseUrl;
    final Map<String, String> headers = new LinkedHashMap<>();
    Map<String, String> environment = System.getenv();

    /** True when the client created the HTTP client itself and may therefore close it. */
    boolean ownsHttpClient;

    /** Builds the engine both client shapes share. */
    Transport buildTransport() {
        String resolvedBaseUrl = firstSet(baseUrl, env("OBLODAI_BASE_URL"), Oblodai.DEFAULT_BASE_URL);
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

        HttpClient client = httpClient;
        if (client != null && client.followRedirects() != HttpClient.Redirect.NEVER) {
            // A following client re-sends the signed headers, the idempotency key and (307/308) the
            // body to the redirect target before the SDK can notice.
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "the injected HttpClient follows redirects; build it with"
                            + " followRedirects(HttpClient.Redirect.NEVER)",
                    "httpClient");
        }
        if (client == null) {
            client =
                    HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
            ownsHttpClient = true;
        }

        Logger resolvedLogger = logger != null ? logger : loggerFromEnvironment();
        if (firstSet(adminToken, env("OBLODAI_ADMIN_TOKEN"), null) != null
                && ADMIN_TOKEN_WARNED.compareAndSet(false, true)) {
            resolvedLogger.warn(
                    "adminToken / OBLODAI_ADMIN_TOKEN is deprecated and ignored: the SDK never sends a"
                            + " raw admin token; operator operations need the dashboard",
                    Map.of());
        }

        return new Transport(
                new Transport.Config(
                        resolvedBaseUrl,
                        id == null ? null : new Credentials(id, key),
                        client,
                        retry,
                        clock != null ? clock : new SkewCorrectingClock(),
                        resolvedLogger,
                        timeoutMs,
                        deadlineMs,
                        Map.copyOf(headers),
                        null,
                        userAgent(),
                        Json.mapper(),
                        hooks,
                        sleeper != null ? sleeper : Sleeper.DEFAULT));
    }

    /**
     * The transport of a client copy with other defaults.
     *
     * @param transport the original client's transport
     * @param defaults the new defaults
     * @return the derived transport, sharing connections and clock
     */
    static Transport derive(Transport transport, RequestOptions defaults) {
        if (defaults == null) {
            return transport;
        }
        if (defaults.idempotencyKey() != null || defaults.requestId() != null) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "withOptions: idempotencyKey and requestId name one call; pass them per call",
                    defaults.idempotencyKey() != null ? "idempotencyKey" : "requestId");
        }
        Transport.Config c = transport.config();
        Map<String, String> headers = new LinkedHashMap<>(c.headers());
        headers.putAll(defaults.extraHeaders());
        return transport.derive(
                new Transport.Config(
                        c.baseUrl(),
                        c.credentials(),
                        c.httpClient(),
                        defaults.maxRetries() != null
                                ? c.retry().withMaxRetries(defaults.maxRetries())
                                : c.retry(),
                        c.clock(),
                        c.logger(),
                        defaults.timeout() != null ? defaults.timeout().toMillis() : c.timeoutMs(),
                        c.deadlineMs(),
                        Map.copyOf(headers),
                        c.adminToken(),
                        c.userAgent(),
                        c.mapper(),
                        c.hooks(),
                        c.sleeper()));
    }

    /**
     * Releases an HTTP client the SDK created for itself. On a JDK where {@code HttpClient} is
     * closeable (21 and later) this shuts it down; on 17 it has no close operation and its
     * connections are released once it becomes unreachable, so this does nothing there.
     *
     * @param httpClient the client to release, or null
     */
    static void closeHttpClient(Object httpClient) {
        if (httpClient == null) return;
        try {
            httpClient.getClass().getMethod("close").invoke(httpClient);
        } catch (ReflectiveOperationException | RuntimeException noCloseOnThisJdk) {
            // JDK 17: HttpClient is not closeable. Nothing to do.
        }
    }

    /** Records a header for every request, refusing one the HTTP layer could not carry. */
    void header(String name, String value) {
        RequestBuilder.assertCallerHeader(name, value);
        headers.put(name, value);
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
                + Oblodai.VERSION
                + " (jdk "
                + System.getProperty("java.version", "?")
                + ")";
    }

    private static void assertBaseUrl(String baseUrl, boolean allowInsecure) {
        // Never echo the URL itself: it may carry credentials.
        URI parsed;
        try {
            parsed = new URI(baseUrl);
        } catch (Exception e) {
            throw new ConfigException(ConfigException.BAD_CONFIG, "baseUrl is not a valid URL", "baseUrl");
        }
        if (parsed.getScheme() == null || parsed.getHost() == null) {
            throw new ConfigException(ConfigException.BAD_CONFIG, "baseUrl is not a valid URL", "baseUrl");
        }
        if (parsed.getRawUserInfo() != null) {
            throw new ConfigException(
                    ConfigException.BAD_CONFIG,
                    "baseUrl must not carry credentials (user:pass@); the SDK signs requests itself",
                    "baseUrl");
        }
        if (parsed.getScheme().equals("https")) return;
        String host = parsed.getHost();
        if (parsed.getScheme().equals("http") && allowInsecure) return;
        throw new ConfigException(
                ConfigException.BAD_CONFIG,
                "baseUrl must use https (got "
                        + parsed.getScheme()
                        + "://"
                        + host
                        + "); set allowInsecureBaseUrl(true) (or OBLODAI_ALLOW_INSECURE=1) for a local"
                        + " gateway",
                "baseUrl");
    }
}
