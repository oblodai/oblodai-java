<div align="center">

<a href="https://oblodai.com">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-white.svg">
    <img src="https://raw.githubusercontent.com/oblodai/.github/main/brand/logo-black.svg" alt="oblodai" height="52">
  </picture>
</a>

<h3>Официальный Java / Kotlin SDK для платёжного шлюза <a href="https://oblodai.com">oblodai</a></h3>

Платежи, выплаты, платёжные ссылки, сплиты, статические кошельки, вебхуки — один API-ключ.

<img src="https://img.shields.io/badge/maven-com.oblodai%3Aoblodai--sdk%201.3.0-C71A36?style=flat-square" alt="maven">
<a href="https://github.com/oblodai/oblodai-java/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/oblodai/oblodai-java/ci.yml?branch=main&style=flat-square&label=CI" alt="CI"></a>
<img src="https://img.shields.io/badge/java-17%2B-007396?style=flat-square" alt="Java 17+">
<a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-000000?style=flat-square" alt="License: MIT"></a>

[Documentation](https://docs.oblodai.com) · [Dashboard](https://my.oblodai.com) · [Read in English →](README.md)

</div>

---

Официальный Java / Kotlin SDK для платёжного шлюза **Oblodai**: приём платежей, выплаты, массовые
операции (батчи), платёжные ссылки, выплатные ссылки (крипточеки), сплиты, статические кошельки,
переводы, вебхуки. Подпись запросов, разбор ответов, типизированные ошибки, идемпотентность и
ретраи — из коробки. Java 17 и новее, одна runtime-зависимость (Jackson) поверх штатного
`java.net.http.HttpClient` из JDK; Kotlin-расширения — корутины и DSL для запросов — объявлены
опциональными зависимостями, так что Java-проект их не тянет.

> **Базовый URL.** По умолчанию `https://api.oblodai.com`. При необходимости переопределите
> `baseUrl(...)` и передайте свои ключи при инициализации. Схема должна быть `https://`; обычный
> `http://` принимается только для loopback (`http://127.0.0.1:8095`) или с явной опцией
> «разрешить небезопасный адрес» (`allowInsecureBaseUrl(true)` или `OBLODAI_ALLOW_INSECURE=1`).

## Установка

```xml
<dependency>
  <groupId>com.oblodai</groupId>
  <artifactId>oblodai-sdk</artifactId>
  <version>1.3.0</version>
</dependency>
```

```gradle
implementation("com.oblodai:oblodai-sdk:1.3.0")
```

Java 17 и новее. Jackson — единственная runtime-зависимость; HTTP-клиент берётся из JDK
(`java.net.http.HttpClient`). Kotlin-разработчики получают корутины и DSL для запросов из того же
артефакта: `kotlin-stdlib` и `kotlinx-coroutines` объявлены `optional`, поэтому подключите их сами,
если эта половина вам нужна.

## Где взять ключи

Ключи выдаются в [личном кабинете](https://my.oblodai.com) в разделе **API keys**. Шлюз выдаёт два
вида ключей, и вызов не тем ключом — это 403 `merchant.wrong_key_kind`:

- **платёжный ключ** — деньги внутрь: инвойсы, платёжные ссылки, кошельки, документы, балансы,
  справочники;
- **выплатной ключ** — деньги наружу и всё, что может двинуть или разблокировать деньги:
  `payouts()`, `refunds()`, `payoutLinks()`, `transfers()`, `splits()`,
  `wallets().refundBlockedDeposit(...)`, правила авто-вывода и IP-allow-list в `settings()`,
  `webhooks().rotateSecret()`, `webhooks().testPayout(...)`, `sandbox().faucet(...)` и
  `sandbox().reset()`.

Боевая пара — это public id `oblodai_<hex>` (единый API-ключ; устаревшие виды пишутся как
`oblodai_pk_<hex>` для платёжного и `oblodai_wk_<hex>` для выплатного) и секрет `oblodai_live_<hex>`.
Пара песочницы — это `test_oblodai_<hex>` и секрет `oblodai_test_<hex>`, причём в песочнице одна пара
работает сразу как оба вида ключа, поэтому интеграции с песочницей выплатной ключ можно не задавать.
Боевые ключи — это две отдельные пары: передайте клиенту обе, и он подпишет каждый вызов тем видом
ключа, который объявляет маршрут.

Есть и третий реквизит — только для платформ, которые сами заводят мерчантов на self-hosted шлюзе:
**админ-токен**, задаётся через `adminToken(...)` (или `OBLODAI_ADMIN_TOKEN`). Он уходит заголовком
`X-Admin-Token` на неподписанных маршрутах `merchants()` и больше нигде.

## Быстрый старт

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
        .secret(System.getenv("OBLODAI_SECRET"))
        .build();

Payment invoice = oblodai.payments().create(new PaymentRequest()
        .amount("25")                 // amounts are decimal strings, never floats
        .currency("USDT")             // what you price in: a fiat (USD, EUR, …) or a crypto asset
        .network(Network.TRON)        // omit it to let the payer choose the network on the pay page
        .orderId("order-1001")        // your reference; the invoice is idempotent per order_id
        .urlCallback("https://shop.example/oblodai/webhook"));

System.out.println(invoice.url() + " " + invoice.address() + " " + invoice.status()); // created
```

Чтобы выставить цену в фиате, используйте `.amount("25").currency("USD").toCurrency("USDT")` —
`currency` это то, в чём вы выставляете счёт, а `to_currency` — актив, который отправит плательщик.

Для денег наружу нужен выплатной ключ. Сначала прогоните проверку, затем создайте выплату со своим
ключом идемпотентности — так потерянный ответ никогда не превратится во вторую выплату:

```java
PayoutValidation check = oblodai.payouts().validate(new PayoutValidateRequest()
        .amount("10").currency("USDT").network(Network.TRON).address(address));

Payout payout = oblodai.payouts().create(new PayoutRequest()
        .amount("10").currency("USDT").network(Network.TRON).address(address)
        .orderId("payout-42"), RequestOptions.of().idempotencyKey("payout-42"));

System.out.println(check.commission() + " fee, payout " + payout.uuid() + " " + payout.status());
```

Готовые к запуску программы лежат в [`examples/`](examples).

### Суммы

Суммы — десятичные строки от начала и до конца (у USDT 6 знаков, у BTC 8, у ETH 18). Никогда не
разбирайте сумму в `double` и никогда не сравнивайте две строки-суммы через `String.compareTo` —
`"10"` окажется раньше `"9"`.

```java
System.out.println(Money.add("10.000000", "0.5"));      // "10.500000"
System.out.println(Money.compare("25", "25.000000"));   // 0 — equal at any scale
System.out.println(Money.isZero("0.000000"));           // true
System.out.println(Money.toBigDecimal("1.5"));          // when you deliberately want one
```

Всё, что не является обычным десятичным числом (`"1e3"`, пустая строка, 64+ символов), — это
`ConfigException` с кодом `sdk.bad_amount`, и запрос даже не будет отправлен. JSON-число там, где
контракт объявляет строку, тоже отвергается, а не приводится к строке молча.

### Асинхронный клиент

```java
OblodaiAsync client = oblodai.async();                 // same engine, connections and clock
CompletableFuture<Payment> invoice = client.payments().create(request);
CompletableFuture<List<Payout>> all = client.payouts().history().all(500);
```

Любой future падает с `OblodaiException` в качестве причины, а `cancel(true)` на любом возвращённом
SDK future прерывает уже выполняющийся HTTP-обмен и останавливает цикл повторов.

### Kotlin

Java SDK и есть Kotlin SDK: `com.oblodai.kotlin` добавляет корутины и билдеры к тем же самым типам.

```kotlin
val oblodai = Oblodai.builder().publicId(id).secret(secret).build().async()

val invoice = oblodai.payments().create(payment {
    amount("25"); currency("USDT"); network(Network.TRON); orderId("order-1001")
}).await()

oblodai.payments().history().asFlow().take(100).collect { println(it.uuid()) }
```

`await()` бросает собственное исключение SDK, а не обёртку `CompletionException`, и отмена корутины
отменяет вызов. `asFlow()` работает по требованию: следующая страница запрашивается только после
того, как коллектор забрал последний элемент предыдущей, — ничего не теряется и ничего не
запрашивается впустую.

## Песочница и тестирование

Песочница — это копия шлюза без блокчейна: фейковый баланс из крана, симулированные депозиты,
настоящие подписанные вебхуки. Начинайте интеграцию с неё — меняется только ключ.

```java
Oblodai sandbox = Oblodai.builder()
        .publicId(System.getenv("OBLODAI_PUBLIC_ID"))       // test_oblodai_…
        .secret(System.getenv("OBLODAI_SECRET"))            // oblodai_test_…
        .build();

sandbox.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("100"));
sandbox.sandbox().deposit(new SandboxDepositRequest().invoiceId(invoiceId).amount("25"));

sandbox.webhooks().testPayment(new TestWebhookPaymentRequest().uuid(invoiceId).status("paid"));
for (WebhookDelivery delivery : sandbox.sandbox().webhooks()) System.out.println(delivery.status());

sandbox.sandbox().reset();                                  // cancels open invoices, zeroes balances
```

`sandbox().replay(deliveryId)` пере-отправляет доставку, которую вы уже видели. Репетиционные
доставки — из `webhooks().testPayment/testPayout/testWallet(...)` или из песочницы — подписаны
ровно так же, как боевые, и несут `test: true` в подписанном теле и `X-Webhook-Test: true` в
заголовках запроса, поэтому `delivery.isTest()` для них истинно: никогда не реагируйте на такую
доставку так, будто деньги действительно двинулись.

`faucet(...)` и `reset()` требуют выплатного ключа на боевой паре ключей; ключ песочницы сразу обоих
видов, поэтому вызвать их может и он.

## Обзор методов

Шестнадцать неймспейсов покрывают все 107 мерчантских маршрутов снимка контракта.

| Неймспейс        | Методы                                                                                                                                                                                      | Маршрутов |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------: |
| `payments()`     | create · info/get · cancel · history/list · batch · qr · services · sendEmail · resend · publicView · select · publicQr                                                                       |        12 |
| `refunds()`      | create · resolve · batch                                                                                                                                                                     |         3 |
| `payouts()`      | create · validate · calculate · info/get · cancel · approve · history/list · mass · batch · services · get/setFeeConfig · get/setRefundFeeConfig                                              |        14 |
| `payoutLinks()`  | create · info/get · list · cancel · batch · cheque · claimPreview · claim                                                                                                                     |         8 |
| `paymentLinks()` | create · info/get · list · toggle · publicView · checkout                                                                                                                                    |         6 |
| `batches()`      | info (опрос асинхронного батча)                                                                                                                                                              |         1 |
| `transfers()`    | toPersonal · toUser · batch                                                                                                                                                                  |         3 |
| `wallets()`      | create · qr · block · refundBlockedDeposit                                                                                                                                                   |         4 |
| `webhooks()`     | register · rotateSecret · deliveries · testPayment/testPayout/testWallet · test(kind, …)                                                                                                      |         7 |
| `documents()`    | statement · ledger · balanceCertificate · feeSchedule · splitReport · batchReport · linkReport · walletStatement · referralsReport · createJob · jobInfo · jobFile · download                  |        13 |
| `splits()`       | createRule · listRules · deleteRule · get/setConfig · get/setOptIn                                                                                                                            |         7 |
| `settings()`     | setDiscount · listDiscounts · get/setAccuracy · get/setAutoRefund · listAccepted · setAccepted · get/setPaymentFeeConfig · list/set/deleteAutoWithdraw · list/add/remove/enableApiAllowlist    |        17 |
| `account()`      | balance · referral · vrcs                                                                                                                                                                    |         3 |
| `catalog()`      | currencies · exchangeRates                                                                                                                                                                   |         2 |
| `sandbox()`      | faucet · deposit · webhooks · replay · reset                                                                                                                                                 |         5 |
| `merchants()`    | create · createSandbox (онбординг; без подписи, `adminToken(...)` на self-hosted шлюзе)                                                                                                       |         2 |

Методы получения одного объекта принимают либо голый id, либо объект запроса:
`payments().info(uuid)`, `payments().info(new PaymentInfoRequest().orderId("order-1001"))`.
Синхронные батчи ограничены по числу элементов — `payouts().mass(...)` до 100, а
`payoutLinks().batch(...)` до 500 — и отчитываются по каждому элементу отдельно, так что и в ответе
200 могут быть неудачи. Асинхронные батчи (`payments().batch`, `payouts().batch`, `refunds().batch`,
`transfers().batch`) принимают до 5000 элементов и опрашиваются через `batches().info(...)`.
Документы приходят как `FileResult` (`bytes`, `contentType`, `filename`). Маршруты для плательщика —
`payments().publicView/select/publicQr`, `paymentLinks().publicView/checkout`,
`payoutLinks().claimPreview/claim` — не требуют учётных данных.

### Списки

Методы-списки возвращают `Pager<T>`. Пока вы не начали читать, ни одного запроса не уходит, а обход
идёт по собственному флагу шлюза `paginate.has_pages`.

```java
Page<Payment> page = oblodai.payments().history(new PaymentHistoryRequest().limit(50)).firstPage();

for (Payout payout : oblodai.payouts().history(new PayoutHistoryRequest().status("confirmed"))) {
    process(payout);                                    // one page fetched at a time
}

List<Payout> refunds = oblodai.payouts().history(new PayoutHistoryRequest().kind("refund")).all(1000);
Stream<Payment> stream = oblodai.payments().history().stream();
```

### Статусы

- Инвойс: `select → created → confirm_check → paid | paid_over | wrong_amount | expired | cancelled`.
  `Statuses.isPaymentPaid(...)` покрывает `paid`/`paid_over`; `wrong_amount` (недоплата) ждёт
  `refunds().resolve(...)`; `Statuses.isPaymentFinal(...)` покрывает остальные.
- Выплата: `pending → approved → awaiting_cosign → broadcasting → sent → confirmed | failed | cancelled`.

Об изменениях состояния лучше узнавать из вебхуков; опрос `info` — только запасной вариант.

Словари (`PaymentStatus`, `PayoutStatus`, `Network`, …) **открытые**: значения, известные этому
снимку, — интернированные константы, которые можно сравнивать через `==`, а значение, которое шлюз
начал присылать и о котором этот SDK никогда не слышал, приходит объектом, несущим ровно эту строку:
`status.wire()` — то, что сказал шлюз, `status.isKnown()` — признак новизны. Это не Java-энумы,
поэтому `switch` по ним нет, как нет и константы `UNKNOWN`. Тип события новее этого SDK приходит так
же — как `UnknownEvent` с исходным `type`. Шлюз, расширяющий свой словарь, не может ни сломать
задеплоенного клиента, ни заставить его выбросить сказанное.

## Вебхуки

Зарегистрируйте эндпоинт через `webhooks().register(url)`; в ответе придёт секрет подписи, показанный
один-единственный раз. Дальше проверяйте каждую доставку по **сырым** байтам запроса — фреймворк,
который разобрал и заново сериализовал тело, уже изменил их, и подпись не сойдётся.

```java
byte[] rawBody = exchange.getRequestBody().readAllBytes();   // the RAW bytes, always

WebhookDeliveryInfo delivery = WebhookVerifier.verifyDelivery(
        rawBody,
        WebhookHeaders.ofMulti(exchange.getRequestHeaders()), // or of(Map), or a lambda
        WebhookVerifier.options(secret).previousSecret(previousSecret));

if (delivery.isTest()) return;                               // a rehearsal: no money moved
if (!seen.add(delivery.id())) return;                        // X-Webhook-Id, stable across retries
if (delivery.event() instanceof PaymentEvent payment && payment.status() == PaymentStatus.PAID) {
    markOrderPaid(payment.orderId());
}
```

Проверки идут в порядке заголовки → MAC → свежесть → тело, поэтому окно свежести никогда не работает
оракулом для неаутентифицированного вызывающего. Секрет, равный null или пустой строке, и
отрицательный допуск — это `ConfigException` до любой криптографии (`Duration.ZERO` осознанно
отключает проверку свежести, окна меньше секунды учитываются). Дедуплицируйте по `delivery.id()`;
отбрасывайте доставки, пришедшие не по порядку, через `WebhookVerifier.isStale(event, lastSequence)`.
После `webhooks().rotateSecret()` храните предыдущий секрет минимум 26 часов: доставки, поставленные
в очередь до ротации, остаются подписанными им всю свою жизнь в повторах. Проверка подписи — это
самостоятельный класс: без клиента, без API-ключа, без сети.

Тип события, неизвестный этому снимку, декодируется в `UnknownEvent`, сохраняющий исходную строку;
отличить их помогает `WebhookVerifier.isKnownEvent(event)`. То, что отвечает ваш приёмник, важно:
всё, что не 2xx, шлюз повторит.

```java
WebhookEvent event;
try {
    event = WebhookVerifier.verify(rawBody, headers, options);
} catch (SignatureException notFromTheGateway) {
    return 401;                       // the ONLY failure that deserves a 401
} catch (WebhookPayloadException unreadable) {
    return 400;                       // authentic delivery, body is not an event: webhook.bad_payload
}
if (!WebhookVerifier.isKnownEvent(event)) return 200;   // newer than this SDK: acknowledge, ignore
```

## Ошибки

Любая неудача — это `OblodaiException`, несущий конверт ошибки от API: `code()`
(`payout.insufficient_funds`), `httpStatus()`, `retryable()`, `retryAfter()`, `requestId()`,
`field()` (для 400) и `synthetic()` (ответил прокси, а не шлюз). В обращении в поддержку указывайте
`requestId()`.

| Класс                                             | HTTP        | Когда                                                        |
| ------------------------------------------------- | ----------- | ------------------------------------------------------------ |
| `ValidationException`                             | 400         | запрос некорректен или поле отвергнуто                        |
| `AuthenticationException`                         | 401         | неверная подпись, неверная метка времени, неизвестный public id |
| `PermissionException`                             | 403         | не тот вид ключа, IP не в allow-list                          |
| `NotFoundException`                               | 404         | объекта нет                                                   |
| `ConflictException` / `IdempotencyConflictException` | 409       | конфликт состояния; ключ переиспользован с другим телом        |
| `RateLimitException`                              | 429         | сработал лимит — соблюдайте `retryAfter()`                     |
| `UnavailableException`                            | 503         | шлюз временно недоступен                                      |
| `InternalException`                               | прочие 5xx  | сбой на стороне шлюза                                         |
| `TransportException`                              | ответа нет  | сбой соединения, таймаут, дедлайн                              |
| `ConfigException`                                 | не отправлен | `sdk.bad_config`, `sdk.missing_credentials`, `sdk.bad_idempotency_key`, `sdk.idempotency_unsupported`, `sdk.bad_path_param`, `sdk.bad_header`, `sdk.bad_amount` |
| `ContractException`                               | нечитаемо   | `sdk.bad_envelope`, `sdk.response_too_large`                  |
| `WebhookPayloadException`                         | —           | `webhook.bad_payload`: доставка подлинная, но это не событие   |
| `SignatureException`                              | —           | не прошла проверка подписи или свежести вебхука                |

Код имеет вид `family.reason`, и снимок контракта несёт все 471 из них — `ErrorCodes.ALL`, а для
одного `ErrorCodes.isKnown(code)`. Ветвитесь по коду, а не по сообщению:

```java
try {
    oblodai.payouts().create(request);
} catch (OblodaiException e) {
    switch (e.code()) {
        case "payout.insufficient_funds", "payout.funds_maturing" ->
                scheduleRetry(e.retryAfter() == null ? 60 : e.retryAfter());
        default -> throw e;   // the SDK already retried what was safe to retry
    }
}
```

Коды, которые стоит обрабатывать по имени: `payout.insufficient_funds` и `payout.funds_maturing`
(оба retryable), `idempotency.key_reused`, `invoice.not_payable`, `payment.not_found`,
`merchant.wrong_key_kind`, `merchant.bad_signature`, `request.rate_limited`.

`toString()` и `details()` никогда не включают сырое тело ответа, поэтому лог не может пролить
содержимое инвойса или пароль чека; `raw()` есть для осознанного разбора.

## Ретраи, идемпотентность и таймауты

- Ошибка повторяется только тогда, когда API сказал `retryable`. Ответы без конверта API (502/503 от
  прокси) и транспортные сбои повторяются только на читающих маршрутах и на записях с ключом
  идемпотентности. «Читающий маршрут» — это не догадка по виду пути: это флаг `safe`, который шлюз
  объявляет для каждого маршрута в снимке контракта. `Retry-After` важнее вычисленного бэкоффа и
  ограничен `maxRetryAfterMs`.
- Создающие маршруты автоматически получают `Idempotency-Key` — один на логический вызов и тот же на
  каждом повторе, — поэтому таймаут не может породить вторую выплату. Передайте свой ключ, чтобы
  повторы были безопасны и после перезапуска процесса. На маршрутах, которые шлюз не дедуплицирует,
  SDK **отказывается** принимать ключ (`sdk.idempotency_unsupported`) — включая методы-списки, где
  отказ происходит при создании пейджера, а не молчаливым отбрасыванием: заголовок всё равно был бы
  проигнорирован, и только SDK считал бы повторную отправку безопасной.
- У каждого метода есть необязательный последний аргумент `RequestOptions` — у каждого, включая
  алиасы (`get`, `list`) и формы списков без аргументов:

```java
oblodai.payouts().create(request, RequestOptions.of()
        .idempotencyKey(orderId)                  // your own key; generated for you when omitted
        .timeout(Duration.ofSeconds(10))          // per attempt
        .deadline(Duration.ofSeconds(45))         // whole call, retries and pauses included
        .header("X-Tenant", "acme")               // one call only, on top of the client-wide headers
        .preferPayoutKey(true));                  // sign with the payout key on an either-kind route
```

- Получив 401, означающий неверную подпись или метку времени, SDK читает заголовок `Date` сервера,
  переподписывает запрос один раз и сохраняет смещение только в том случае, если эта попытка прошла
  аутентификацию. Смещение общее для всех вызовов клиента и правится атомарно, поэтому параллельные
  вызовы на хосте с уехавшими часами сходятся к одной поправке, а не отменяют правки друг друга.
- Редиректы не выполняются никогда; если внедрённый `HttpClient` всё же перешёл по редиректу, SDK
  заметит, что ответ пришёл с другого URL, и завалит вызов.
- Тело ответа читается под потолком — 8 МиБ для JSON-конверта и 64 МиБ для документа, — и ответ
  больше этого валит вызов с `sdk.response_too_large`, а не процесс.

## Конфигурация

```java
Oblodai oblodai = Oblodai.builder()
        .publicId(id).secret(secret)
        .payoutKey(payoutId, payoutSecret)
        .baseUrl("https://api.oblodai.com")
        .timeout(Duration.ofSeconds(30))
        .deadline(Duration.ofSeconds(90))
        .retry(new RetryOptions(2, 250, 4_000, 30_000))
        .header("X-Tenant", "acme")
        .logger(Logger.console(Logger.Level.INFO))
        .build();
```

| Опция билдера                        | По умолчанию                                | Что делает                                                       |
| ------------------------------------ | ------------------------------------------- | ---------------------------------------------------------------- |
| `publicId(…)` / `secret(…)`          | `OBLODAI_PUBLIC_ID` / `OBLODAI_SECRET`      | платёжная пара ключей; секрет подписывает и никогда не отправляется |
| `payoutKey(publicId, secret)`        | `OBLODAI_PAYOUT_PUBLIC_ID` / `…_SECRET`     | выплатная пара ключей, выбирается автоматически по маршруту       |
| `adminToken(…)`                      | `OBLODAI_ADMIN_TOKEN`                       | `X-Admin-Token`, уходит только на маршруты онбординга мерчантов   |
| `baseUrl(…)`                         | `OBLODAI_BASE_URL`, иначе `https://api.oblodai.com` | адрес API; префикс пути сохраняется                       |
| `allowInsecureBaseUrl(boolean)`      | `OBLODAI_ALLOW_INSECURE=1`, иначе `false`   | разрешает http-адрес вне loopback                                 |
| `timeout(Duration)`                  | 30 с                                        | на одну попытку                                                   |
| `deadline(Duration)`                 | 90 с                                        | на весь вызов, включая повторы и паузы                            |
| `retry(RetryOptions)`                | 2 повтора, база 250 мс, потолок 4 с, лимит `Retry-After` 30 с | `RetryOptions.none()` отключает повторы          |
| `header(name, value)`                | —                                           | заголовок на каждый запрос; имена, принадлежащие SDK, отвергаются  |
| `logger(Logger)`                     | `OBLODAI_LOG`, иначе тишина                 | структурные логи; поля приходят уже отредактированными            |
| `objectMapper(ObjectMapper)`         | собственная конфигурация SDK                | маппер JSON для разбора ответов                                   |
| `httpClient(HttpClient)`             | клиент, который SDK создаёт и закрывает сам | контроль над прокси, TLS, исполнителями                           |
| `clock(SkewCorrectingClock)`         | часы, обучающиеся времени шлюза             | часы для подписи                                                  |
| `environment(Map<String,String>)`    | `System.getenv()`                           | подменяет окружение, из которого читаются фолбэки, для тестов      |

| Переменная окружения         | Действие                                                       |
| ---------------------------- | -------------------------------------------------------------- |
| `OBLODAI_PUBLIC_ID`          | public id платёжного ключа                                      |
| `OBLODAI_SECRET`             | секрет платёжного ключа                                         |
| `OBLODAI_PAYOUT_PUBLIC_ID`   | public id выплатного ключа                                      |
| `OBLODAI_PAYOUT_SECRET`      | секрет выплатного ключа                                         |
| `OBLODAI_ADMIN_TOKEN`        | админ-токен self-hosted шлюза                                   |
| `OBLODAI_BASE_URL`           | адрес API                                                       |
| `OBLODAI_LOG`                | `debug` \| `info` \| `warn` \| `error` — лог в stderr           |
| `OBLODAI_ALLOW_INSECURE`     | `1` разрешает http-адрес                                        |

Явно заданная опция побеждает; иначе читается окружение, а затем берётся значение по умолчанию.

**Редактирование секретов.** Транспорт вычищает чувствительные значения *до* того, как поля попадут
в логгер, поэтому внедрённый логгер никогда не видит ни ключа, ни подписи, ни пароля от чека — это
не то, о чём должна помнить реализация логгера. То же и с моделями, несущими показанное однажды
значение: `WebhookEndpoint.secret`, `WebhookSecretRotated.secret`, `ApiKeyPair.secret`,
`PayoutLink.claimToken`/`claimUrl`/`passcode` читаются через свои аксессоры и превращаются в
`[redacted]` в `toString()` и в JSON.

**Self-hosted или локальный шлюз.** `baseUrl("http://127.0.0.1:8095")` работает сразу; для прочих
http-хостов нужен `allowInsecureBaseUrl(true)`. Префикс пути в `baseUrl` сохраняется
(`https://gw.corp/oblodai` → `https://gw.corp/oblodai/v1/payment`).

```java
Oblodai.builder().baseUrl("http://127.0.0.1:8095").build();
Oblodai.builder().baseUrl("http://gw.corp").allowInsecureBaseUrl(true).build();
```

`header(...)` отвергает имена, принадлежащие SDK (`Accept`, `Content-Type`, `User-Agent`,
`X-Public-Id`, `X-Signature`, `X-Timestamp`, `Idempotency-Key`, `X-Admin-Token`), с ошибкой
`sdk.bad_header` — так админ-токен нельзя прицепить ко всем запросам подряд.

Клиент реализует `AutoCloseable`: `close()` освобождает HTTP-клиент, который он создал для себя (на
JDK 17 это no-op, там у `HttpClient` нет операции закрытия). Один клиент на пару ключей, живущий
столько же, сколько процесс, — это задуманная форма использования.

## Контрактный снимок

`contract/` выгружается собственным набором тестов шлюза из коммита ядра `7ec04293c426`: реестр
маршрутов (107 мерчантских маршрутов, у каждого метод, путь, вид ключа, `idempotent`, `safe`, `bare`
и форма списка), схемы DTO запросов с английскими описаниями полей, энумы, все 471 код ошибок,
векторы подписи, эталонные тела ответов, записанные с живого шлюза, и настоящие подписанные доставки
вебхуков.

`src/main/java/com/oblodai/contract/` генерируется из него скриптом `codegen/run.sh` — 92 файла, 107
маршрутов, 471 код ошибок, 76 типов запросов. `codegen/run.sh --check` — это дрейф-гейт: он валит
сборку, когда закоммиченные исходники и снимок расходятся, и `mvn verify` запускает его первым.
Дальше контрактные тесты сверяют каждую модель с эталонными телами, а каждый маршрут — с реестром.

Чтобы обновить: замените `contract/`, выполните `codegen/run.sh`, прогоните `mvn -o verify` и
поправьте числа, приведённые здесь и в [AGENTS.md](AGENTS.md).

## Разработка

```bash
git clone https://github.com/oblodai/oblodai-java && cd oblodai-java
mvn -o verify                                          # drift gate + compile + 518 offline tests + jars
mvn -o test                                            # tests only
codegen/run.sh                                         # regenerate after refreshing contract/
codegen/run.sh --check                                 # what the drift gate runs
OBLODAI_LIVE_URL=http://127.0.0.1:8095 mvn -o verify   # adds the 18 live tests
mvn -Prelease deploy                                   # publish to Maven Central; see RELEASING.md
```

Живой ярус заводит мерчанта на тестируемом шлюзе, берёт ключ песочницы и проходит весь денежный путь,
включая настоящий подписанный вебхук, доставленный в приёмник, который поднимает сам тест. Без
`OBLODAI_LIVE_URL` эти тесты пропускаются.

Каждый сниппет из этого файла и из [README.md](README.md) компилируется в тестовой сборке
(`examples/java/com/oblodai/examples/DocSnippets.java`,
`examples/kotlin/com/oblodai/examples/DocSnippetsKotlin.kt`), а `DocSnippetsTest` проверяет, что оба
README несут одинаковый код и что каждый сниппет действительно там есть.

Пишете код с ИИ-агентом? Дайте ему [AGENTS.md](AGENTS.md). См. также [CHANGELOG.md](CHANGELOG.md) и
[MIGRATION-1.3.md](MIGRATION-1.3.md).

## Лицензия

MIT — см. [LICENSE](LICENSE).
