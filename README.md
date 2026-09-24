<div align="center">

<a href="https://oblodai.com">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-white.svg">
    <img src="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-black.svg" alt="oblodai" height="52">
  </picture>
</a>

<h3>Official Java / Kotlin SDK for the <a href="https://oblodai.com">oblodai</a> payment gateway</h3>

Payments, payouts, payment links, splits, static wallets, webhooks — one API key.

<img src="https://img.shields.io/badge/maven-com.oblodai%3Aoblodai--sdk%202.0.0-C71A36?style=flat-square" alt="maven">
<a href="https://github.com/oblodai/oblodai-java/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/oblodai/oblodai-java/ci.yml?branch=main&style=flat-square&label=CI" alt="CI"></a>
<img src="https://img.shields.io/badge/java-17%2B-007396?style=flat-square" alt="Java 17+">
<a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-000000?style=flat-square" alt="License: MIT"></a>

[Documentation](https://docs.oblodai.com) · [Dashboard](https://my.oblodai.com) · [Читать по-русски →](README.ru.md)

</div>

---

The official Java / Kotlin SDK for the **Oblodai** payment gateway: payments, payouts, batches,
payment links, payout links (crypto cheques), splits, static wallets, webhooks, documents. Java 17
or newer; one runtime dependency (Jackson, for the JSON tree) over the JDK's own
`java.net.http.HttpClient`. Kotlin gets coroutines and flows from the same artifact.

Version 2 is generated from the gateway's OpenAPI contract: every resource, method and model under
`com.oblodai.generated` comes from `openapi.json` by `tools/sdkgen`, and a drift check keeps it
that way. The runtime around it - signing, retries, idempotency, pagination, errors, webhooks - is
written by hand, once. Coming from 1.x? See [MIGRATION-2.0.md](MIGRATION-2.0.md).

## Installation

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>2.0.0</version>
</dependency>
```

```gradle
implementation("com.oblodai:oblodai-sdk:2.0.0")
```

## Keys

One API key signs every route: a public id `oblodai_<hex>` and a secret `oblodai_live_<hex>` from the
[dashboard](https://my.oblodai.com) (sandbox: `test_oblodai_<hex>` / `oblodai_test_<hex>`). They
fall back to `OBLODAI_PUBLIC_ID` and `OBLODAI_SECRET`. The only other credential is the admin token
of a self-hosted gateway (`adminToken(...)`, `OBLODAI_ADMIN_TOKEN`), sent on the onboarding route
alone.

## Quick start

Configure the client once and share it: it is immutable and thread-safe.

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
        .secret(System.getenv("OBLODAI_SECRET"))
        .build();
```

Accept a payment:

```java
PaymentView invoice = oblodai.payments().create(PaymentRequest.builder()
        .amount("25")                  // a decimal string or a BigDecimal, never a double
        .currency("USDT")
        .orderId("order-1001")         // your reference; the invoice is idempotent per order
        .urlCallback("https://shop.example/oblodai/webhook")
        .build());

System.out.println(invoice.url());     // send the payer here
```

Read it back (webhooks are the source of truth; this is for reconciliation):

```java
PaymentInfoResult now = oblodai.payments().getInfo(
        LookupRequest.builder().uuid(invoice.uuid()).build());

if (Statuses.isPaymentPaid(now.status())) {
    BigDecimal received = Money.toBigDecimal(now.merchantAmount());   // exact, never a double
    System.out.println("paid, " + received + " credited");
}
```

Send a payout under your own idempotency key - a retry after a crash can never pay twice:

```java
PayoutItem payout = oblodai.payouts().create(
        PayoutRequest.builder()
                .amount(new BigDecimal("10.50"))
                .currency("USDT")
                .network("tron")
                .address("TXYZ")
                .orderId("withdrawal-77")
                .build(),
        RequestOptions.of().idempotencyKey("withdrawal-77"));   // one key for every retry
```

### Amounts

Amounts are `BigDecimal` in the models and decimal strings on the wire. A builder takes a
`BigDecimal` or a decimal string (`.amount("10.50")`); there is no `double` overload, and a `double`
that slips in anyway (`putExtra`, a parsed map) fails with `sdk.float_amount` before anything is
sent. `Money` adds, subtracts and compares amounts exactly and refuses a `double` too.

## Resources and methods

`client.<resource>().<method>(params, options)`: the resource is the contract's tag, the method its
`operationId` without the resource name. Parameters are a model built with its builder
(`PaymentRequest.builder()...build()`); path parameters come first, query parameters are a
`*Query` model. Every method has an overload with a trailing `RequestOptions`, and overloads
without the optional parts. The full list is in [`names.lock`](names.lock).

| resource | methods |
| --- | --- |
| `account()` | `getBalance`, `getSummary`, `listExchangeRates` |
| `apiAllowlist()` | `addEntry`, `list`, `removeEntry`, `setEnabled` |
| `batches()` | `createPayment`, `createPayout`, `createRefund`, `getInfo` |
| `checkout()` | `get`, `getOnramp`, `getPublicPaymentLink`, `getQr`, `getSourceOfFundsForm`, `listCurrencies`, `paymentLink`, `selectMethod`, `startOnramp`, `submitSourceOfFunds` |
| `documents()` | `createJob`, `downloadJobFile`, `getBalance`, `getBatch`, `getFees`, `getJob`, `getLedger`, `getPaymentLink`, `getPayoutLinkCheque`, `getReferrals`, `getSigned`, `getSplit`, `getStatement`, `getWalletStatement` |
| `paymentLinks()` | `create`, `get`, `list`, `toggle` |
| `payments()` | `cancel`, `create`, `getAmlLinks`, `getCheckoutConfig`, `getInfo`, `getQr`, `listHistory`, `listServices`, `resolve`, `sendEmail`, `setCheckoutConfig` |
| `payoutLinks()` | `cancel`, `claimPayout`, `create`, `createBatch`, `get`, `getPayoutClaim`, `list` |
| `payouts()` | `approve`, `calculate`, `cancel`, `create`, `createMass`, `createTransferBatch`, `getInfo`, `listHistory`, `listServices`, `transferToPersonal`, `transferToUser`, `validate` |
| `referrals()` | `getInfo` |
| `refunds()` | `blockedWallet`, `payment` |
| `sandbox()` | `faucet`, `listWebhooks`, `onboardStore`, `replayWebhook`, `reset`, `simulateDeposit` |
| `settings()` | `configureVrcs`, `deleteAutoWithdrawRule`, `getAccuracy`, `getAutoConvert`, `getAutoRefund`, `getPaymentFeeConfig`, `getPayoutFeeConfig`, `getRefundFeeConfig`, `listAcceptedCurrencies`, `listApiLog`, `listAutoWithdrawRules`, `listDiscounts`, `setAcceptedCurrencies`, `setAccuracy`, `setAutoConvert`, `setAutoRefund`, `setAutoWithdrawRule`, `setDiscount`, `setPaymentFeeConfig`, `setPayoutFeeConfig`, `setRefundFeeConfig` |
| `splits()` | `createRule`, `deleteRule`, `getConfig`, `getRecipientOptIn`, `listRules`, `setConfig`, `setRecipientOptIn` |
| `wallets()` | `block`, `create`, `getQr` |
| `webhooks()` | `listDeliveries`, `register`, `requeueDelivery`, `resendPayment`, `rotateSecret`, `sendLegacyTest`, `sendTestConversion`, `sendTestPayment`, `sendTestPayout`, `sendTestWallet`, `setActive` |

Models keep fields this SDK version does not know yet (`extra()`, sent back as they came), and an
enum value it does not know parses (`PaymentStatus.of("new_status").isKnown() == false`).
`toString()` is short and never shows a secret.

## Per-call options

Five options, all explicit: `idempotencyKey`, `timeout` (a `Duration`, per attempt), `maxRetries`,
`extraHeaders` and `requestId` (sent as `X-Request-ID`; a fresh UUID when not set, the same on
every attempt of the call). `withOptions(...)` makes a copy of the client with other defaults.

```java
oblodai.account().getBalance(RequestOptions.of()
        .timeout(Duration.ofSeconds(5))       // per attempt
        .maxRetries(0)                        // this call only
        .extraHeader("X-Trace", "t-42")
        .requestId("order-1001-balance"));    // sent as X-Request-ID

Oblodai patient = oblodai.withOptions(RequestOptions.of().timeout(Duration.ofSeconds(60)));
```

## Lists

A list method returns a lazy `Pager`: iterate it for every item, `byPage()` for every page,
`firstPage()` for one, `all(max)` to collect. Nothing is requested until it is consumed.

```java
int seen = 0;
for (PaymentView p : oblodai.payments().listHistory()) {            // every item, page by page
    System.out.println(p.orderId() + " " + p.status());
    seen++;
}
for (Page<PaymentView> page : oblodai.payments().listHistory(
        HistoryRequest.builder().limit(100L).build()).byPage()) {  // one request per page
    System.out.println(page.items().size() + " of " + page.total());
}
```

## Errors

Everything the SDK throws is an unchecked `OblodaiException` with `code()` (`family.reason`),
`httpStatus()`, `retryable()`, `retryAfter()`, `requestId()` and `field()`; `getMessage()` reads
`[code] text (request_id=...)`. Subclasses by status: `ValidationException` 400,
`AuthenticationException` 401, `PermissionException` 403, `NotFoundException` 404,
`ConflictException` / `IdempotencyConflictException` 409, `RateLimitException` 429,
`UnavailableException` 503, `InternalException`, `TransportException` (no response),
`ConfigException` (refused before sending), `ContractException` (an answer the SDK cannot read).

```java
String code = null;
try {
    oblodai.payments().getInfo(LookupRequest.builder().uuid("missing").build());
} catch (OblodaiException e) {
    System.err.println(e.getMessage());   // [payment.not_found] … (request_id=…)
    code = e.code();                      // retryable(), field(), requestId() as well
}
```

## Retries and idempotency

A call is retried (two retries by default, exponential backoff with jitter, `Retry-After`
honoured) only when repeating it cannot duplicate an effect: a read-only route, or a write the
gateway deduplicates by `Idempotency-Key`. The SDK generates that key for such routes and reuses it
on every attempt; your own key (`RequestOptions.idempotencyKey`) makes it survive a restart. A
write the gateway does not deduplicate is never re-sent after a timeout or a proxy error, and a key
passed to it is refused (`sdk.idempotency_unsupported`). A 409 `idempotency.in_progress` waits and
repeats with the same key. One deadline (90 s by default) bounds the whole call.

## Raw responses and hooks

```java
ApiResponse<PaymentView> raw = oblodai.withRawResponse(c -> c.payments().create(
        PaymentRequest.builder().amount("25").currency("USDT").build()));
System.out.println(raw.status() + " " + raw.requestId() + " " + raw.value().uuid());

Oblodai observed = Oblodai.builder()
        .onRequest(r -> System.out.println("-> " + r.operationId() + " #" + r.attempt()))
        .onResponse(r -> System.out.println("<- " + r.status() + " in " + r.elapsed()))
        .build();
observed.close();
```

## Long-running operations

Batches and document exports finish in the background. `jobs()` follows them: `waitFor()` polls
until the status is terminal (`completed`, `stopped`, `done`, `failed`, `expired`) and returns that
answer; `download()` fetches an export's file.

```java
BatchSubmitResponse submitted = oblodai.batches().createPayout(PayoutBatchRequest.builder()
        .payouts(List.of(PayoutRequest.builder()
                .amount("5").currency("USDT").network("tron").address("TXYZ").orderId("b-1")
                .build()))
        .build());
BatchInfoResponse batch = oblodai.jobs().batch(submitted).waitFor();   // polls until terminal
System.out.println(batch.status() + ": " + batch.succeeded() + "/" + batch.total());

DocumentJobAccepted export = oblodai.documents().createJob(
        DocumentJobRequest.builder().kind(DocumentJobKind.STATEMENT).build());
Job<?> job = oblodai.jobs().document(export);
job.waitFor(Duration.ofMinutes(2), Duration.ofSeconds(3));
FileResult file = job.download();                                   // .writeTo(Path.of(...))
System.out.println(file.filename() + " " + Path.of(".").toAbsolutePath());
```

## Webhooks

Verify over the raw request bytes; deduplicate on `eventId()` (`X-Webhook-Event-Id`); never act on
a test delivery. `event().asPayment()`, `asPayout()`, `asWallet()`, `asConversion()` give the typed
event; an event kind this SDK does not know is still delivered, with its raw `type()` and
`fields()`.

```java
try {
    WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
            rawBody,                                    // the RAW bytes, not a re-serialized body
            WebhookHeaders.of(headers),
            WebhookVerifier.options(secret));
    if (delivery.isTest()) {
        return 200;                                     // a rehearsal: no money moved
    }
    if (delivery.event().type().equals("payment")) {
        System.out.println(delivery.event().asPayment().status());
    }
    return 200;
} catch (SignatureException e) {
    return 401;
}
```

## Asynchronous client

`oblodai.async()` (or `builder().buildAsync()`) has the same resources returning
`CompletableFuture`; cancelling a future cancels the HTTP exchange.

```java
String uuid = oblodai.async().payments()
        .create(PaymentRequest.builder().amount("25").currency("USDT").build())
        .thenApply(PaymentView::uuid)
        .join();
```

### Kotlin

```kotlin
val async = oblodai.async()
val invoice = async.payments().create(
    PaymentRequest.builder().amount("25").currency("USDT").orderId("order-1001").build()
).await()                                          // suspends; cancelling cancels the call
val all = async.payments().listHistory().asFlow().toList()
println("${invoice.uuid()}: ${all.size} invoices")
```

## Configuration

| option | environment | default |
| --- | --- | --- |
| `publicId(...)` / `secret(...)` | `OBLODAI_PUBLIC_ID` / `OBLODAI_SECRET` | none |
| `baseUrl(...)` | `OBLODAI_BASE_URL` | `https://api.oblodai.com` |
| `adminToken(...)` | `OBLODAI_ADMIN_TOKEN` | none |
| `allowInsecureBaseUrl(true)` | `OBLODAI_ALLOW_INSECURE=1` | https only, loopback excepted |
| `logger(...)` | `OBLODAI_LOG=debug\|info\|warn\|error` | silent |
| `timeout(Duration)` / `deadline(Duration)` | | 30 s per attempt / 90 s per call |
| `retry(RetryOptions)` / `maxRetries(int)` | | 2 retries |
| `header(name, value)`, `onRequest(...)`, `onResponse(...)`, `httpClient(...)` | | |

## Development

```bash
OBLODAI_BACKEND=../oblodai-backend make ci   # drift, build + lint + tests + conformance, package
```

`make ci` runs Maven in docker (`maven:3-eclipse-temurin-21`, cache in `.m2cache/`). The drift
check regenerates `src/main/java/com/oblodai/generated` with the backend's `tools/sdkgen` and
compares; the conformance suite (`tools/sdkgen/conformance`) runs every shared scenario on both
clients. Never edit generated code: change the contract or the generator, then run `make sdk` in
the backend.

## License

MIT - see [LICENSE](LICENSE).
