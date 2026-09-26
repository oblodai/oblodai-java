package com.oblodai.examples;

import com.oblodai.ApiResponse;
import com.oblodai.Money;
import com.oblodai.Oblodai;
import com.oblodai.RequestOptions;
import com.oblodai.Statuses;
import com.oblodai.core.FileResult;
import com.oblodai.core.Job;
import com.oblodai.core.Page;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.SignatureException;
import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.BatchSubmitResponse;
import com.oblodai.generated.models.DocumentJobAccepted;
import com.oblodai.generated.models.DocumentJobKind;
import com.oblodai.generated.models.DocumentJobRequest;
import com.oblodai.generated.models.LookupRequest;
import com.oblodai.generated.models.PaymentHistoryRequest;
import com.oblodai.generated.models.PaymentInfoResult;
import com.oblodai.generated.models.PaymentRequest;
import com.oblodai.generated.models.PaymentView;
import com.oblodai.generated.models.PayoutBatchRequest;
import com.oblodai.generated.models.PayoutItem;
import com.oblodai.generated.models.PayoutRequest;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Every Java block of README.md and README.ru.md, as code that compiles and runs.
 *
 * <p>Each method opens with one README block, statement for statement; anything after it only hands
 * a value back. {@code ReadmeExamplesTest} fails when a README block stops appearing here, and runs
 * every method against a scripted gateway - a renamed method, argument or field breaks the build,
 * not the reader.
 */
public final class ReadmeSnippets {

    private ReadmeSnippets() {}

    /**
     * README: "Configure the client".
     *
     * @return the client
     */
    public static Oblodai configure() {
        Oblodai oblodai = Oblodai.builder()
                .publicId(System.getenv("OBLODAI_PUBLIC_ID"))
                .secret(System.getenv("OBLODAI_SECRET"))
                .build();
        return oblodai;
    }

    /**
     * README: "Accept a payment".
     *
     * @param oblodai the client
     * @return the invoice
     */
    public static PaymentView acceptPayment(Oblodai oblodai) {
        PaymentView invoice = oblodai.payments().create(PaymentRequest.builder()
                .amount("25")                  // a decimal string or a BigDecimal, never a double
                .currency("USDT")
                .orderId("order-1001")         // your reference; the invoice is idempotent per order
                .urlCallback("https://shop.example/oblodai/webhook")
                .build());

        System.out.println(invoice.url());     // send the payer here
        return invoice;
    }

    /**
     * README: "Read it back".
     *
     * @param oblodai the client
     * @param invoice the invoice
     * @return its state
     */
    public static PaymentInfoResult readBack(Oblodai oblodai, PaymentView invoice) {
        PaymentInfoResult now = oblodai.payments().getInfo(
                LookupRequest.builder().uuid(invoice.uuid()).build());

        if (Statuses.isPaymentPaid(now.status())) {
            BigDecimal received = Money.toBigDecimal(now.merchantAmount());   // exact, never a double
            System.out.println("paid, " + received + " credited");
        }
        return now;
    }

    /**
     * README: "Send a payout".
     *
     * @param oblodai the client
     * @return the payout
     */
    public static PayoutItem sendPayout(Oblodai oblodai) {
        PayoutItem payout = oblodai.payouts().create(
                PayoutRequest.builder()
                        .amount(new BigDecimal("10.50"))
                        .currency("USDT")
                        .network("tron")
                        .address("TXYZ")
                        .orderId("withdrawal-77")
                        .build(),
                RequestOptions.of().idempotencyKey("withdrawal-77"));   // one key for every retry
        return payout;
    }

    /**
     * README: "Per-call options".
     *
     * @param oblodai the client
     * @return the copy with other defaults
     */
    public static Oblodai options(Oblodai oblodai) {
        oblodai.account().getBalance(RequestOptions.of()
                .timeout(Duration.ofSeconds(5))       // per attempt
                .maxRetries(0)                        // this call only
                .extraHeader("X-Trace", "t-42")
                .requestId("order-1001-balance"));    // sent as X-Request-ID

        Oblodai patient = oblodai.withOptions(RequestOptions.of().timeout(Duration.ofSeconds(60)));
        return patient;
    }

    /**
     * README: "Lists".
     *
     * @param oblodai the client
     * @return how many items the walk saw
     */
    public static int lists(Oblodai oblodai) {
        int seen = 0;
        for (PaymentView p : oblodai.payments().listHistory()) {            // every item, page by page
            System.out.println(p.orderId() + " " + p.status());
            seen++;
        }
        for (Page<PaymentView> page : oblodai.payments().listHistory(
                PaymentHistoryRequest.builder().limit(100L).build()).byPage()) {  // one request per page
            System.out.println(page.items().size() + " of " + page.total());
        }
        return seen;
    }

    /**
     * README: "Errors".
     *
     * @param oblodai the client
     * @return the error's code, or null
     */
    public static String errors(Oblodai oblodai) {
        String code = null;
        try {
            oblodai.payments().getInfo(LookupRequest.builder().uuid("missing").build());
        } catch (OblodaiException e) {
            System.err.println(e.getMessage());   // [payment.not_found] … (request_id=…)
            code = e.code();                      // retryable(), field(), requestId() as well
        }
        return code;
    }

    /**
     * README: "Raw responses and hooks".
     *
     * @param oblodai the client
     * @return the raw answer
     */
    public static ApiResponse<PaymentView> raw(Oblodai oblodai) {
        ApiResponse<PaymentView> raw = oblodai.withRawResponse(c -> c.payments().create(
                PaymentRequest.builder().amount("25").currency("USDT").build()));
        System.out.println(raw.status() + " " + raw.requestId() + " " + raw.value().uuid());

        Oblodai observed = Oblodai.builder()
                .onRequest(r -> System.out.println("-> " + r.operationId() + " #" + r.attempt()))
                .onResponse(r -> System.out.println("<- " + r.status() + " in " + r.elapsed()))
                .build();
        observed.close();
        return raw;
    }

    /**
     * README: "Long-running operations".
     *
     * @param oblodai the client
     * @return the export
     */
    public static FileResult jobs(Oblodai oblodai) {
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
        return file;
    }

    /**
     * README: "Webhooks".
     *
     * @param rawBody the exact request bytes
     * @param headers the request headers
     * @param secret the endpoint secret
     * @return the HTTP status to answer with
     */
    public static int webhooks(byte[] rawBody, Map<String, String> headers, String secret) {
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
    }

    /**
     * README: "Asynchronous client".
     *
     * @param oblodai the client
     * @return the invoice id, when it arrives
     */
    public static String async(Oblodai oblodai) {
        String uuid = oblodai.async().payments()
                .create(PaymentRequest.builder().amount("25").currency("USDT").build())
                .thenApply(PaymentView::uuid)
                .join();
        return uuid;
    }
}
