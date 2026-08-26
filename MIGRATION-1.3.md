# Migrating to 1.3

The 1.3 line is the first Java SDK generated from the gateway's own contract snapshot. If you are
coming from a 1.2 client (or from hand-written HTTP calls), this is what changed and what it means
for code you already have.

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>1.3.0</version>
</dependency>
```

## The signature is five fields

Requests are signed over

```
ts \n METHOD \n path+query \n Idempotency-Key \n body
```

with HMAC-SHA256, over the exact bytes sent; the idempotency slot is the empty string when no key is
sent, never absent. A 1.2 client signing four fields is rejected by the current gateway with 401
`merchant.bad_signature`. Nothing to do beyond upgrading — the SDK does the signing — but if you
built a request by hand anywhere, that is where the 401 comes from.

## Merchant provisioning and the admin token

`merchants().create(...)` and `merchants().createSandbox(id)` provision a merchant on a self-hosted
gateway. They are unsigned and carry `X-Admin-Token`, which you set once:

```java
Oblodai gateway = Oblodai.builder()
        .baseUrl("https://gw.corp")
        .adminToken(System.getenv("OBLODAI_ADMIN_TOKEN"))
        .build();

MerchantOnboarded merchant = gateway.merchants().create(
        new MerchantsRequest().email("shop@example.com").name("Example Shop"));
```

The token rides on those routes and on no others. Setting it through `header("X-Admin-Token", …)` is
refused (`sdk.bad_header`) — that would have sent your admin credential on every call.

## Rehearsal webhooks are flagged

`webhooks().test(...)`, `testPayment/testPayout/testWallet` and sandbox deliveries are signed exactly
like live ones and now carry `test: true` in the signed body as well as `X-Webhook-Test: true` on the
request. Check it, and never act on a rehearsal as if money moved:

```java
WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(rawBody, headers, options);
if (delivery.isTest()) return ok();          // or WebhookVerifier.isTestEvent(event)
```

## Blocked wallets

A static wallet can be blocked (`wallets().block(...)`, the `blocked` field on `Wallet`), and a
deposit that lands on a blocked address is sent back with `wallets().refundBlockedDeposit(...)`,
which answers with the new `WalletBlocked` model. The refund is a money-moving call, so branch on its
failures: `wallet.bad_uuid`, `refund.no_address`, `refund.nothing_to_refund`, `refund.dust`,
`refund.destination_internal`.

There is no wallet.blocked error code — an earlier draft of this document named one, and it never
existed. The SDK's own
test suite now checks every code named in its sources and in this documentation against the
contract's catalogue, so a code that does not exist cannot be documented again.

## Retry safety comes from the contract

Whether a request may be re-sent after a transport failure is the gateway's own per-route `safe`
flag, carried in `contract/contract.json` and exposed on `Routes.ALL`. The SDK no longer guesses from
the shape of the path. If you were reading `RouteSpec.safe()` before, the values are the same for
every route in this snapshot — but they are now stated rather than inferred, and the code generator
fails if the contract stops declaring them.

## `webhook.bad_payload`

A delivery whose signature verified but whose body cannot be read as an event now raises
`WebhookPayloadException` with code `webhook.bad_payload`, in the **contract** family — not
`SignatureException`. If your receiver answers 401 to signature failures, keep answering 401 to
`SignatureException` only; answer 400 to this one. It means the delivery is authentic and something
about the body is unexpected.

```java
try {
    event = WebhookVerifier.verify(rawBody, headers, options);
} catch (SignatureException bad) {
    return status(401);                       // not from Oblodai, or replayed
} catch (WebhookPayloadException unreadable) {
    log.error("authentic delivery I cannot read", unreadable);
    return status(400);                       // do not answer 401 here
}
```

An event `type` this SDK does not know no longer throws either: it arrives as an `UnknownEvent`
carrying the raw `type` string. `WebhookVerifier.isKnownEvent(event)` tells them apart, and
`isTestEvent`/`isStale` work on both.

## Secrets are redacted in rendering

`WebhookEndpoint.secret`, `WebhookSecretRotated.secret`, `ApiKeyPair.secret` and
`PayoutLink.claimToken`/`claimUrl`/`passcode` still return their value from their accessor, and now
render as `[redacted]` in `toString()` and in JSON. Code that stored the value through the accessor
is unaffected; code that logged the whole object, or serialised it into a structured log, will see
placeholders — which is the point.

```java
WebhookEndpoint endpoint = oblodai.webhooks().register("https://shop.example/hook");
store(endpoint.secret());             // still the real secret
log.info("registered {}", endpoint);  // WebhookEndpoint[endpointId=…, secret=[redacted]]
```

## The vocabularies are open value types, not enums

`PaymentStatus`, `PayoutStatus`, `PayoutLinkStatus`, `DeliveryStatus`, `Network`, `FeeBearer`,
`FeeBearerResult`, `BatchOnError`, `WebhookKind`, `ErrorKind`, `AmountMode` and `EventType` are now
final classes with interned constants instead of Java enums. What still works:

```java
if (payment.status() == PaymentStatus.PAID) { … }        // known values are interned
Statuses.isPaymentPaid(payment.status());
PaymentStatus.of("paid");                                // and from(...) as before
Network.TRON.wire();                                     // "tron"
```

What changed:

- There is no `UNKNOWN` constant any more. A value this snapshot does not know keeps the string the
  gateway sent: `status.wire()` is that string and `status.isKnown()` is `false`. Previously the
  value was thrown away, which left a caller unable to log or report what actually arrived.
- `switch (status)` over the constants no longer compiles (they are not enum constants); use
  `if`/`equals` or compare `wire()`.
- `values()` is `VALUES`, a `List`, and there is no `name()` or `ordinal()`.
- All of them implement `Vocabulary` (`wire()`, `isKnown()`), so generic code can handle any of them.

## Model corrections

- `ClaimResult.status` is the **link's** status (`claimed`), not a payout status.
- Document reports are keyed by the query parameter the gateway actually reads (`uuid`), so
  `documents().splitReport(id, query)` and its siblings send the right parameter.
- Every list route decodes into `{items, paginate}`; a body missing either is a `ContractException`
  rather than a silently empty page. If you relied on an empty page to mean "no results", check for
  the exception — it means the answer was unreadable, which is not the same thing.

## Smaller changes worth knowing

- Every method takes `RequestOptions` as its optional last argument — the aliases (`get`, `list`) and
  the no-argument list forms included. It now also carries per-call `header(name, value)`.
- Passing `idempotencyKey` to a list method is refused (`sdk.idempotency_unsupported`) as the pager is
  built, instead of being dropped.
- `Money` helpers raise `ConfigException` (`sdk.bad_amount`) instead of `IllegalArgumentException`.
  If you were catching `IllegalArgumentException` around them, catch `OblodaiException` instead.
- Both clients are `AutoCloseable`, and `cancel(true)` on a future the async client returned aborts
  the exchange in flight.
- Kotlin's `asFlow()` no longer drops items on a large result set; it pulls a page at a time.
