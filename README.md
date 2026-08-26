<div align="center">

<a href="https://oblodai.com">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-white.svg">
    <img src="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-black.svg" alt="oblodai" height="52">
  </picture>
</a>

<h3>Official Java / Kotlin SDK for the <a href="https://oblodai.com">oblodai</a> payment gateway</h3>

Payments, payouts, payment links, splits, static wallets, webhooks — one API key.

<img src="https://img.shields.io/badge/maven-com.oblodai%3Aoblodai--sdk%201.3.0-C71A36?style=flat-square" alt="maven">
<a href="https://github.com/oblodai/oblodai-java/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/oblodai/oblodai-java/ci.yml?branch=main&style=flat-square&label=CI" alt="CI"></a>
<img src="https://img.shields.io/badge/java-17%2B-007396?style=flat-square" alt="Java 17+">
<a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-000000?style=flat-square" alt="License: MIT"></a>

[Documentation](https://docs.oblodai.com) · [Dashboard](https://my.oblodai.com) · [Читать по-русски →](README.ru.md)

</div>

---

The official Java / Kotlin SDK for the **Oblodai** payment gateway: accepting payments, payouts,
bulk operations (batches), payment links, payout links (crypto cheques), splits, static wallets,
transfers, webhooks. Request signing, response parsing, typed errors, idempotency and retries — out
of the box. Java 17 or newer, one runtime dependency (Jackson) over the JDK's own
`java.net.http.HttpClient`; the Kotlin extensions — coroutines and a request DSL — are optional
dependencies that a Java project never pulls.

> **Base URL.** Defaults to `https://api.oblodai.com`. Override `baseUrl(...)` and supply your own
> keys at initialisation if needed. The scheme must be `https://`; plain `http://` is accepted only
> for loopback (`http://127.0.0.1:8095`) or with the explicit allow-insecure option
> (`allowInsecureBaseUrl(true)`, or `OBLODAI_ALLOW_INSECURE=1`).

## Installation

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>1.3.0</version>
</dependency>
```

```gradle
implementation("com.oblodai:oblodai-sdk:1.3.0")
```

Java 17 or newer. Jackson is the only runtime dependency; the HTTP client is the JDK's own
`java.net.http.HttpClient`. Kotlin users get coroutines and a request DSL from the same artifact —
`kotlin-stdlib` and `kotlinx-coroutines` are declared `optional`, so add them yourself if you want
that half.

## Where to get keys

One API key, and it signs everything. Take it from the [dashboard](https://my.oblodai.com) under
**API keys**: a public id `oblodai_<hex>` and a secret `oblodai_live_<hex>`, shown once. That pair
signs every signed route — money in and money out alike, invoices and payouts, settings and
documents.

The sandbox pair comes from sandbox onboarding (`merchants().createSandbox(...)`, or the dashboard):
a public id `test_oblodai_<hex>` with the secret `oblodai_test_<hex>`. It behaves exactly like the
live pair against sandbox data.

A second credential exists only for platforms that onboard merchants on a self-hosted gateway: an
**admin token**, set with `adminToken(...)` (or `OBLODAI_ADMIN_TOKEN`). It is sent as `X-Admin-Token`
on the unsigned `merchants()` provisioning routes and on nothing else.

> **Legacy split keys.** Merchants onboarded before the single-key change may still hold a split
> pair — `oblodai_pk_<hex>` for money in, `oblodai_wk_<hex>` for money out — and only they can see a
> 403 `merchant.wrong_key_kind`. If you do, replace the pair with an API key in the dashboard; this
> SDK carries one pair and does not switch between kinds.

## Quick start

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
        .secret(System.getenv("OBLODAI_SECRET"))
        .build();

Payment invoice = oblodai.payments().create(new PaymentRequest()
        .amount("25")                 // amounts are decimal strings, never floats
        .currency("USDT")             // what you price in: a fiat (USD, EUR, …) or a crypto asset
        .network(Network.TRON)        // omit it to let the payer choose the network on the pay page
        .orderId("order-1001")        // your reference; the invoice is idempotent per order_id
        .urlCallback("https://shop.example/oblodai/webhook"));

System.out.println(invoice.url() + " " + invoice.address() + " " + invoice.status()); // created
```

Price in fiat with `.amount("25").currency("USD").toCurrency("USDT")` — `currency` is what you
charge, `to_currency` the asset the payer sends.

Money out uses the same key. Dry-run it first, then create it under your own idempotency key so a
lost response can never become a second payout:

```java
PayoutValidation check = oblodai.payouts().validate(new PayoutValidateRequest()
        .amount("10").currency("USDT").network(Network.TRON).address(address));

Payout payout = oblodai.payouts().create(new PayoutRequest()
        .amount("10").currency("USDT").network(Network.TRON).address(address)
        .orderId("payout-42"), RequestOptions.of().idempotencyKey("payout-42"));

System.out.println(check.commission() + " fee, payout " + payout.uuid() + " " + payout.status());
```

Runnable programs live in [`examples/`](examples).

### Amounts

Amounts are decimal strings end to end (USDT has 6 decimals, BTC 8, ETH 18). Never parse one into a
`double`, and never compare two amount strings with `String.compareTo` — `"10"` sorts before `"9"`.

```java
System.out.println(Money.add("10.000000", "0.5"));      // "10.500000"
System.out.println(Money.compare("25", "25.000000"));   // 0 — equal at any scale
System.out.println(Money.isZero("0.000000"));           // true
System.out.println(Money.toBigDecimal("1.5"));          // when you deliberately want one
```

Anything that is not a plain decimal (`"1e3"`, an empty string, 64+ characters) is a
`ConfigException` with code `sdk.bad_amount`, refused before it is sent. A JSON number arriving
where the contract says string is refused too, rather than silently stringified.

### Async

```java
OblodaiAsync client = oblodai.async();                 // same engine, connections and clock
CompletableFuture<Payment> invoice = client.payments().create(request);
CompletableFuture<List<Payout>> all = client.payouts().history().all(500);
```

Every future fails with an `OblodaiException` as its cause, and `cancel(true)` on any future the SDK
returned aborts the HTTP exchange in flight and stops the retry loop.

### Kotlin

The Java SDK is the Kotlin SDK: `com.oblodai.kotlin` adds coroutines and builders on the same types.

```kotlin
val oblodai = Oblodai.builder().publicId(id).secret(secret).build().async()

val invoice = oblodai.payments().create(payment {
    amount("25"); currency("USDT"); network(Network.TRON); orderId("order-1001")
}).await()

oblodai.payments().history().asFlow().take(100).collect { println(it.uuid()) }
```

`await()` throws the SDK's own exception rather than a `CompletionException` wrapper, and cancelling
the coroutine cancels the call. `asFlow()` is pull-based: it asks for the next page only once the
collector has taken the last item of the previous one, so nothing is dropped and nothing is fetched
that the collector never reads.

## Sandbox / testing

The sandbox is a chainless copy of the gateway: fake balance from a faucet, simulated deposits, real
signed webhooks. Integrate against it first — a sandbox key is the only thing that changes.

```java
Oblodai sandbox = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))       // test_oblodai_…
        .secret(System.getenv("OBLODAI_SECRET"))            // oblodai_test_…
        .build();

sandbox.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("100"));
sandbox.sandbox().deposit(new SandboxDepositRequest().invoiceId(invoiceId).amount("25"));

sandbox.webhooks().testPayment(new TestWebhookPaymentRequest().uuid(invoiceId).status("paid"));
for (WebhookDelivery delivery : sandbox.sandbox().webhooks()) System.out.println(delivery.status());

sandbox.sandbox().reset();                                  // cancels open invoices, zeroes balances
```

`sandbox().replay(deliveryId)` re-sends a delivery you have already seen. Rehearsal deliveries — from
`webhooks().testPayment/testPayout/testWallet(...)` or from the sandbox — are signed exactly like
live ones and carry `test: true` in the signed body and `X-Webhook-Test: true` on the request, so
`delivery.isTest()` is true for them: never act on one as if money moved.

`faucet(...)` and `reset()` are signed like every other route — the sandbox key calls them itself.

## Method overview

Sixteen namespaces cover all 107 merchant routes of the contract snapshot.

| Namespace        | Methods                                                                                                                                                                                     | Routes |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -----: |
| `payments()`     | create · info/get · cancel · history/list · batch · qr · services · sendEmail · resend · publicView · select · publicQr                                                                       |     12 |
| `refunds()`      | create · resolve · batch                                                                                                                                                                     |      3 |
| `payouts()`      | create · validate · calculate · info/get · cancel · approve · history/list · mass · batch · services · get/setFeeConfig · get/setRefundFeeConfig                                              |     14 |
| `payoutLinks()`  | create · info/get · list · cancel · batch · cheque · claimPreview · claim                                                                                                                     |      8 |
| `paymentLinks()` | create · info/get · list · toggle · publicView · checkout                                                                                                                                    |      6 |
| `batches()`      | info (poll an asynchronous batch)                                                                                                                                                            |      1 |
| `transfers()`    | toPersonal · toUser · batch                                                                                                                                                                  |      3 |
| `wallets()`      | create · qr · block · refundBlockedDeposit                                                                                                                                                   |      4 |
| `webhooks()`     | register · rotateSecret · deliveries · testPayment/testPayout/testWallet · test(kind, …)                                                                                                      |      7 |
| `documents()`    | statement · ledger · balanceCertificate · feeSchedule · splitReport · batchReport · linkReport · walletStatement · referralsReport · createJob · jobInfo · jobFile · download                  |     13 |
| `splits()`       | createRule · listRules · deleteRule · get/setConfig · get/setOptIn                                                                                                                            |      7 |
| `settings()`     | setDiscount · listDiscounts · get/setAccuracy · get/setAutoRefund · listAccepted · setAccepted · get/setPaymentFeeConfig · list/set/deleteAutoWithdraw · list/add/remove/enableApiAllowlist    |     17 |
| `account()`      | balance · referral · vrcs                                                                                                                                                                    |      3 |
| `catalog()`      | currencies · exchangeRates                                                                                                                                                                   |      2 |
| `sandbox()`      | faucet · deposit · webhooks · replay · reset                                                                                                                                                 |      5 |
| `merchants()`    | create · createSandbox (provisioning; unsigned, `adminToken(...)` on a self-hosted gateway)                                                                                                   |      2 |

Lookups accept a bare id or a request object: `payments().info(uuid)`,
`payments().info(new PaymentInfoRequest().orderId("order-1001"))`. Synchronous batches are capped per
call — `payouts().mass(...)` at 100 elements, `payoutLinks().batch(...)` at 500 — and report each
element separately, so a 200 can still contain failures. Asynchronous batches (`payments().batch`,
`payouts().batch`, `refunds().batch`, `transfers().batch`) take up to 5000 elements and are polled
with `batches().info(...)`. Documents come back as a `FileResult` (`bytes`, `contentType`,
`filename`). Payer-facing routes — `payments().publicView/select/publicQr`,
`paymentLinks().publicView/checkout`, `payoutLinks().claimPreview/claim` — need no credentials.

### Lists

List methods return a `Pager<T>`. Nothing is requested until you consume it, and the walk follows the
gateway's own `paginate.has_pages` flag.

```java
Page<Payment> page = oblodai.payments().history(new PaymentHistoryRequest().limit(50)).firstPage();

for (Payout payout : oblodai.payouts().history(new PayoutHistoryRequest().status("confirmed"))) {
    process(payout);                                    // one page fetched at a time
}

List<Payout> refunds = oblodai.payouts().history(new PayoutHistoryRequest().kind("refund")).all(1000);
Stream<Payment> stream = oblodai.payments().history().stream();
```

### Statuses

- Invoice: `select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled`.
  `Statuses.isPaymentPaid(...)` covers `paid`/`paid_over`; `wrong_amount` (underpaid) waits for
  `refunds().resolve(...)`; `Statuses.isPaymentFinal(...)` covers the rest.
- Payout: `pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled`.

Prefer webhooks for state changes; poll `info` only as a fallback.

The vocabularies (`PaymentStatus`, `PayoutStatus`, `Network`, …) are **open**: the values this
snapshot knows are interned constants you can compare with `==`, and a value the gateway starts
sending that this SDK has never heard of arrives as an instance carrying that exact string —
`status.wire()` is what the gateway said, `status.isKnown()` says it is new. They are not Java enums,
so there is no `switch` over them and no `UNKNOWN` constant. An event kind newer than this SDK
arrives the same way, as an `UnknownEvent` carrying the raw `type`. A gateway that grows its
vocabulary can neither break a deployed client nor have what it said thrown away.

## Webhooks

Register an endpoint with `webhooks().register(url)`; the response carries the signing secret, shown
once. Then verify every delivery over the **raw** request bytes — a framework that parsed and
re-serialized the body has already changed them, and the signature will not match.

```java
byte[] rawBody = exchange.getRequestBody().readAllBytes();   // the RAW bytes, always

WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody,
        WebhookHeaders.ofMulti(exchange.getRequestHeaders()), // or of(Map), or a lambda
        WebhookVerifier.options(secret).previousSecret(previousSecret));

if (delivery.isTest()) return;                               // a rehearsal: no money moved
if (!seen.add(delivery.id())) return;                        // X-Webhook-Id, stable across retries
if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
    markOrderPaid(payment.orderId());
}
```

The checks run headers → MAC → freshness → body, so the freshness window is never an oracle for an
unauthenticated caller. A secret that is null or blank, and a negative tolerance, are a
`ConfigException` before any crypto (`Duration.ZERO` disables the freshness check deliberately,
sub-second windows are honoured). Deduplicate on `delivery.id()`; drop out-of-order deliveries with
`WebhookVerifier.isStale(event, lastSequence)`. After `webhooks().rotateSecret()` keep the previous
secret for at least 26 hours: deliveries queued before the rotation stay signed with it for their
whole retry life. Verification is a standalone class — no client, no API key, no network.

An event type this snapshot does not know decodes to an `UnknownEvent` that keeps the raw string;
`WebhookVerifier.isKnownEvent(event)` tells them apart. What your receiver answers matters, because
the gateway retries anything that is not 2xx:

```java
WebhookEvent event;
try {
    event = WebhookVerifier.verify(rawBody, headers, options);
} catch (SignatureException notFromTheGateway) {
    return 401;                       // the ONLY failure that deserves a 401
} catch (WebhookPayloadException unreadable) {
    return 400;                       // authentic delivery, body is not an event: webhook.bad_payload
}
if (!WebhookVerifier.isKnownEvent(event)) return 200;   // newer than this SDK: acknowledge, ignore
```

## Errors

Every failure is an `OblodaiException` carrying the API's error envelope: `code()`
(`payout.insufficient_funds`), `httpStatus()`, `retryable()`, `retryAfter()`, `requestId()`,
`field()` (400s), `synthetic()` (a proxy answered, not the gateway). Quote `requestId()` to support.

| Class                                             | HTTP        | When                                                        |
| ------------------------------------------------- | ----------- | ----------------------------------------------------------- |
| `ValidationException`                             | 400         | the request is malformed or a field is rejected              |
| `AuthenticationException`                         | 401         | bad signature, bad timestamp, unknown public id              |
| `PermissionException`                             | 403         | IP not on the allow-list, feature disabled                   |
| `NotFoundException`                               | 404         | no such object                                               |
| `ConflictException` / `IdempotencyConflictException` | 409       | state conflict; the key was reused with a different body     |
| `RateLimitException`                              | 429         | rate limited — honour `retryAfter()`                         |
| `UnavailableException`                            | 503         | gateway temporarily unavailable                              |
| `InternalException`                               | other 5xx   | gateway fault                                                |
| `TransportException`                              | no response | connection failure, timeout, deadline                        |
| `ConfigException`                                 | not sent    | `sdk.bad_config`, `sdk.missing_credentials`, `sdk.bad_idempotency_key`, `sdk.idempotency_unsupported`, `sdk.bad_path_param`, `sdk.bad_header`, `sdk.bad_amount` |
| `ContractException`                               | unreadable  | `sdk.bad_envelope`, `sdk.response_too_large`                 |
| `WebhookPayloadException`                         | —           | `webhook.bad_payload`: authentic delivery, not an event      |
| `SignatureException`                              | —           | webhook signature or freshness check failed                  |

A code is `family.reason`, and the contract snapshot carries all 469 of them — `ErrorCodes.ALL`, and
`ErrorCodes.isKnown(code)` for one. Branch on the code, not on the message:

```java
try {
    oblodai.payouts().create(request);
} catch (OblodaiException e) {
    switch (e.code()) {
        case "payout.insufficient_funds", "payout.funds_maturing" ->
                scheduleRetry(e.retryAfter() == null ? 60 : e.retryAfter());
        default -> throw e;   // the SDK already retried what was safe to retry
    }
}
```

Codes worth handling by name: `payout.insufficient_funds` and `payout.funds_maturing` (both
retryable), `idempotency.key_reused`, `invoice.not_payable`, `payment.not_found`,
`merchant.bad_signature`, `request.rate_limited`.

`toString()` and `details()` never include the raw response body, so a log cannot spill an invoice
payload or a cheque passcode; `raw()` is there for deliberate inspection.

## Retries, idempotency and timeouts

- An error is retried only when the API says `retryable`. Answers without an API envelope (a proxy
  502/503) and transport failures are retried only on read routes or keyed writes. "Read route" is
  not a guess about the path: it is the `safe` flag the gateway states for each route in the contract
  snapshot. `Retry-After` wins over the computed backoff, capped by `maxRetryAfterMs`.
- Create-type routes get an `Idempotency-Key` automatically — one per logical call, reused on every
  retry — so a timeout can never produce a second payout. Pass your own key to make retries safe
  across process restarts. On routes the gateway does not deduplicate the SDK **refuses** a key
  (`sdk.idempotency_unsupported`) — list methods included, where it is refused as the pager is built
  rather than quietly dropped: the header would be ignored, and only the SDK would believe a re-send
  was safe.
- Every method takes an optional last argument, `RequestOptions` — every method, including the
  aliases (`get`, `list`) and the no-argument list forms:

```java
oblodai.payouts().create(request, RequestOptions.of()
        .idempotencyKey(orderId)                  // your own key; generated for you when omitted
        .timeout(Duration.ofSeconds(10))          // per attempt
        .deadline(Duration.ofSeconds(45))         // whole call, retries and pauses included
        .header("X-Tenant", "acme"));             // one call only, on top of the client-wide headers
```

- On a 401 that means a bad signature or timestamp, the SDK reads the server's `Date`, re-signs once,
  and keeps the offset only if that attempt got past authentication. The offset is shared by every
  call the client makes and corrected atomically, so concurrent calls on a skewed host converge on
  one correction instead of undoing each other's.
- Redirects are never followed; if an injected `HttpClient` follows one, the SDK notices the answer
  came from another URL and fails the call.
- A response body is read under a ceiling — 8 MiB for a JSON envelope, 64 MiB for a document — and a
  larger answer fails the call with `sdk.response_too_large` instead of the process.

## Configuration

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(id).secret(secret)
        .baseUrl("https://api.oblodai.com")
        .timeout(Duration.ofSeconds(30))
        .deadline(Duration.ofSeconds(90))
        .retry(new RetryOptions(2, 250, 4_000, 30_000))
        .header("X-Tenant", "acme")
        .logger(Logger.console(Logger.Level.INFO))
        .build();
```

| Builder option                       | Default                                     | What it does                                                     |
| ------------------------------------ | ------------------------------------------- | ---------------------------------------------------------------- |
| `publicId(…)` / `secret(…)`          | `OBLODAI_PUBLIC_ID` / `OBLODAI_SECRET`      | the merchant's API key; the secret signs and is never sent        |
| `adminToken(…)`                      | `OBLODAI_ADMIN_TOKEN`                       | `X-Admin-Token`, sent on merchant provisioning routes only        |
| `baseUrl(…)`                         | `OBLODAI_BASE_URL`, else `https://api.oblodai.com` | API origin; a path prefix is kept                          |
| `allowInsecureBaseUrl(boolean)`      | `OBLODAI_ALLOW_INSECURE=1`, else `false`    | permits a plain-http base URL away from loopback                  |
| `timeout(Duration)`                  | 30 s                                        | per attempt                                                       |
| `deadline(Duration)`                 | 90 s                                        | whole call, retries and pauses included                           |
| `retry(RetryOptions)`                | 2 retries, 250 ms base, 4 s cap, 30 s `Retry-After` cap | `RetryOptions.none()` disables retrying                |
| `header(name, value)`                | —                                           | a header on every request; names the SDK owns are refused          |
| `logger(Logger)`                     | `OBLODAI_LOG`, else silent                  | structured logging; fields arrive already redacted                |
| `objectMapper(ObjectMapper)`         | the SDK's own configuration                 | JSON mapper to decode with                                        |
| `httpClient(HttpClient)`             | one the SDK builds and closes itself        | control proxies, TLS, executors                                   |
| `clock(SkewCorrectingClock)`         | a clock that learns the gateway's time      | the signing clock                                                 |
| `environment(Map<String,String>)`    | `System.getenv()`                           | replaces the environment the fallbacks read, for tests            |

| Environment variable         | Effect                                                        |
| ---------------------------- | ------------------------------------------------------------- |
| `OBLODAI_PUBLIC_ID`          | public id of the API key                                       |
| `OBLODAI_SECRET`             | secret of the API key                                          |
| `OBLODAI_ADMIN_TOKEN`        | admin token of a self-hosted gateway                           |
| `OBLODAI_BASE_URL`           | API origin                                                     |
| `OBLODAI_LOG`                | `debug` \| `info` \| `warn` \| `error` — log to stderr         |
| `OBLODAI_ALLOW_INSECURE`     | `1` permits a plain-http base URL                              |

An option set explicitly wins; otherwise the environment is read, then the default.

**Redaction.** The transport redacts sensitive values *before* handing fields to a logger, so an
injected logger never sees a key, a signature or a cheque passcode — it is not something the logger
implementation has to remember. The same goes for models that carry a shown-once value —
`WebhookEndpoint.secret`, `WebhookSecretRotated.secret`, `ApiKeyPair.secret`,
`PayoutLink.claimToken`/`claimUrl`/`passcode`: readable through their accessor, `[redacted]` in
`toString()` and in JSON.

**Self-hosted or local gateway.** `baseUrl("http://127.0.0.1:8095")` works out of the box; other
plain-http hosts need `allowInsecureBaseUrl(true)`. A path prefix in `baseUrl` is kept
(`https://gw.corp/oblodai` → `https://gw.corp/oblodai/v1/payment`).

```java
Oblodai.builder().baseUrl("http://127.0.0.1:8095").build();
Oblodai.builder().baseUrl("http://gw.corp").allowInsecureBaseUrl(true).build();
```

`header(...)` refuses the names the SDK owns (`Accept`, `Content-Type`, `User-Agent`, `X-Public-Id`,
`X-Signature`, `X-Timestamp`, `Idempotency-Key`, `X-Admin-Token`) with `sdk.bad_header`, so the admin
token is not something a caller can bolt onto every request.

The client is `AutoCloseable`: `close()` releases the HTTP client it built for itself (a no-op on
JDK 17, where `HttpClient` has no close operation). One client per key pair, kept for the life of the
process, is the intended shape.

## The contract snapshot

`contract/` is exported by the gateway's own test suite from core commit `2cc44c16f516`: the route
registry (107 merchant routes, each with its method, path, gate — `public`, `key` or `onboard` —
`idempotent`, `safe`, `bare` and list shape), request DTO schemas with English field docs, enums, all
469 error codes, signing vectors, golden response bodies recorded from a live gateway and real signed
webhook deliveries.

`src/main/java/com/oblodai/contract/` is generated from it by `codegen/run.sh` — 92 files, 107 routes,
469 error codes, 76 request types. `codegen/run.sh --check` is the drift gate: it fails the build when
the committed sources and the snapshot disagree, and `mvn verify` runs it first. The contract tests
then check every model against the golden bodies and every route against the registry.

To refresh: replace `contract/`, run `codegen/run.sh`, run `mvn -o verify`, and update the counts
quoted here and in [AGENTS.md](AGENTS.md).

## Development

```bash
git clone https://github.com/oblodai/oblodai-java && cd oblodai-java
mvn -o verify                                          # drift gate + compile + 521 offline tests + jars
mvn -o test                                            # tests only
codegen/run.sh                                         # regenerate after refreshing contract/
codegen/run.sh --check                                 # what the drift gate runs
OBLODAI_LIVE_URL=http://127.0.0.1:8095 mvn -o verify   # adds the 18 live tests
mvn -Prelease deploy                                   # publish to Maven Central; see RELEASING.md
```

The live tier onboards a merchant on the gateway under test, takes a sandbox key and walks the whole
money path, including a real signed webhook delivered to a receiver the test starts. Without
`OBLODAI_LIVE_URL` those tests are skipped.

Every snippet in this file and in [README.ru.md](README.ru.md) is compiled as part of the test build
(`examples/java/com/oblodai/examples/DocSnippets.java`,
`examples/kotlin/com/oblodai/examples/DocSnippetsKotlin.kt`), and `DocSnippetsTest` checks that the
two READMEs carry the same code and that every snippet really appears there.

Writing code with an AI agent? Point it at [AGENTS.md](AGENTS.md). See also
[CHANGELOG.md](CHANGELOG.md) and [MIGRATION-1.3.md](MIGRATION-1.3.md).

## License

MIT — see [LICENSE](LICENSE).
