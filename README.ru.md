<div align="center">

<a href="https://oblodai.com">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-white.svg">
    <img src="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-black.svg" alt="oblodai" height="52">
  </picture>
</a>

<h3>Официальный Java / Kotlin SDK для платёжного шлюза <a href="https://oblodai.com">oblodai</a></h3>

Платежи, выплаты, платёжные ссылки, сплиты, статические кошельки, вебхуки — один API-ключ.

<img src="https://img.shields.io/badge/maven-com.oblodai%3Aoblodai--sdk%202.0.0-C71A36?style=flat-square" alt="maven">
<a href="https://github.com/oblodai/oblodai-java/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/oblodai/oblodai-java/ci.yml?branch=main&style=flat-square&label=CI" alt="CI"></a>
<img src="https://img.shields.io/badge/java-17%2B-007396?style=flat-square" alt="Java 17+">
<a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-000000?style=flat-square" alt="License: MIT"></a>

[Documentation](https://docs.oblodai.com) · [Dashboard](https://my.oblodai.com) · [Read in English →](README.md)

</div>

---

Официальный Java / Kotlin SDK платёжного шлюза **Oblodai**: приём платежей, выплаты, пакеты,
платёжные ссылки, ссылки на выплату (крипточеки), сплиты, статические кошельки, вебхуки, документы.
Java 17 и новее; одна зависимость времени выполнения (Jackson — для дерева JSON) поверх
`java.net.http.HttpClient` из JDK. Для Kotlin — корутины и flow в том же артефакте.

Версия 2 генерируется из OpenAPI-контракта шлюза: все ресурсы, методы и модели в
`com.oblodai.generated` строит `tools/sdkgen` из `openapi.json`, и проверка дрейфа это стережёт.
Runtime вокруг — подпись, повторы, идемпотентность, постраничность, ошибки, вебхуки — написан
руками, один раз. Переходите с 1.x — см. [MIGRATION-2.0.md](MIGRATION-2.0.md).

## Установка

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>2.0.0</version>
</dependency>
```

```gradle
implementation("com.oblodai:oblodai-sdk:2.0.0")
```

## Ключи

Один API-ключ подписывает все маршруты: публичный идентификатор `oblodai_<hex>` и секрет
`oblodai_live_<hex>` из [кабинета](https://my.oblodai.com) (песочница: `test_oblodai_<hex>` /
`oblodai_test_<hex>`). Без явной передачи берутся из `OBLODAI_PUBLIC_ID` и `OBLODAI_SECRET`.
Единственный другой секрет — админ-токен своего шлюза (`adminToken(...)`, `OBLODAI_ADMIN_TOKEN`),
он уходит только на маршрут подключения магазина.

## Быстрый старт

Клиент создаётся один раз и используется всеми: он неизменяемый и потокобезопасный.

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
        .secret(System.getenv("OBLODAI_SECRET"))
        .build();
```

Принять платёж:

```java
PaymentView invoice = oblodai.payments().create(PaymentRequest.builder()
        .amount("25")                  // a decimal string or a BigDecimal, never a double
        .currency("USDT")
        .orderId("order-1001")         // your reference; the invoice is idempotent per order
        .urlCallback("https://shop.example/oblodai/webhook")
        .build());

System.out.println(invoice.url());     // send the payer here
```

Прочитать счёт (источник истины — вебхуки; это для сверки):

```java
PaymentInfoResult now = oblodai.payments().getInfo(
        LookupRequest.builder().uuid(invoice.uuid()).build());

if (Statuses.isPaymentPaid(now.status())) {
    BigDecimal received = Money.toBigDecimal(now.merchantAmount());   // exact, never a double
    System.out.println("paid, " + received + " credited");
}
```

Выплата под своим ключом идемпотентности — повтор после падения не заплатит дважды:

```java
PayoutItem payout = oblodai.payouts().create(
        PayoutRequest.builder()
                .amount(new BigDecimal("10.50"))
                .currency("USDT")
                .network("tron")
                .address("TXYZ")
                .orderId("withdrawal-77")
                .build(),
        RequestOptions.of().idempotencyKey("withdrawal-77"));   // one key for every retry
```

### Суммы

В моделях суммы — `BigDecimal`, на проводе — десятичные строки. Builder принимает `BigDecimal` или
десятичную строку (`.amount("10.50")`); перегрузки с `double` нет, а `double`, попавший в запрос
иначе (`putExtra`, разобранная карта), — ошибка `sdk.float_amount` до отправки. `Money` складывает,
вычитает и сравнивает суммы точно и тоже не принимает `double`.

## Ресурсы и методы

`client.<ресурс>().<метод>(параметры, опции)`: ресурс — тег контракта, метод — его `operationId`
без имени ресурса. Параметры — модель со своим builder'ом (`PaymentRequest.builder()...build()`);
параметры пути идут первыми, параметры запроса — модель `*Query`. У каждого метода есть перегрузка
с `RequestOptions` последним аргументом и перегрузки без необязательных частей. Полный список — в
[`names.lock`](names.lock).

| ресурс | методы |
| --- | --- |
| `account()` | `getBalance`, `getSummary`, `listExchangeRates` |
| `apiAllowlist()` | `addEntry`, `list`, `removeEntry`, `setEnabled` |
| `batches()` | `createPayment`, `createPayout`, `createRefund`, `getInfo` |
| `checkout()` | `get`, `getOnramp`, `getPublicPaymentLink`, `getQr`, `getSourceOfFundsForm`, `listCurrencies`, `paymentLink`, `selectMethod`, `startOnramp`, `submitSourceOfFunds` |
| `documents()` | `createJob`, `downloadJobFile`, `getBalance`, `getBatch`, `getFees`, `getJob`, `getLedger`, `getPaymentLink`, `getPayoutLinkCheque`, `getReferrals`, `getSigned`, `getSplit`, `getStatement`, `getWalletStatement` |
| `paymentLinks()` | `create`, `get`, `list`, `toggle` |
| `payments()` | `cancel`, `create`, `getAmlLinks`, `getCheckoutConfig`, `getInfo`, `getQr`, `listHistory`, `listServices`, `resolve`, `sendEmail`, `setCheckoutConfig` |
| `payoutLinks()` | `cancel`, `claimPayout`, `create`, `createBatch`, `get`, `getPayoutClaim`, `list` |
| `payouts()` | `approve`, `calculate`, `cancel`, `create`, `createMass`, `createTransferBatch`, `getInfo`, `listHistory`, `listServices`, `transferToPersonal`, `transferToUser`, `validate` |
| `referrals()` | `getInfo` |
| `refunds()` | `blockedWallet`, `payment` |
| `sandbox()` | `faucet`, `listWebhooks`, `onboardStore`, `replayWebhook`, `reset`, `simulateDeposit` |
| `settings()` | `configureVrcs`, `deleteAutoWithdrawRule`, `getAccuracy`, `getAutoConvert`, `getAutoRefund`, `getPaymentFeeConfig`, `getPayoutFeeConfig`, `getRefundFeeConfig`, `listAcceptedCurrencies`, `listApiLog`, `listAutoWithdrawRules`, `listDiscounts`, `setAcceptedCurrencies`, `setAccuracy`, `setAutoConvert`, `setAutoRefund`, `setAutoWithdrawRule`, `setDiscount`, `setPaymentFeeConfig`, `setPayoutFeeConfig`, `setRefundFeeConfig` |
| `splits()` | `createRule`, `deleteRule`, `getConfig`, `getRecipientOptIn`, `listRules`, `setConfig`, `setRecipientOptIn` |
| `wallets()` | `block`, `create`, `getQr` |
| `webhooks()` | `listDeliveries`, `register`, `requeueDelivery`, `resendPayment`, `rotateSecret`, `sendLegacyTest`, `sendTestConversion`, `sendTestPayment`, `sendTestPayout`, `sendTestWallet`, `setActive` |

Модели сохраняют поля, которых эта версия SDK ещё не знает (`extra()`, уходят обратно как пришли), а
незнакомое значение enum разбирается (`PaymentStatus.of("new_status").isKnown() == false`).
`toString()` короткий и не показывает секретов.

## Опции вызова

Пять опций, все явные: `idempotencyKey`, `timeout` (`Duration`, на попытку), `maxRetries`,
`extraHeaders` и `requestId` (уходит как `X-Request-ID`; без него — новый UUID, один на все попытки
вызова). `withOptions(...)` — копия клиента с другими значениями по умолчанию.

```java
oblodai.account().getBalance(RequestOptions.of()
        .timeout(Duration.ofSeconds(5))       // per attempt
        .maxRetries(0)                        // this call only
        .extraHeader("X-Trace", "t-42")
        .requestId("order-1001-balance"));    // sent as X-Request-ID

Oblodai patient = oblodai.withOptions(RequestOptions.of().timeout(Duration.ofSeconds(60)));
```

## Списки

Списочный метод возвращает ленивый `Pager`: перебор — все элементы, `byPage()` — все страницы,
`firstPage()` — одна, `all(max)` — собрать. Пока его не читают, запросов нет.

```java
int seen = 0;
for (PaymentView p : oblodai.payments().listHistory()) {            // every item, page by page
    System.out.println(p.orderId() + " " + p.status());
    seen++;
}
for (Page<PaymentView> page : oblodai.payments().listHistory(
        HistoryRequest.builder().limit(100L).build()).byPage()) {  // one request per page
    System.out.println(page.items().size() + " of " + page.total());
}
```

## Ошибки

Всё, что бросает SDK, — непроверяемое `OblodaiException` с `code()` (`семейство.причина`),
`httpStatus()`, `retryable()`, `retryAfter()`, `requestId()` и `field()`; `getMessage()` —
`[код] текст (request_id=...)`. Подклассы по статусу: `ValidationException` 400,
`AuthenticationException` 401, `PermissionException` 403, `NotFoundException` 404,
`ConflictException` / `IdempotencyConflictException` 409, `RateLimitException` 429,
`UnavailableException` 503, `InternalException`, `TransportException` (ответа нет),
`ConfigException` (отказ до отправки), `ContractException` (ответ, который SDK не может прочитать).

```java
String code = null;
try {
    oblodai.payments().getInfo(LookupRequest.builder().uuid("missing").build());
} catch (OblodaiException e) {
    System.err.println(e.getMessage());   // [payment.not_found] … (request_id=…)
    code = e.code();                      // retryable(), field(), requestId() as well
}
```

## Повторы и идемпотентность

Вызов повторяется (по умолчанию два повтора, экспоненциальная пауза со случайной добавкой,
`Retry-After` соблюдается) только когда повтор не может задвоить действие: маршрут на чтение или
запись, которую шлюз дедуплицирует по `Idempotency-Key`. Для таких маршрутов SDK сам создаёт ключ и
шлёт его во всех попытках; свой ключ (`RequestOptions.idempotencyKey`) переживает и перезапуск.
Запись без дедупликации после таймаута или ошибки прокси не повторяется, а ключ для неё — отказ
(`sdk.idempotency_unsupported`). На 409 `idempotency.in_progress` SDK ждёт и повторяет с тем же
ключом. Весь вызов ограничен одним сроком (по умолчанию 90 с).

## Сырой ответ и хуки

```java
ApiResponse<PaymentView> raw = oblodai.withRawResponse(c -> c.payments().create(
        PaymentRequest.builder().amount("25").currency("USDT").build()));
System.out.println(raw.status() + " " + raw.requestId() + " " + raw.value().uuid());

Oblodai observed = Oblodai.builder()
        .onRequest(r -> System.out.println("-> " + r.operationId() + " #" + r.attempt()))
        .onResponse(r -> System.out.println("<- " + r.status() + " in " + r.elapsed()))
        .build();
observed.close();
```

## Долгие операции

Пакеты и выгрузки документов завершаются в фоне. `jobs()` следит за ними: `waitFor()` опрашивает,
пока статус не станет конечным (`completed`, `stopped`, `done`, `failed`, `expired`), и возвращает
этот ответ; `download()` скачивает файл выгрузки.

```java
BatchSubmitResponse submitted = oblodai.batches().createPayout(PayoutBatchRequest.builder()
        .payouts(List.of(PayoutRequest.builder()
                .amount("5").currency("USDT").network("tron").address("TXYZ").orderId("b-1")
                .build()))
        .build());
BatchInfoResponse batch = oblodai.jobs().batch(submitted).waitFor();   // polls until terminal
System.out.println(batch.status() + ": " + batch.succeeded() + "/" + batch.total());

DocumentJobAccepted export = oblodai.documents().createJob(
        DocumentJobRequest.builder().kind(DocumentJobKind.STATEMENT).build());
Job<?> job = oblodai.jobs().document(export);
job.waitFor(Duration.ofMinutes(2), Duration.ofSeconds(3));
FileResult file = job.download();                                   // .writeTo(Path.of(...))
System.out.println(file.filename() + " " + Path.of(".").toAbsolutePath());
```

## Вебхуки

Проверяйте подпись по сырым байтам запроса; дедуплицируйте по `eventId()` (`X-Webhook-Event-Id`);
не действуйте по тестовой доставке. `event().asPayment()`, `asPayout()`, `asWallet()`,
`asConversion()` — типизированное событие; событие незнакомого вида всё равно доставляется, с
исходным `type()` и `fields()`.

```java
try {
    WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
            rawBody,                                    // the RAW bytes, not a re-serialized body
            WebhookHeaders.of(headers),
            WebhookVerifier.options(secret));
    if (delivery.isTest()) {
        return 200;                                     // a rehearsal: no money moved
    }
    if (delivery.event().type().equals("payment")) {
        System.out.println(delivery.event().asPayment().status());
    }
    return 200;
} catch (SignatureException e) {
    return 401;
}
```

## Асинхронный клиент

`oblodai.async()` (или `builder().buildAsync()`) — те же ресурсы, возвращающие `CompletableFuture`;
отмена future отменяет HTTP-обмен.

```java
String uuid = oblodai.async().payments()
        .create(PaymentRequest.builder().amount("25").currency("USDT").build())
        .thenApply(PaymentView::uuid)
        .join();
```

### Kotlin

```kotlin
val async = oblodai.async()
val invoice = async.payments().create(
    PaymentRequest.builder().amount("25").currency("USDT").orderId("order-1001").build()
).await()                                          // suspends; cancelling cancels the call
val all = async.payments().listHistory().asFlow().toList()
println("${invoice.uuid()}: ${all.size} invoices")
```

## Настройка

| опция | переменная окружения | по умолчанию |
| --- | --- | --- |
| `publicId(...)` / `secret(...)` | `OBLODAI_PUBLIC_ID` / `OBLODAI_SECRET` | нет |
| `baseUrl(...)` | `OBLODAI_BASE_URL` | `https://api.oblodai.com` |
| `adminToken(...)` | `OBLODAI_ADMIN_TOKEN` | нет |
| `allowInsecureBaseUrl(true)` | `OBLODAI_ALLOW_INSECURE=1` | только https, кроме loopback |
| `logger(...)` | `OBLODAI_LOG=debug\|info\|warn\|error` | молчит |
| `timeout(Duration)` / `deadline(Duration)` | | 30 с на попытку / 90 с на вызов |
| `retry(RetryOptions)` / `maxRetries(int)` | | 2 повтора |
| `header(имя, значение)`, `onRequest(...)`, `onResponse(...)`, `httpClient(...)` | | |

## Разработка

```bash
OBLODAI_BACKEND=../oblodai-backend make ci   # drift, build + lint + tests + conformance, package
```

`make ci` запускает Maven в docker (`maven:3-eclipse-temurin-21`, кэш в `.m2cache/`). Проверка
дрейфа перегенерирует `src/main/java/com/oblodai/generated` генератором бэкенда `tools/sdkgen` и
сравнивает; набор conformance (`tools/sdkgen/conformance`) прогоняет общие сценарии на обоих
клиентах. Сгенерированный код руками не правится: меняется контракт или генератор, затем
`make sdk` в бэкенде.

## Лицензия

MIT — см. [LICENSE](LICENSE).
