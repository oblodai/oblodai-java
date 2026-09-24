# Migrating from 1.x to 2.0

2.0 is generated from the gateway's OpenAPI contract. The runtime (signing, retries, idempotency,
webhooks) behaves as before; the surface around it changed in one sweep so every SDK of the family
names things the same way. There are no users of 1.x to keep compatible (the owner's decision), so
nothing is deprecated - it is renamed.

## At a glance

| 1.x | 2.0 |
| --- | --- |
| `com.oblodai.contract.requests.*`, `com.oblodai.models.*` (hand-written, Jackson-bound) | `com.oblodai.generated.models.*` - generated, parsed field by field |
| `new PaymentRequest().amount("25").currency("USDT")` | `PaymentRequest.builder().amount("25").currency("USDT").build()` |
| amounts are `String` | amounts are `BigDecimal` (the builder also takes a decimal string); a `double` is `sdk.float_amount` |
| `com.oblodai.resources.*`, `resources.async.*` | `com.oblodai.generated.resources.*`, `...resources.async.*` |
| method names per resource, with aliases (`info`/`get`, `history`/`list`) | one name per operation: `operationId` without the resource (table below), no aliases |
| `payments().info("uuid")` | `payments().getInfo(LookupRequest.builder().uuid("uuid").build())` |
| `catalog()`, `transfers()`, `merchants()` | `checkout()` / `account()`, `payouts()`, `sandbox().onboardStore(id)` |
| `RequestOptions.of().header(n, v)` | `RequestOptions.of().extraHeader(n, v)` / `extraHeaders(map)` |
| `RequestOptions.of().deadline(d)` | the deadline is client-wide: `builder().deadline(d)` |
| - | `RequestOptions.of().maxRetries(n)`, `.requestId(id)` (sent as `X-Request-ID`) |
| `e.getMessage()` = the gateway's text | `e.getMessage()` = `[code] text (request_id=…)`; `e.text()` = the text alone |
| `Money.add("1", "2")` returns `String` | `Money.add(...)` takes `BigDecimal`, a decimal string or an integer and returns `BigDecimal` |
| `Pager`: `firstPage()`, iteration, `stream()`, `all(max)` | the same, plus `byPage()`; `Page.hasPages()` / `total()` |
| `batches().info(...)` polled by hand | `jobs().batch(submitted).waitFor()`, `jobs().document(accepted).waitFor()` / `.download()` |
| - | `withOptions(RequestOptions)`, `withRawResponse(c -> ...)`, `builder().onRequest(...)` / `onResponse(...)` |
| `WebhookEvent` sealed union (`PaymentEvent`, `PayoutEvent`, `WalletEvent`, `UnknownEvent`) | `WebhookEvent` over the verified body: `type()`, `uuid()`, `sequence()`, …, typed `asPayment()` / `asPayout()` / `asWallet()` / `asConversion()` from the generated models |
| `WebhookDeliveryInfo(event, id, eventType, …)` | adds `eventId()` (`X-Webhook-Event-Id`), the key to deduplicate on |
| `builder().objectMapper(...)` | removed: Jackson only reads the JSON tree now |
| Kotlin request DSL (`payment { ... }`) | removed: the generated builders read the same from Kotlin; `await()`, `asFlow()`, `asSequence()`, `verifyWebhook()` stay |
| `contract/`, `codegen/` | removed; `names.lock` holds the public names, `make ci` checks drift against the backend |

## Methods

Every route, its 1.x method(s) and its 2.0 method. The 2.0 names are fixed in
[`names.lock`](names.lock) (snake_case there, camelCase in Java); a later contract can add names but
never silently drop or rename one.

| route | 1.x | 2.0 |
| --- | --- | --- |
| `POST /v1/balance` | `account().balance` | `account().getBalance` |
| `POST /v1/summary` | — | `account().getSummary` |
| `POST /v1/exchange-rate/list` | `catalog().exchangeRates` | `account().listExchangeRates` |
| `POST /v1/api-allowlist/add` | `settings().addApiAllowlist` | `apiAllowlist().addEntry` |
| `POST /v1/api-allowlist/list` | `settings().listApiAllowlist` | `apiAllowlist().list` |
| `POST /v1/api-allowlist/remove` | `settings().removeApiAllowlist` | `apiAllowlist().removeEntry` |
| `POST /v1/api-allowlist/enable` | `settings().enableApiAllowlist` | `apiAllowlist().setEnabled` |
| `POST /v1/payment/batch` | `payments().batch` | `batches().createPayment` |
| `POST /v1/payout/batch` | `payouts().batch` | `batches().createPayout` |
| `POST /v1/refund/batch` | `refunds().batch` | `batches().createRefund` |
| `POST /v1/batch/info` | `batches().info` | `batches().getInfo` |
| `GET /v1/pay/{id}` | `payments().publicView` | `checkout().get` |
| `GET /v1/pay/{id}/onramp` | — | `checkout().getOnramp` |
| `GET /v1/link/{id}` | `paymentLinks().publicView` | `checkout().getPublicPaymentLink` |
| `GET /v1/pay/{id}/qr` | `payments().publicQr` | `checkout().getQr` |
| `GET /v1/aml/{token}` | — | `checkout().getSourceOfFundsForm` |
| `GET /v1/currencies` | `catalog().currencies` | `checkout().listCurrencies` |
| `POST /v1/link/{id}/checkout` | `paymentLinks().checkout` | `checkout().paymentLink` |
| `POST /v1/pay/{id}/select` | `payments().select` | `checkout().selectMethod` |
| `POST /v1/pay/{id}/onramp` | — | `checkout().startOnramp` |
| `POST /v1/aml/{token}` | — | `checkout().submitSourceOfFunds` |
| `POST /v1/documents/jobs` | `documents().createJob` | `documents().createJob` |
| `GET /v1/documents/jobs/file` | `documents().jobFile` | `documents().downloadJobFile` |
| `GET /v1/documents/balance` | `documents().balanceCertificate` | `documents().getBalance` |
| `GET /v1/documents/batch` | `documents().batchReport` | `documents().getBatch` |
| `GET /v1/documents/fees` | `documents().feeSchedule` | `documents().getFees` |
| `POST /v1/documents/jobs/info` | `documents().jobInfo` | `documents().getJob` |
| `GET /v1/documents/ledger` | `documents().ledger` | `documents().getLedger` |
| `GET /v1/documents/link` | `documents().linkReport` | `documents().getPaymentLink` |
| `POST /v1/payout/link/cheque` | `payoutLinks().cheque` | `documents().getPayoutLinkCheque` |
| `GET /v1/documents/referrals` | `documents().referralsReport` | `documents().getReferrals` |
| `GET /v1/documents/{kind}/{id}` | `documents().download` | `documents().getSigned` |
| `GET /v1/documents/split` | `documents().splitReport` | `documents().getSplit` |
| `GET /v1/documents/statement` | `documents().statement` | `documents().getStatement` |
| `GET /v1/documents/wallet/statement` | `documents().walletStatement` | `documents().getWalletStatement` |
| `POST /v1/payment/link` | `paymentLinks().create` | `paymentLinks().create` |
| `POST /v1/payment/link/info` | `paymentLinks().info` | `paymentLinks().get` |
| `POST /v1/payment/link/list` | `paymentLinks().list` | `paymentLinks().list` |
| `POST /v1/payment/link/toggle` | `paymentLinks().toggle` | `paymentLinks().toggle` |
| `POST /v1/payment/cancel` | `payments().cancel` | `payments().cancel` |
| `POST /v1/payment` | `payments().create` | `payments().create` |
| `POST /v1/payment/aml-links` | — | `payments().getAmlLinks` |
| `POST /v1/checkout-config/get` | — | `payments().getCheckoutConfig` |
| `POST /v1/payment/info` | `payments().info` | `payments().getInfo` |
| `POST /v1/payment/qr` | `payments().qr` | `payments().getQr` |
| `POST /v1/payment/history` | `payments().history` | `payments().listHistory` |
| `POST /v1/payment/services` | `payments().services` | `payments().listServices` |
| `POST /v1/payment/resolve` | `refunds().resolve` | `payments().resolve` |
| `POST /v1/payment/send-email` | `payments().sendEmail` | `payments().sendEmail` |
| `POST /v1/checkout-config/set` | — | `payments().setCheckoutConfig` |
| `POST /v1/payout/link/cancel` | `payoutLinks().cancel` | `payoutLinks().cancel` |
| `POST /v1/claim/{token}` | `payoutLinks().claim` | `payoutLinks().claimPayout` |
| `POST /v1/payout/link` | `payoutLinks().create` | `payoutLinks().create` |
| `POST /v1/payout/link/batch` | `payoutLinks().batch` | `payoutLinks().createBatch` |
| `POST /v1/payout/link/info` | `payoutLinks().info` | `payoutLinks().get` |
| `GET /v1/claim/{token}` | `payoutLinks().claimPreview` | `payoutLinks().getPayoutClaim` |
| `POST /v1/payout/link/list` | `payoutLinks().list` | `payoutLinks().list` |
| `POST /v1/payout/approve` | `payouts().approve` | `payouts().approve` |
| `POST /v1/payout/calculate` | `payouts().calculate` | `payouts().calculate` |
| `POST /v1/payout/cancel` | `payouts().cancel` | `payouts().cancel` |
| `POST /v1/payout` | `payouts().create` | `payouts().create` |
| `POST /v1/payout/mass` | `payouts().mass` | `payouts().createMass` |
| `POST /v1/transfer/batch` | `transfers().batch` | `payouts().createTransferBatch` |
| `POST /v1/payout/info` | `payouts().info` | `payouts().getInfo` |
| `POST /v1/payout/history` | `payouts().history` | `payouts().listHistory` |
| `POST /v1/payout/services` | `payouts().services` | `payouts().listServices` |
| `POST /v1/transfer/to-personal` | `transfers().toPersonal` | `payouts().transferToPersonal` |
| `POST /v1/transfer/to-user` | `transfers().toUser` | `payouts().transferToUser` |
| `POST /v1/payout/validate` | `payouts().validate` | `payouts().validate` |
| `POST /v1/referral/info` | `account().referral` | `referrals().getInfo` |
| `POST /v1/wallet/blocked-address-refund` | `wallets().refundBlockedDeposit` | `refunds().blockedWallet` |
| `POST /v1/payment/refund` | `refunds().create` | `refunds().payment` |
| `POST /v1/sandbox/faucet` | `sandbox().faucet` | `sandbox().faucet` |
| `GET /v1/sandbox/webhooks` | `sandbox().webhooks` | `sandbox().listWebhooks` |
| `POST /v1/merchants/{id}/sandbox` | `merchants().createSandbox` | `sandbox().onboardStore` |
| `POST /v1/sandbox/webhooks/replay` | `sandbox().replay` | `sandbox().replayWebhook` |
| `POST /v1/sandbox/reset` | `sandbox().reset` | `sandbox().reset` |
| `POST /v1/sandbox/deposit` | `sandbox().deposit` | `sandbox().simulateDeposit` |
| `POST /v1/vrcs` | `account().vrcs` | `settings().configureVrcs` |
| `POST /v1/auto-withdraw/delete` | `settings().deleteAutoWithdraw` | `settings().deleteAutoWithdrawRule` |
| `POST /v1/payment/accuracy/get` | `settings().getAccuracy` | `settings().getAccuracy` |
| `POST /v1/payment/autoconvert/get` | — | `settings().getAutoConvert` |
| `POST /v1/payment/autorefund/get` | `settings().getAutoRefund` | `settings().getAutoRefund` |
| `POST /v1/payment/fee-config/get` | `settings().getPaymentFeeConfig` | `settings().getPaymentFeeConfig` |
| `POST /v1/payout/fee-config/get` | `payouts().getFeeConfig` | `settings().getPayoutFeeConfig` |
| `POST /v1/payout/refund-fee-config/get` | `payouts().getRefundFeeConfig` | `settings().getRefundFeeConfig` |
| `POST /v1/payment/accepted/list` | `settings().listAccepted` | `settings().listAcceptedCurrencies` |
| `POST /v1/payment/api-log` | — | `settings().listApiLog` |
| `POST /v1/auto-withdraw/list` | `settings().listAutoWithdraw` | `settings().listAutoWithdrawRules` |
| `POST /v1/payment/discount/list` | `settings().listDiscounts` | `settings().listDiscounts` |
| `POST /v1/payment/accepted/set` | `settings().setAccepted` | `settings().setAcceptedCurrencies` |
| `POST /v1/payment/accuracy/set` | `settings().setAccuracy` | `settings().setAccuracy` |
| `POST /v1/payment/autoconvert/set` | — | `settings().setAutoConvert` |
| `POST /v1/payment/autorefund/set` | `settings().setAutoRefund` | `settings().setAutoRefund` |
| `POST /v1/auto-withdraw/set` | `settings().setAutoWithdraw` | `settings().setAutoWithdrawRule` |
| `POST /v1/payment/discount/set` | `settings().setDiscount` | `settings().setDiscount` |
| `POST /v1/payment/fee-config/set` | `settings().setPaymentFeeConfig` | `settings().setPaymentFeeConfig` |
| `POST /v1/payout/fee-config/set` | `payouts().setFeeConfig` | `settings().setPayoutFeeConfig` |
| `POST /v1/payout/refund-fee-config/set` | `payouts().setRefundFeeConfig` | `settings().setRefundFeeConfig` |
| `POST /v1/split/rule` | `splits().createRule` | `splits().createRule` |
| `POST /v1/split/rule/delete` | `splits().deleteRule` | `splits().deleteRule` |
| `POST /v1/split/config/get` | `splits().getConfig` | `splits().getConfig` |
| `POST /v1/split/recipient/optin/get` | `splits().getOptIn` | `splits().getRecipientOptIn` |
| `POST /v1/split/rule/list` | `splits().listRules` | `splits().listRules` |
| `POST /v1/split/config/set` | `splits().setConfig` | `splits().setConfig` |
| `POST /v1/split/recipient/optin` | `splits().setOptIn` | `splits().setRecipientOptIn` |
| `POST /v1/wallet/block` | `wallets().block` | `wallets().block` |
| `POST /v1/wallet` | `wallets().create` | `wallets().create` |
| `POST /v1/wallet/qr` | `wallets().qr` | `wallets().getQr` |
| `POST /v1/webhooks/deliveries` | `webhooks().deliveries` | `webhooks().listDeliveries` |
| `POST /v1/webhooks` | `webhooks().register` | `webhooks().register` |
| `POST /v1/webhooks/deliveries/requeue` | — | `webhooks().requeueDelivery` |
| `POST /v1/payment/resend` | `payments().resend` | `webhooks().resendPayment` |
| `POST /v1/webhooks/rotate-secret` | `webhooks().rotateSecret` | `webhooks().rotateSecret` |
| `POST /v1/payment/testing-webhook` | `webhooks().testLegacy` | `webhooks().sendLegacyTest` |
| `POST /v1/test-webhook/conversion` | — | `webhooks().sendTestConversion` |
| `POST /v1/test-webhook/payment` | `webhooks().testLegacy`, `webhooks().testPayment` | `webhooks().sendTestPayment` |
| `POST /v1/test-webhook/payout` | `webhooks().testLegacy`, `webhooks().testPayout` | `webhooks().sendTestPayout` |
| `POST /v1/test-webhook/wallet` | `webhooks().testLegacy`, `webhooks().testWallet` | `webhooks().sendTestWallet` |
| `POST /v1/webhooks/active` | — | `webhooks().setActive` |
