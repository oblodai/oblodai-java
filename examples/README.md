# Examples

Runnable programs against a sandbox key. They are compiled with the tests and run there against a
scripted gateway (`ReadmeExamplesTest`, `ReadmeKotlinTest`), so they cannot drift away from the SDK.

| file                                                  | what it shows                                                  |
| ----------------------------------------------------- | -------------------------------------------------------------- |
| `java/com/oblodai/examples/AcceptPayment.java`         | create an invoice, read it back, interpret its status           |
| `java/com/oblodai/examples/SendPayout.java`            | dry-run a payout, then send it under your own idempotency key   |
| `java/com/oblodai/examples/WebhookReceiver.java`       | verify a delivery over the raw bytes, deduplicate, act          |
| `java/com/oblodai/examples/ReadmeSnippets.java`        | every Java block of README.md and README.ru.md                  |
| `kotlin/com/oblodai/examples/AcceptPaymentKotlin.kt`   | the same journey with coroutines and flows                      |
| `kotlin/com/oblodai/examples/ReadmeSnippetsKotlin.kt`  | the Kotlin block of the READMEs                                 |

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

The sandbox pays invoices for you: `sandbox().simulateDeposit(...)` simulates an on-chain deposit,
`sandbox().faucet(...)` credits test funds, and `sandbox().listWebhooks()` shows what was delivered.
