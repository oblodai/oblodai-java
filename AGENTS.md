# Oblodai Java SDK — guide for coding agents

`com.oblodai:oblodai-sdk:2.0.0`, Java 17+. Resources, models and the route table under
`com.oblodai.generated` are generated from the gateway's OpenAPI contract by the backend's
`tools/sdkgen`; everything else is the hand-written runtime.

## Non-negotiables

- Never edit `src/main/java/com/oblodai/generated`: change the contract or the generator, then run
  `make sdk` in the backend. `make ci` fails on drift and on a lost or renamed name in `names.lock`.
- Amounts are `BigDecimal` (the builders also take a decimal string). There is no `double` overload;
  a `double` in a body is `sdk.float_amount` before anything is sent. `Money` is exact.
- Every method's last argument is an optional `RequestOptions`: `idempotencyKey`, `timeout`
  (`Duration`, per attempt), `maxRetries`, `extraHeaders`, `requestId` (`X-Request-ID`).
- One API key signs every signed route (`publicId`/`secret`, or `OBLODAI_PUBLIC_ID` /
  `OBLODAI_SECRET`); `adminToken` is sent on the onboarding route only. A route's `auth` is
  `public`, `key` or `onboard`.
- Retries and keys come from the route table (`idempotent`, `safe`), never from the path. The SDK
  generates the idempotency key where the gateway deduplicates and reuses it on every attempt; a key
  on any other route is `sdk.idempotency_unsupported`, list methods included.
- The client is immutable and `AutoCloseable`; cancelling a future of the async client aborts the
  exchange.

## Naming

`client.<resource>().<method>(...)`: the resource is the contract tag, the method its `operationId`
without the resource name, camelCase (`payments().create`, `payments().getInfo`,
`payouts().listHistory`, `documents().createJob`). The full list is `names.lock` (snake_case). Path
parameters come first; a body is a model built with `Model.builder()...build()`; query parameters
are a `*Query` model. Results are generated models (`extra()` keeps unknown fields, enums are open:
`of(value)`, `value()`, `isKnown()`).

- Lists: `Pager<T>` — iterate, `byPage()`, `firstPage()`, `all(max)`, `stream()`.
- Files: `FileResult { bytes, contentType, filename }`, `writeTo(path)`.
- Long-running: `jobs().batch(submitted)` / `jobs().document(accepted)` → `Job.waitFor()`,
  `download()`; the table is `Lro`.
- `withOptions(RequestOptions)`, `withRawResponse(c -> ...)` → `ApiResponse`, hooks
  `builder().onRequest(...)` / `onResponse(...)`.

## Errors

`catch (OblodaiException e)` → `code()` (`family.reason`), `httpStatus()`, `retryable()`,
`retryAfter()`, `requestId()`, `field()`, `synthetic()`; `getMessage()` is
`[code] text (request_id=…)`, `text()` the text alone. Subclasses: `ValidationException` 400,
`AuthenticationException` 401, `PermissionException` 403, `NotFoundException` 404,
`ConflictException`/`IdempotencyConflictException` 409, `RateLimitException` 429,
`UnavailableException` 503, `InternalException`, `TransportException` (no response),
`ConfigException` (before sending: `sdk.bad_config`, `sdk.missing_credentials`,
`sdk.bad_idempotency_key`, `sdk.idempotency_unsupported`, `sdk.bad_path_param`, `sdk.bad_header`,
`sdk.bad_amount`, `sdk.float_amount`), `ContractException` (`sdk.bad_envelope`,
`sdk.response_too_large`), `WebhookPayloadException` (`webhook.bad_payload`: authentic but not an
event — answer 400), `SignatureException` (webhooks), `JobTimeoutException` (`sdk.job_timeout`).

## Webhooks

```java
WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody, WebhookHeaders.of(headers), WebhookVerifier.options(secret));
```

Verify over the raw bytes; deduplicate on `delivery.eventId()` (`X-Webhook-Event-Id`); never act on
`delivery.isTest()`; drop out-of-order events with `WebhookVerifier.isStale(event, lastSequence)`.
`event.asPayment()` / `asPayout()` / `asWallet()` / `asConversion()` give the typed event; an unknown
`type()` is still delivered with its `fields()`.

## Building

```bash
OBLODAI_BACKEND=../oblodai-backend make ci   # drift → mvn verify (lint, tests, conformance, javadoc) → package check
make test                                    # unit tests only
make conformance                             # the backend's shared scenarios only
```
