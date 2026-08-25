# Examples

Runnable programs against a sandbox key. They are compiled by `mvn verify`, so they cannot drift
away from the SDK.

| file                                                     | what it shows                                              |
| -------------------------------------------------------- | ---------------------------------------------------------- |
| `java/com/oblodai/examples/AcceptPayment.java`            | create an invoice, read it back, interpret its status        |
| `java/com/oblodai/examples/SendPayout.java`               | dry-run a payout, then send it under your own idempotency key |
| `java/com/oblodai/examples/WebhookReceiver.java`          | verify a delivery over the raw bytes, deduplicate, act        |
| `kotlin/com/oblodai/examples/AcceptPaymentKotlin.kt`      | the same journey with coroutines and the request DSL          |

Get a sandbox key from the dashboard (or, on a self-hosted gateway, with
`merchants().create(...)` followed by `merchants().createSandbox(...)`), then:

```bash
export OBLODAI_PUBLIC_ID=test_… OBLODAI_SECRET=…
# a local gateway: export OBLODAI_BASE_URL=http://127.0.0.1:8095

mvn -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.oblodai.examples.AcceptPayment
mvn -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.oblodai.examples.SendPayout
OBLODAI_WEBHOOK_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.oblodai.examples.WebhookReceiver
mvn -q exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.oblodai.examples.AcceptPaymentKotlinKt
```

The sandbox pays invoices for you: `sandbox().deposit(...)` simulates an on-chain deposit,
`sandbox().faucet(...)` credits test funds, and `sandbox().webhooks()` shows what was delivered.
