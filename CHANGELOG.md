# Changelog

All notable changes to the Oblodai Java SDK. The version tracks the SDK family: 1.3 is the line that
signs requests with the gateway's five-field recipe and is generated from its contract snapshot.

## 1.3.0 — 2026-08-25

First release of the Java SDK, generated from and verified against contract snapshot
`e09b495accae` (gateway commit `7b8eb828b9ec`).

### The API

- `Oblodai` — blocking client, built with `Oblodai.builder()`; every option falls back to the
  environment (`OBLODAI_PUBLIC_ID`, `OBLODAI_SECRET`, `OBLODAI_PAYOUT_PUBLIC_ID`,
  `OBLODAI_PAYOUT_SECRET`, `OBLODAI_BASE_URL`, `OBLODAI_ADMIN_TOKEN`, `OBLODAI_ALLOW_INSECURE`,
  `OBLODAI_LOG`).
- `OblodaiAsync` — the same surface returning `CompletableFuture`, over the same engine, connections,
  retry policy and learned clock skew. `Oblodai#async()` or `builder().buildAsync()`.
- All 107 merchant routes across 16 namespaces: `payments`, `refunds`, `payouts`, `payoutLinks`,
  `paymentLinks`, `batches`, `transfers`, `wallets`, `webhooks`, `documents`, `splits`, `settings`,
  `account`, `catalog`, `sandbox`, `merchants`.
- `Pager<T>` for paginated routes: `firstPage()`, iteration, `stream()`, `all(max)` — nothing is
  requested until it is consumed. `AsyncPager<T>` is its non-blocking twin.
- Models are records with the gateway's own field names (`@JsonProperty`) and English documentation;
  amounts and timestamps stay strings.
- `com.oblodai.kotlin`: `await()`, `asFlow()`, `asSequence()` and request builders (`payment { … }`).
  Both Kotlin dependencies are optional, so a Java project pulls neither.

### Correctness

- Requests are signed `ts \n METHOD \n path+query \n Idempotency-Key \n body` with HMAC-SHA256, over
  the exact bytes sent; the idempotency slot is empty rather than absent when no key is used. Every
  signing vector the gateway exports is replayed in the unit tests.
- Idempotency keys are generated once per logical call on routes the gateway deduplicates and reused
  on every retry; a caller key is refused on routes that do not deduplicate
  (`sdk.idempotency_unsupported`), and never forwarded to list pages.
- Retries follow the gateway's own `retryable` flag. Transport failures and answers with no gateway
  envelope are retried only when repeating is safe (a read route, or a keyed write). `Retry-After`
  wins over the exponential backoff; a per-attempt timeout and an overall per-call deadline both
  apply.
- On a 401 that means a bad signature or timestamp, the client learns the gateway's time from the
  `Date` header, re-signs once, and keeps the offset only if that attempt got past authentication.
- Path parameters are percent-encoded and refused when they could rewrite the URL; caller headers
  that collide with signed headers are dropped; a plain-http base URL is refused unless it is
  loopback or explicitly allowed.
- `OblodaiException` carries `code`, `httpStatus`, `retryable`, `retryAfter`, `requestId`, `field`
  and `synthetic`, with a subclass per status. Neither `toString()` nor `details()` includes the raw
  response body.
- `WebhookVerifier` verifies over raw bytes with a constant-time compare, accepts the previous secret
  during a rotation, rejects deliveries outside ±300 s, and parses into a sealed event union. It
  needs no client and no API key.

### Testing

- Unit: signing and webhook vectors from the contract snapshot, plus the retry, idempotency, skew,
  URL and header rules against a fake `HttpClient`.
- Contract: every one of the 107 routes is called through its SDK method and checked for method,
  path, key kind and idempotency header; every recorded golden body is decoded by its model and its
  key set compared field by field.
- Live: an onboarding-to-payout journey and a sweep of every namespace against a running gateway
  (`OBLODAI_LIVE_URL`), including a real signed webhook delivered to a receiver the test starts.
