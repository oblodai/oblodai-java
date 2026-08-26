# Changelog

All notable changes to the Oblodai Java SDK are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). The version tracks the SDK family: 1.3 is
the line that signs requests with the gateway's five-field recipe and is generated from its contract
snapshot.

## [1.3.0] — 2026-08-26

First release of the Java SDK, generated from and verified against contract snapshot
`de2c4a5d15d1` (gateway commit `2cc44c16f516`, exported 2026-08-26). Upgrading from the 1.2 line:
see [MIGRATION-1.3.md](MIGRATION-1.3.md).

### The API

- **One API key.** A merchant has a single key pair and it signs every signed route, money in and
  money out alike; the payout credential pair and the payout-key option are gone. There is no
  `payoutKey(publicId, secret)`, no `RequestOptions.preferPayoutKey(...)`, no
  `OBLODAI_PAYOUT_PUBLIC_ID` / `OBLODAI_PAYOUT_SECRET`, and no key-kind fallback on
  `batches().info(...)`. The route registry's `auth` is `public`, `key` or `onboard`, and onboarding
  answers with `api_key` alone. `merchant.wrong_key_kind` has left the catalogue with the split keys:
  only a merchant still holding a legacy `oblodai_pk_…` / `oblodai_wk_…` pair can meet it.
- `Oblodai` — blocking client, built with `Oblodai.builder()`; every option falls back to the
  environment (`OBLODAI_PUBLIC_ID`, `OBLODAI_SECRET`, `OBLODAI_BASE_URL`, `OBLODAI_ADMIN_TOKEN`,
  `OBLODAI_ALLOW_INSECURE`, `OBLODAI_LOG`).
- `OblodaiAsync` — the same surface returning `CompletableFuture`, over the same engine, connections,
  retry policy and learned clock skew. `Oblodai#async()` or `builder().buildAsync()`.
- All 107 merchant routes across 16 namespaces: `payments`, `refunds`, `payouts`, `payoutLinks`,
  `paymentLinks`, `batches`, `transfers`, `wallets`, `webhooks`, `documents`, `splits`, `settings`,
  `account`, `catalog`, `sandbox`, `merchants`.
- `Pager<T>` for paginated routes: `firstPage()`, iteration, `stream()`, `all(max)` — nothing is
  requested until it is consumed. `AsyncPager<T>` is its non-blocking twin.
- Models are records with the gateway's own field names (`@JsonProperty`) and English documentation;
  amounts and timestamps stay strings.
- The vocabularies (`PaymentStatus`, `Network`, …) are open value types implementing `Vocabulary`:
  known values are interned constants comparable with `==`, and a value the gateway grows after this
  snapshot keeps the string it sent (`wire()`, `isKnown()`) instead of collapsing into a sentinel.
  Webhook events do the same through `UnknownEvent` and `WebhookVerifier.isKnownEvent(...)`.
- `com.oblodai.kotlin`: `await()`, `asFlow()`, `asSequence()` and request builders (`payment { … }`).
  Both Kotlin dependencies are optional, so a Java project pulls neither. `asFlow()` is pull-based:
  it fetches the next page only once the collector has consumed the previous one.
- `merchants()` — merchant provisioning on a self-hosted gateway, unsigned, with `X-Admin-Token`
  attached to those routes and no others (`adminToken(...)` or `OBLODAI_ADMIN_TOKEN`).
- Blocked static wallets: `wallets().block(...)`, the `blocked` field on `Wallet`, and
  `wallets().refundBlockedDeposit(...)` to send a deposit that landed on one back.
- Rehearsal webhooks carry `test: true` in the signed body as well as `X-Webhook-Test: true`;
  `delivery.isTest()` and `WebhookVerifier.isTestEvent(event)` read it.
- `RequestOptions`: `idempotencyKey`, `timeout`, `deadline`, `header` — on every method of both
  trees, aliases and no-argument list forms included.
- Both clients are `AutoCloseable`, and cancelling a future the async client returned aborts the HTTP
  exchange in flight instead of stopping at a stage.

### Correctness

- Requests are signed `ts \n METHOD \n path+query \n Idempotency-Key \n body` with HMAC-SHA256, over
  the exact bytes sent; the idempotency slot is empty rather than absent when no key is used. Every
  signing vector the gateway exports is replayed in the unit tests.
- Idempotency keys are generated once per logical call on routes the gateway deduplicates and reused
  on every retry; a caller key is refused on routes that do not deduplicate
  (`sdk.idempotency_unsupported`), and never forwarded to list pages.
- Retries follow the gateway's own `retryable` flag. Transport failures and answers with no gateway
  envelope are retried only when repeating is safe — and "safe" is the gateway's own per-route
  `safe` flag from the contract snapshot, never a guess from the shape of the path. `Retry-After`
  wins over the exponential backoff (bounded by `maxRetryAfterMs`); a per-attempt timeout and an
  overall per-call deadline both apply, the deadline covering the whole response read.
- The error envelope is decoded field by field: a `retryable` that is not a boolean falls back to the
  status, a `retry_after` that is a float or a numeric string is understood and clamped, and a body
  with no usable `code` is treated as no envelope at all rather than failing the decode.
- Response bodies are read under a ceiling — 8 MiB for a JSON envelope, 64 MiB for a document — and a
  larger answer fails with `sdk.response_too_large` instead of exhausting the heap.
- A list route that answers without `items`/`paginate` is a `ContractException`, not an empty page.
- Values shown once — `WebhookEndpoint.secret`, `WebhookSecretRotated.secret`, `ApiKeyPair.secret`,
  `PayoutLink.claimToken`/`claimUrl`/`passcode` — stay readable through their accessor and render as
  `[redacted]` in `toString()` and in JSON. An injected logger receives fields the transport has
  already redacted.
- `Money` refuses anything that is not a plain decimal with `sdk.bad_amount`, and the JSON mapper
  refuses a number where the contract says string rather than stringifying it.
- On a 401 that means a bad signature or timestamp, the client learns the gateway's time from the
  `Date` header, re-signs once, and keeps the offset only if that attempt got past authentication.
  The offset is shared and corrected atomically, so concurrent calls on a skewed host converge on one
  correction instead of rolling back each other's.
- Path parameters are percent-encoded and refused when they could rewrite the URL; a caller header
  whose name the SDK owns is refused outright (`sdk.bad_header`), as is one carrying a line break or
  a non-ASCII value; a plain-http base URL is refused unless it is loopback or explicitly allowed;
  redirects are never followed, and one an injected HTTP client followed is detected and refused.
- `OblodaiException` carries `code`, `httpStatus`, `retryable`, `retryAfter`, `requestId`, `field`
  and `synthetic`, with a subclass per status. Neither `toString()` nor `details()` includes the raw
  response body.
- `WebhookVerifier` verifies over raw bytes with a constant-time compare, accepts the previous secret
  during a rotation, rejects deliveries outside ±300 s, and parses into a sealed event union. It
  needs no client and no API key. The MAC is checked before the freshness window, so the window is
  never an oracle; an empty secret or a negative tolerance is refused before any crypto; an event
  kind this snapshot does not know arrives as `UnknownEvent` with its raw `type`; and a delivery that
  verified but cannot be read is `webhook.bad_payload` in the contract family, so a receiver that
  answers 401 to signature failures does not reject an authentic event.

### Packaging

- Maven Central publishing lives in the `release` profile: javadoc and sources jars, GPG signing and
  the Sonatype central-publishing plugin. An ordinary `mvn verify` neither resolves those plugins nor
  asks for a signing key. See [RELEASING.md](RELEASING.md).

### Testing

- Unit: signing and webhook vectors from the contract snapshot, plus the retry, idempotency, skew,
  URL and header rules against a fake `HttpClient`.
- Contract: every one of the 107 routes is called through **both** the blocking and the asynchronous
  client and checked for method, path, the one API key, the admin token and the idempotency header;
  the generated registry is compared to `contract.json` field by field (`method`, `path`, `auth`,
  `idempotent`,
  `safe`, `bare`, `list`), with a test that proves the comparison catches a flipped flag; every
  recorded golden body is decoded by its model and its key set compared field by field.
- Parity: the two trees are checked by reflection to expose the same methods with the same parameter
  types, and every method to have a `RequestOptions` overload.
- Live: an onboarding-to-payout journey and a sweep of every namespace against a running gateway
  (`OBLODAI_LIVE_URL`), including a real signed webhook delivered to a receiver the test starts.
