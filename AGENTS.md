# Oblodai Java SDK — guide for coding agents

`com.oblodai:oblodai-sdk:1.3.0`, Java 17+. Everything below is verified against the gateway's
contract snapshot in `contract/contract.json`.

## Non-negotiables

- Amounts are decimal **strings**: `.amount("25")`, never `25` or a `double`. Use `Money.add`,
  `Money.compare`, `Money.subtract`; `Money.toBigDecimal` only when you deliberately want one.
- Every method's optional last argument is a `RequestOptions`
  (`idempotencyKey`, `timeout`, `deadline`, `header`, `preferPayoutKey`). Every method: the aliases
  and the no-argument list forms have the overload too.
- Two key kinds. The **payout key** is required for `payouts()`, `refunds()`, `payoutLinks()`,
  `transfers()`, `splits()`, `wallets().refundBlockedDeposit`, `settings()` auto-withdraw and
  API allow-list, `webhooks().rotateSecret()`, `webhooks().testPayout(...)`, `sandbox().faucet(...)`
  and `sandbox().reset()`. Configure it with `.payoutKey(publicId, secret)` (or `OBLODAI_PAYOUT_*`);
  the wrong kind is a 403 `merchant.wrong_key_kind`.
- List methods return `Pager<T>`: `firstPage()` for one page, iteration or `stream()` for every item,
  `all(max)` to collect. Nothing is requested until consumed.
- Idempotency keys are generated automatically on create routes and reused across retries. Passing
  `idempotencyKey` to a route the gateway does not deduplicate throws `sdk.idempotency_unsupported` —
  list methods included, where it is refused as the pager is built.
- Whether a failed request may be re-sent comes from the contract's `safe` flag per route, never from
  the shape of the path. `codegen/run.sh` fails if the contract stops declaring it.
- The client is `AutoCloseable`; cancelling a future the async client returned aborts the exchange.

## Naming

| intent            | call                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------- |
| fetch one         | `.info(uuid)` or `.info(new XInfoRequest().orderId(…))`; `.get(...)` is an alias             |
| fetch many        | `.history(...)` on payments and payouts (alias `.list`), `.list(...)` elsewhere              |
| create            | `.create(...)`; webhooks: `.register(url)`                                                   |
| many, synchronous | `payouts().mass(...)` — ≤100, `payoutLinks().batch(...)` — ≤500, `List<BatchElement<T>>` per element |
| many, asynchronous| `payments().batch`, `payouts().batch`, `refunds().batch`, `transfers().batch` — ≤5000, poll `batches().info(...)` |
| documents         | `documents().*` → `FileResult { bytes, contentType, filename }`                              |
| provisioning      | `merchants().create(...)`, `merchants().createSandbox(id)` — unsigned; `adminToken(...)` on a self-hosted gateway |
| payer-facing      | `payments().publicView/select/publicQr`, `paymentLinks().publicView/checkout`, `payoutLinks().claimPreview/claim` — no credentials |

## Errors

`catch (OblodaiException e)` → `code()` (`family.reason`), `httpStatus()`, `retryable()`
(authoritative — the SDK already retried what it should), `retryAfter()`, `requestId()` (quote to
support), `field()` (400s), `synthetic()` (a proxy answered, not the gateway). Subclasses:
`ValidationException` 400, `AuthenticationException` 401, `PermissionException` 403,
`NotFoundException` 404, `ConflictException`/`IdempotencyConflictException` 409,
`RateLimitException` 429, `UnavailableException` 503, `InternalException` other 5xx,
`TransportException` (no response), `ConfigException` (before sending: `sdk.bad_config`,
`sdk.missing_credentials`, `sdk.bad_idempotency_key`, `sdk.idempotency_unsupported`,
`sdk.bad_path_param`, `sdk.bad_header`, `sdk.bad_amount`), `ContractException` (unreadable envelope:
`sdk.bad_envelope`, `sdk.response_too_large`), `WebhookPayloadException` (`webhook.bad_payload` — an
authentic delivery whose body is not an event; answer 400, never 401), `SignatureException`
(webhooks).

Codes worth handling: `payout.insufficient_funds` (retryable), `payout.funds_maturing` (retryable),
`idempotency.key_reused`, `invoice.not_payable`, `payment.not_found`, `merchant.wrong_key_kind`,
`merchant.bad_signature`, `request.rate_limited`. Full list: `ErrorCodes.ALL`.

## Statuses

- Payment: `select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled`.
  `Statuses.isPaymentPaid` is paid/paid_over. `wrong_amount` needs `refunds().resolve(...)`.
- Payout: `pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled`.
- Webhook event types: `invoice.<status>`, `payout.<status>`, `wallet.paid`; the body's `type` is
  `payment` | `payout` | `wallet` (the sealed `WebhookEvent`). A `type` this snapshot does not know
  decodes to `UnknownEvent`, which keeps the raw string — `WebhookVerifier.isKnownEvent(event)`.
- The vocabularies are open value types (`Vocabulary`): known values are interned constants
  (`status == PaymentStatus.PAID` works, `PaymentStatus.VALUES` lists them), and a value this
  snapshot does not know decodes to an instance carrying the raw string — `wire()` and `isKnown()`.
  They are not Java enums, so there is no `switch` over them and no `UNKNOWN` constant.

## Webhooks

```java
WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody, WebhookHeaders.of(headers), WebhookVerifier.options(secret));
```

Verify over the **raw** bytes. The checks run headers → MAC → freshness → body, so the freshness
window is never an oracle for an unauthenticated caller; a secret that is null or blank, and a
negative tolerance, are `ConfigException` before any crypto (`Duration.ZERO` disables the freshness
check deliberately, sub-second windows are honoured). `delivery.isTest()` (and `WebhookVerifier.isTestEvent(event)`) is true
for rehearsal deliveries — `test: true` in the signed body, `X-Webhook-Test: true` on the request —
never treat one as money. Deduplicate on `delivery.id()` (`X-Webhook-Id`); drop out-of-order events
with `WebhookVerifier.isStale(event, lastSequence)`. During a rotation pass `.previousSecret(old)`
for at least 26 h.

## Machine-readable surface

`Routes.ALL` (107 routes: path, auth, idempotent, safe, bare, list — each field equal to
`contract/contract.json`, checked route by route in `RoutesTest`), the generated request types in
`com.oblodai.contract.requests`, `ErrorCodes.ALL` (471 codes), the vocabulary enums (`Network`, `PaymentStatus`,
`PayoutStatus`, `EventType`, …), and `contract/` itself (schemas, golden response bodies per route,
error samples, signed webhook samples).

## Building

```bash
mvn -o verify          # drift gate → compile (Kotlin then Java) → 518 offline tests → jar, sources, javadoc
mvn -o test            # tests only
codegen/run.sh         # regenerate src/main/java/com/oblodai/contract from contract/contract.json
codegen/run.sh --check  # what the drift gate runs
OBLODAI_LIVE_URL=http://127.0.0.1:8095 mvn -o verify   # adds the 18 live tests
mvn -Prelease deploy   # Maven Central; inert without -Prelease, see RELEASING.md
```

Source files stay ≤ 400 lines; a namespace that outgrows it is split into a package-private base
class in the same package (`PaymentsCheckoutRoutes`, `SettingsPayoutKeyRoutes`, …), never into a
second public type. The blocking and async trees are method-for-method identical — `AsyncParityTest`
enforces it, and `RouteWiringTest` drives every route through both clients.
