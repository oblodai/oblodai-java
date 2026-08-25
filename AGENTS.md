# Oblodai Java SDK — guide for coding agents

`com.oblodai:oblodai-sdk:1.3.0`, Java 17+. Everything below is verified against the gateway's
contract snapshot in `contract/contract.json`.

## Non-negotiables

- Amounts are decimal **strings**: `.amount("25")`, never `25` or a `double`. Use `Money.add`,
  `Money.compare`, `Money.subtract`; `Money.toBigDecimal` only when you deliberately want one.
- Every method's optional last argument is a `RequestOptions`
  (`idempotencyKey`, `timeout`, `deadline`, `preferPayoutKey`).
- Two key kinds. The **payout key** is required for `payouts()`, `refunds()`, `payoutLinks()`,
  `transfers()`, `splits()`, `wallets().refundBlockedDeposit`, `settings()` auto-withdraw and
  API allow-list, `webhooks().rotateSecret()`, `webhooks().testPayout(...)`, `sandbox().faucet(...)`
  and `sandbox().reset()`. Configure it with `.payoutKey(publicId, secret)` (or `OBLODAI_PAYOUT_*`);
  the wrong kind is a 403 `merchant.wrong_key_kind`.
- List methods return `Pager<T>`: `firstPage()` for one page, iteration or `stream()` for every item,
  `all(max)` to collect. Nothing is requested until consumed.
- Idempotency keys are generated automatically on create routes and reused across retries. Passing
  `idempotencyKey` to a route the gateway does not deduplicate throws `sdk.idempotency_unsupported`.

## Naming

| intent            | call                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------- |
| fetch one         | `.info(uuid)` or `.info(new XInfoRequest().orderId(…))`; `.get(...)` is an alias             |
| fetch many        | `.history(...)` on payments and payouts (alias `.list`), `.list(...)` elsewhere              |
| create            | `.create(...)`; webhooks: `.register(url)`                                                   |
| many, synchronous | `payouts().mass(...)`, `payoutLinks().batch(...)` — ≤100, `List<BatchElement<T>>` per element |
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
`TransportException` (no response), `ConfigException` (before sending), `ContractException`
(unreadable envelope), `SignatureException` (webhooks).

Codes worth handling: `payout.insufficient_funds` (retryable), `payout.funds_maturing` (retryable),
`idempotency.key_reused`, `invoice.not_payable`, `payment.not_found`, `merchant.wrong_key_kind`,
`merchant.bad_signature`, `request.rate_limited`. Full list: `ErrorCodes.ALL`.

## Statuses

- Payment: `select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled`.
  `Statuses.isPaymentPaid` is paid/paid_over. `wrong_amount` needs `refunds().resolve(...)`.
- Payout: `pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled`.
- Webhook event types: `invoice.<status>`, `payout.<status>`, `wallet.paid`; the body's `type` is
  `payment` | `payout` | `wallet` (the sealed `WebhookEvent`).
- An unknown vocabulary value decodes to that enum's `UNKNOWN` instead of failing.

## Webhooks

```java
WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody, WebhookHeaders.of(headers), WebhookVerifier.options(secret));
```

Verify over the **raw** bytes. Deduplicate on `delivery.id()` (`X-Webhook-Id`); drop out-of-order
events with `WebhookVerifier.isStale(event, lastSequence)`. During a rotation pass
`.previousSecret(old)` for at least 26 h.

## Machine-readable surface

`Routes.ALL` (107 routes: path, auth, idempotent, safe, bare, list), the generated request types in
`com.oblodai.contract.requests`, `ErrorCodes.ALL`, the vocabulary enums (`Network`, `PaymentStatus`,
`PayoutStatus`, `EventType`, …), and `contract/` itself (schemas, golden response bodies per route,
error samples, signed webhook samples).
