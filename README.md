# Oblodai Java SDK

Official Java (and Kotlin) client for the [Oblodai](https://oblodai.com) crypto payment gateway:
invoices, payouts, refunds, payout links, static wallets, webhooks, documents — the whole merchant
API, typed end to end and verified against the gateway's own contract snapshot.

- Java 17+, one runtime dependency (Jackson), `java.net.http.HttpClient` underneath.
- Every route the gateway exposes has a method here; request types and the route registry are
  generated from the gateway, and a drift gate fails the build when they disagree.
- Retries driven by the API's own `retryable` flag, automatic idempotency keys, clock-skew correction.
- Blocking and `CompletableFuture` clients over one engine; Kotlin gets coroutines and a request DSL.
- `com.oblodai.webhooks.WebhookVerifier`: signature verification that needs no client and no API key.

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>1.3.0</version>
</dependency>
```

## Start in the sandbox

Get your keys in the Oblodai dashboard. A **sandbox key** (`test_…`) drives a chainless copy of the
gateway — fake balance from a faucet, simulated deposits, real webhooks — so integrate against it
first.

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
charge, `to_currency` the asset the payer sends. Runnable programs live in [`examples/`](examples).

### Two keys

The gateway issues a **payment key** (`pk_…`) and a **payout key** (`wk_…`). Sandbox keys are both at
once; live keys are separate, and money-out routes need the payout one: `payouts()`, `refunds()`,
`payoutLinks()`, `transfers()`, `splits()`, `wallets().refundBlockedDeposit(...)`, auto-withdraw, the
IP allow-list, `webhooks().rotateSecret()`, `sandbox().faucet(...)`/`reset()`. Give the client both
pairs and it picks the right one per call:

```java
Oblodai.builder().publicId(id).secret(secret).payoutKey(payoutId, payoutSecret).build();
// or OBLODAI_PUBLIC_ID / OBLODAI_SECRET / OBLODAI_PAYOUT_PUBLIC_ID / OBLODAI_PAYOUT_SECRET
```

A call with the wrong kind is a 403 `merchant.wrong_key_kind`.

## Resources

| Namespace                  | Methods                                                                                                                                                                       |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `payments()`               | create · info/get · cancel · history/list · batch · qr · services · sendEmail · resend · publicView · select · publicQr                                                        |
| `refunds()`                | create · resolve · batch                                                                                                                                                      |
| `payouts()`                | create · validate · calculate · info/get · cancel · approve · history/list · mass · batch · services · get/setFeeConfig · get/setRefundFeeConfig                               |
| `payoutLinks()`            | create · info/get · list · cancel · batch · cheque · claimPreview · claim                                                                                                     |
| `paymentLinks()`           | create · info/get · list · toggle · publicView · checkout                                                                                                                     |
| `batches()` / `transfers()`| info · toPersonal · toUser · batch                                                                                                                                            |
| `wallets()`                | create · qr · block · refundBlockedDeposit                                                                                                                                    |
| `webhooks()`               | register · rotateSecret · deliveries · testPayment/testPayout/testWallet · test(kind, …)                                                                                       |
| `documents()`              | statement · ledger · balanceCertificate · feeSchedule · splitReport · batchReport · linkReport · walletStatement · referralsReport · createJob · jobInfo · jobFile · download   |
| `splits()`                 | createRule · listRules · deleteRule · get/setConfig · get/setOptIn                                                                                                             |
| `settings()`               | setDiscount · listDiscounts · get/setAccuracy · get/setAutoRefund · listAccepted · setAccepted · get/setPaymentFeeConfig · list/set/deleteAutoWithdraw · list/add/remove/enableApiAllowlist |
| `account()` / `catalog()`  | balance · referral · vrcs · currencies · exchangeRates                                                                                                                        |
| `sandbox()`                | faucet · deposit · webhooks · replay · reset                                                                                                                                  |
| `merchants()`              | create · createSandbox (provisioning; `adminToken(...)` on a self-hosted gateway)                                                                                             |

Every method takes an optional last argument, `RequestOptions` — every method, including the
aliases (`get`, `list`) and the no-argument list forms:

```java
oblodai.payouts().create(request, RequestOptions.of()
        .idempotencyKey(orderId)                  // your own key; generated for you when omitted
        .timeout(Duration.ofSeconds(10))          // per attempt
        .deadline(Duration.ofSeconds(45))         // whole call, retries and pauses included
        .header("X-Tenant", "acme")               // one call only, on top of the client-wide headers
        .preferPayoutKey(true));                  // sign with the payout key on an either-kind route
```

Lookups accept a bare id or a request object: `payments().info(uuid)`,
`payments().info(new PaymentInfoRequest().orderId("order-1001"))`.

### Lists

List methods return a `Pager<T>`. Nothing is requested until you consume it, and the walk follows
the gateway's own `paginate.has_pages` flag.

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
`status.wire()` is what the gateway said, `status.isKnown()` says it is new. An event kind newer than
this SDK arrives the same way, as an `UnknownEvent` carrying the raw `type`
(`WebhookVerifier.isKnownEvent(event)`). A gateway that grows its vocabulary can neither break a
deployed client nor have what it said thrown away.

### Errors

Every failure is an `OblodaiException` carrying the API's error envelope: `code()`
(`payout.insufficient_funds`), `httpStatus()`, `retryable()`, `retryAfter()`, `requestId()`,
`field()`, `synthetic()`. Subclasses for `catch`: `ValidationException` (400),
`AuthenticationException` (401), `PermissionException` (403), `NotFoundException` (404),
`ConflictException` / `IdempotencyConflictException` (409), `RateLimitException` (429),
`UnavailableException` (503), `InternalException` (other 5xx), `TransportException` (no response),
`ConfigException` (rejected before sending — `sdk.bad_config`, `sdk.missing_credentials`,
`sdk.bad_idempotency_key`, `sdk.idempotency_unsupported`, `sdk.bad_path_param`, `sdk.bad_header`,
`sdk.bad_amount`), `ContractException` (unreadable envelope — `sdk.bad_envelope`,
`sdk.response_too_large`), `WebhookPayloadException` (`webhook.bad_payload`),
`SignatureException` (webhooks). Quote `requestId()` to support.

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

`toString()` and `details()` never include the raw response body, so a log cannot spill an invoice
payload or a cheque passcode; `raw()` is there for deliberate inspection. The same goes for the
models that carry a shown-once value — `WebhookEndpoint.secret`, `WebhookSecretRotated.secret`,
`ApiKeyPair.secret`, `PayoutLink.claimToken`/`claimUrl`/`passcode`: readable through their accessor,
`[redacted]` in `toString()` and in JSON. A logger you inject sees fields the transport has already
redacted, whatever the logger does with them.

### Retries and idempotency

- Create-type routes get an `Idempotency-Key` automatically — one per logical call, reused on every
  retry — so a timeout can never produce a second payout. Pass your own key to make retries safe
  across process restarts. On routes the gateway does not deduplicate the SDK **refuses** a key
  (`sdk.idempotency_unsupported`) — list methods included, where it is refused as the pager is built
  rather than quietly dropped: the header would be ignored, and only the SDK would believe a re-send
  was safe.
- An error is retried only when the API says `retryable`. Answers without an API envelope (a proxy
  502/503) and transport failures are retried only on read routes or keyed writes. "Read route" is
  not a guess about the path: it is the `safe` flag the gateway states for each route in the contract
  snapshot. `Retry-After` wins over the computed backoff (capped by `maxRetryAfterMs`).
- `Oblodai.builder().retry(new RetryOptions(maxRetries, baseDelayMs, maxDelayMs, maxRetryAfterMs))`;
  `.timeout(...)` per attempt, `.deadline(...)` per call.
- On a 401 that means a bad signature or timestamp, the SDK reads the server's `Date`, re-signs once,
  and keeps the offset only if that attempt got past authentication. The offset is shared by every
  call the client makes and corrected atomically, so concurrent calls on a skewed host converge on
  one correction instead of undoing each other's.
- A response body is read under a ceiling — 8 MiB for a JSON envelope, 64 MiB for a document — and a
  larger answer fails the call with `sdk.response_too_large` instead of the process.

### Webhooks

```java
byte[] rawBody = request.getInputStream().readAllBytes();      // the RAW bytes, always

WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody,
        WebhookHeaders.of(headers),        // also ofMulti(Map<String, List<String>>), or a lambda
        WebhookVerifier.options(secret));                       // .previousSecret(old) while rotating

if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
    markOrderPaid(payment.orderId());
}
```

Rehearsal deliveries (`webhooks().test(...)`, sandbox) are signed exactly like live ones and carry
`test: true` in the body (and `X-Webhook-Test: true`): check `delivery.isTest()` — or
`WebhookVerifier.isTestEvent(event)` on a body you already have — and never act on one as if money
moved. `delivery.id()` (`X-Webhook-Id`) is stable across retries — deduplicate on it;
`WebhookVerifier.isStale(event, lastSequence)` drops out-of-order deliveries. After
`webhooks().rotateSecret()` keep the previous secret for at least 26 hours. Verification is a
standalone class: no client, no key, no network.

### Async

```java
OblodaiAsync async = oblodai.async();                  // same engine, connections and clock
CompletableFuture<Payment> invoice = async.payments().create(request);
CompletableFuture<List<Payout>> all = async.payouts().history().all(500);
```

Every future fails with an `OblodaiException` as its cause.

### Kotlin

The Java SDK is the Kotlin SDK: `com.oblodai.kotlin` adds coroutines and builders on the same types
(both Kotlin dependencies are `optional`, so a Java project pulls neither).

```kotlin
val oblodai = Oblodai.builder().publicId(id).secret(secret).build().async()

val invoice = oblodai.payments().create(payment {
    amount("25"); currency("USDT"); network(Network.TRON); orderId("order-1001")
}).await()

oblodai.payments().history().asFlow().take(100).collect { println(it.uuid()) }
```

`await()` throws the SDK's own exception rather than a `CompletionException` wrapper, and cancelling
the coroutine cancels the call — `cancel(true)` on any future the SDK returned aborts the HTTP
exchange in flight and stops the retry loop. `asFlow()` is pull-based: it asks for the next page only
once the collector has taken the last item of the previous one, so nothing is dropped and nothing is
fetched that the collector never reads.

### Money

Amounts are decimal strings end to end (USDT has 6 decimals, BTC 8, ETH 18). `Money.add`,
`Money.subtract`, `Money.compare`, `Money.isZero`, and `Money.toBigDecimal` when you deliberately
want one. Never parse an amount into a `double`, and never compare two amount strings with
`String.compareTo` — `"10"` sorts before `"9"`. Anything that is not a plain decimal (`"1e3"`, an
empty string, 64+ characters) is a `ConfigException` with code `sdk.bad_amount`. A JSON number
arriving where the contract says string is refused too, rather than silently stringified.

### Self-hosted or local gateway

`baseUrl("http://127.0.0.1:8095")` works out of the box; other plain-http hosts need
`allowInsecureBaseUrl(true)` (or `OBLODAI_ALLOW_INSECURE=1`). A path prefix in `baseUrl` is kept
(`https://gw.corp/oblodai` → `https://gw.corp/oblodai/v1/payment`). Merchant provisioning routes are
unsigned and carry `X-Admin-Token` when `adminToken(...)` is set — and only those routes: the header
is not something a caller can add to every request through `header(...)`, which refuses the names the
SDK owns (`Accept`, `Content-Type`, `User-Agent`, `X-Public-Id`, `X-Signature`, `X-Timestamp`,
`Idempotency-Key`, `X-Admin-Token`) with `sdk.bad_header`. Redirects are never followed; if an
injected `HttpClient` follows one, the SDK notices the answer came from another URL and fails the
call.

The client is `AutoCloseable`: `close()` releases the HTTP client it built for itself (a no-op on
JDK 17, where `HttpClient` has no close operation). One client per key pair, kept for the life of the
process, is the intended shape.

## The contract snapshot

`contract/` is exported by the gateway's own test suite: the route registry (107 merchant routes,
each with its method, path, key kind, `idempotent`, `safe`, `bare` and list shape), request DTO
schemas with English field docs, enums, all 471 error codes, signing vectors, golden response bodies
recorded from a live gateway and real signed webhook deliveries. `src/main/java/com/oblodai/contract/` is generated
from it by `codegen/run.sh`; `codegen/run.sh --check` (which `mvn verify` runs first) fails the build
when they disagree; the contract tests check every model against the golden bodies and every route
against the registry.

## Development

```bash
mvn -o verify                                    # drift gate + compile + 514 offline tests + jars
codegen/run.sh                                   # after refreshing contract/
OBLODAI_LIVE_URL=http://127.0.0.1:8095 mvn -q verify   # also runs the 18 live tests
mvn -Prelease deploy                             # publish to Maven Central; see RELEASING.md
```

The live tier onboards a merchant on the gateway under test, takes a sandbox key and walks the whole
money path, including a real signed webhook delivered to a receiver the test starts. Without
`OBLODAI_LIVE_URL` those tests are skipped.

License: MIT.
