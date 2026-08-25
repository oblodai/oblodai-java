package com.oblodai.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.Oblodai;
import com.oblodai.contract.AmountMode;
import com.oblodai.contract.Network;
import com.oblodai.contract.PaymentStatus;
import com.oblodai.contract.PayoutLinkStatus;
import com.oblodai.contract.requests.*;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.NotFoundException;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.ValidationException;
import com.oblodai.models.*;
import com.oblodai.resources.DocumentQuery;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Every namespace against a REAL gateway at {@code OBLODAI_LIVE_URL}. The point is not the business
 * outcome but that the bodies the SDK sends are accepted (no 400 from our own shapes) and the bodies
 * that come back decode (no {@link ContractException}). Routes needing a subsystem the stand may
 * lack (documents, email) are probed and skipped when the gateway reports them disabled.
 */
@EnabledIfEnvironmentVariable(named = "OBLODAI_LIVE_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveSweepTest {

    private static final String ADDRESS = "TQrY8bkbpXKPt2LZbU8jqfnpFbUSF15sbx";

    private Oblodai oblodai;
    private Oblodai anonymous;
    private Payment invoice;
    private PayoutLink cheque;
    private boolean documentsEnabled = true;

    /** Fails on SDK-side shape problems; tolerates a business refusal (403/404/409). */
    private static <T> T accept(Supplier<T> call) {
        try {
            return call.get();
        } catch (ContractException | ValidationException fatal) {
            throw fatal;
        } catch (OblodaiException refusal) {
            return null;
        }
    }

    private static String reference(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    @BeforeAll
    void openAStore() {
        oblodai = Live.sandboxClient("sweep");
        anonymous = Live.publicClient();
        oblodai.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("1000"));
        // A per-invoice url_callback needs a registered endpoint: the gateway signs with its secret.
        oblodai.webhooks().register(Live.hookUrl());
        invoice =
                oblodai
                        .payments()
                        .create(
                                new PaymentRequest()
                                        .amount("25")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId(reference("sw"))
                                        .payerEmail("buyer@example.com")
                                        .urlCallback(Live.hookUrl()));
        try {
            oblodai.documents().balanceCertificate();
        } catch (NotFoundException e) {
            if ("document.disabled".equals(e.code())) documentsEnabled = false;
        } catch (OblodaiException e) {
            documentsEnabled = false;
        }
    }

    @Test
    @Order(1)
    void catalogAndAccount() {
        assertFalse(anonymous.catalog().currencies().currencies().isEmpty());
        assertNotNull(
                anonymous
                        .catalog()
                        .exchangeRates(new ExchangeRateListRequest().currencyFrom("BTC"))
                        .firstPage()
                        .items());
        assertNotNull(oblodai.account().balance().balance().merchant());
        assertNotNull(oblodai.account().referral().code());
        assertNotNull(oblodai.account().vrcs().enabled());
        assertNotNull(oblodai.account().vrcs(false).enabled());
    }

    @Test
    @Order(2)
    void paymentsLookupsQrServicesPublicCheckoutAndBatch() {
        assertEquals(invoice.uuid(), oblodai.payments().get(invoice.uuid()).uuid());
        // A sandbox invoice carries a synthetic address, which the gateway deliberately does not
        // render into a QR: the fields come back empty rather than absent.
        assertNotNull(oblodai.payments().qr(invoice.uuid()).image());
        assertFalse(
                oblodai.payments().services(new PaymentServicesRequest().limit(5)).firstPage().items().isEmpty());
        assertEquals(PaymentStatus.CREATED, anonymous.payments().publicView(invoice.uuid()).status());
        assertNotNull(anonymous.payments().publicQr(invoice.uuid()).image());

        Payment multi =
                oblodai
                        .payments()
                        .create(new PaymentRequest().amount("10").currency("USDT").orderId(reference("sw-multi")));
        assertEquals(
                Network.TRON,
                anonymous
                        .payments()
                        .select(multi.uuid(), new PayIdSelectRequest().currency("USDT").network(Network.TRON))
                        .network());

        accept(() -> oblodai.payments().resend(invoice.uuid()));
        accept(() -> oblodai.payments().sendEmail(new PaymentSendEmailRequest().uuid(invoice.uuid())));

        BatchSubmitted batch =
                oblodai
                        .payments()
                        .batch(
                                new PaymentBatchRequest()
                                        .onError("continue")
                                        .payments(
                                                List.of(
                                                        new PaymentBatchRequest.Payment()
                                                                .amount("3")
                                                                .currency("USDT")
                                                                .network(Network.TRON)
                                                                .orderId(reference("sw-b")))));
        assertNotNull(batch.batchId());
        assertEquals(
                batch.batchId(),
                oblodai.batches().info(new BatchInfoRequest().batchId(batch.batchId())).batchId());

        Payment toCancel =
                oblodai
                        .payments()
                        .create(
                                new PaymentRequest()
                                        .amount("1")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .orderId(reference("sw-c")));
        assertEquals(PaymentStatus.CANCELLED, oblodai.payments().cancel(toCancel.uuid()).status());

        for (Payment page : oblodai.payments().history(new PaymentHistoryRequest().limit(2)).all(4)) {
            assertNotNull(page.uuid());
        }
    }

    @Test
    @Order(3)
    void depositThenRefundResolveAndRefundBatch() {
        oblodai
                .sandbox()
                .deposit(
                        new SandboxDepositRequest()
                                .invoiceId(invoice.uuid())
                                .amount("25")
                                .confirmations(20)
                                .txid(reference("sw-tx")));
        PaymentStatus status = oblodai.payments().get(invoice.uuid()).status();
        assertTrue(
                status == PaymentStatus.PAID || status == PaymentStatus.CONFIRM_CHECK,
                "a confirmed deposit credits the invoice, got " + status);

        accept(
                () ->
                        oblodai
                                .refunds()
                                .create(
                                        new PaymentRefundRequest()
                                                .uuid(invoice.uuid())
                                                .address(ADDRESS)
                                                .amount("5")
                                                .reference(reference("sw-r"))));
        accept(
                () ->
                        oblodai
                                .refunds()
                                .resolve(new PaymentResolveRequest().uuid(invoice.uuid()).action("accept")));
        accept(
                () ->
                        oblodai
                                .refunds()
                                .batch(
                                        new RefundBatchRequest()
                                                .refunds(
                                                        List.of(
                                                                new RefundBatchRequest.Refund()
                                                                        .uuid(invoice.uuid())
                                                                        .address(ADDRESS)
                                                                        .amount("1")
                                                                        .reference(reference("sw-rb"))))));
    }

    @Test
    @Order(4)
    void payoutsCalculateValidateCreateCancelMassBatchServicesAndFeeConfigs() {
        assertEquals(
                "USDT",
                oblodai
                        .payouts()
                        .calculate(
                                new PayoutCalculateRequest().amount("10").currency("USDT").network(Network.TRON))
                        .currency());
        assertTrue(
                oblodai
                        .payouts()
                        .validate(
                                new PayoutValidateRequest()
                                        .amount("10")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .address(ADDRESS))
                        .valid());

        Payout payout =
                oblodai
                        .payouts()
                        .create(
                                new PayoutRequest()
                                        .amount("10")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .address(ADDRESS)
                                        .orderId(reference("sw-po")));
        assertEquals(
                payout.uuid(),
                oblodai.payouts().get(new PayoutInfoRequest().orderId(payout.orderId())).uuid());
        accept(() -> oblodai.payouts().cancel(payout.uuid()));
        accept(() -> oblodai.payouts().approve(payout.uuid()));

        List<BatchElement<Payout>> mass =
                oblodai
                        .payouts()
                        .mass(
                                new PayoutMassRequest()
                                        .payouts(
                                                List.of(
                                                        new PayoutMassRequest.Payout()
                                                                .amount("1")
                                                                .currency("USDT")
                                                                .network(Network.TRON)
                                                                .address(ADDRESS)
                                                                .orderId(reference("sw-m")))));
        assertEquals(0, mass.get(0).idx());

        assertNotNull(
                oblodai
                        .payouts()
                        .batch(
                                new PayoutBatchRequest()
                                        .payouts(
                                                List.of(
                                                        new PayoutBatchRequest.Payout()
                                                                .amount("1")
                                                                .currency("USDT")
                                                                .network(Network.TRON)
                                                                .address(ADDRESS)
                                                                .orderId(reference("sw-pb")))))
                        .batchId());
        assertFalse(oblodai.payouts().services().firstPage().items().isEmpty());
        assertNotNull(
                oblodai.payouts().setFeeConfig(new PayoutFeeConfigSetRequest().feeOnRecipient(true)).feeOnRecipient());
        assertNotNull(oblodai.payouts().getFeeConfig().feeOnRecipient());
        assertNotNull(
                oblodai
                        .payouts()
                        .setRefundFeeConfig(new PayoutRefundFeeConfigSetRequest().feeOnCustomer(true))
                        .feeOnCustomer());
        assertNotNull(oblodai.payouts().getRefundFeeConfig().feeOnCustomer());
        assertNotNull(
                oblodai.payouts().history(new PayoutHistoryRequest().kind("refund").limit(5)).firstPage().items());
    }

    @Test
    @Order(5)
    void payoutLinksCreateInfoListClaimCancelAndBatch() {
        cheque =
                oblodai
                        .payoutLinks()
                        .create(
                                new PayoutLinkRequest()
                                        .amount("5")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .reference(reference("sw-pl"))
                                        .title("Bonus")
                                        .expiresInSeconds(3600));
        assertNotNull(cheque.claimToken(), "the claim token is returned once, on create");
        assertEquals(PayoutLinkStatus.FUNDED, oblodai.payoutLinks().get(cheque.linkId()).status());
        assertFalse(
                oblodai.payoutLinks().list(new PayoutLinkListRequest().limit(5)).firstPage().items().isEmpty());
        assertTrue(anonymous.payoutLinks().claimPreview(cheque.claimToken()).claimable());
        assertNotNull(
                anonymous
                        .payoutLinks()
                        .claim(cheque.claimToken(), new ClaimTokenRequest().address(ADDRESS))
                        .payoutId());

        PayoutLink second =
                oblodai
                        .payoutLinks()
                        .create(
                                new PayoutLinkRequest()
                                        .amount("1")
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .reference(reference("sw-pl2")));
        assertEquals(
                PayoutLinkStatus.CANCELLED, oblodai.payoutLinks().cancel(second.linkId()).status());

        List<BatchElement<PayoutLink>> batch =
                oblodai
                        .payoutLinks()
                        .batch(
                                new PayoutLinkBatchRequest()
                                        .items(
                                                List.of(
                                                        new PayoutLinkBatchRequest.Item()
                                                                .amount("1")
                                                                .currency("USDT")
                                                                .network(Network.TRON)
                                                                .reference(reference("sw-plb")))));
        assertTrue(batch.get(0).ok());
    }

    @Test
    @Order(6)
    void paymentLinksCreateInfoListToggleAndPublicCheckout() {
        PaymentLinkCreated created =
                oblodai
                        .paymentLinks()
                        .create(
                                new PaymentLinkRequest()
                                        .title("Tip")
                                        .amountMode(AmountMode.FIXED)
                                        .currency("USDT")
                                        .amountFixed("10")
                                        .pinnedNetwork(Network.TRON));
        assertNotNull(created.linkId());
        assertTrue(oblodai.paymentLinks().get(created.linkId()).active());
        assertFalse(oblodai.paymentLinks().list().firstPage().items().isEmpty());
        assertEquals(
                AmountMode.FIXED, anonymous.paymentLinks().publicView(created.linkId()).amountMode());
        assertNotNull(
                anonymous
                        .paymentLinks()
                        .checkout(
                                created.linkId(),
                                new LinkIdCheckoutRequest().currency("USDT").network(Network.TRON))
                        .uuid());
        assertFalse(oblodai.paymentLinks().toggle(created.linkId(), false).active());
    }

    @Test
    @Order(7)
    void splitsAndSettings() {
        SplitRule rule =
                oblodai
                        .splits()
                        .createRule(
                                new SplitRuleRequest()
                                        .percent("10")
                                        .address(ADDRESS)
                                        .network(Network.TRON)
                                        .note("partner"));
        assertTrue(
                oblodai.splits().listRules().all(50).stream()
                        .anyMatch(r -> r.ruleId().equals(rule.ruleId())));
        assertEquals(
                3600,
                oblodai.splits().setConfig(new SplitConfigSetRequest().refundHoldSeconds(3600)).refundHoldSeconds());
        assertEquals(3600, oblodai.splits().getConfig().refundHoldSeconds());
        assertTrue(oblodai.splits().setOptIn(true).enabled());
        assertTrue(oblodai.splits().getOptIn().enabled());
        assertTrue(oblodai.splits().deleteRule(rule.ruleId()).ok());

        assertEquals(
                2,
                oblodai
                        .settings()
                        .setDiscount(
                                new PaymentDiscountSetRequest()
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .discountPercent(2))
                        .discountPercent());
        assertFalse(oblodai.settings().listDiscounts().firstPage().items().isEmpty());
        assertTrue(
                oblodai
                        .settings()
                        .setAccuracy(new PaymentAccuracySetRequest().enabled(true).accuracyPercent(2))
                        .enabled());
        assertTrue(oblodai.settings().getAccuracy().enabled());
        assertTrue(
                oblodai
                        .settings()
                        .setAutoRefund(new PaymentAutorefundSetRequest().overpay(true).underpay(false))
                        .overpay());
        assertNotNull(oblodai.settings().getAutoRefund().configured());
        assertTrue(
                oblodai
                        .settings()
                        .setAccepted(
                                new PaymentAcceptedSetRequest()
                                        .accepted(
                                                List.of(
                                                        new PaymentAcceptedSetRequest.Accepted()
                                                                .currency("USDT")
                                                                .network(Network.TRON))))
                        .ok());
        assertNotNull(oblodai.settings().listAccepted().firstPage().items());
        assertEquals(
                50,
                oblodai
                        .settings()
                        .setPaymentFeeConfig(new PaymentFeeConfigSetRequest().payerPaysPercent(50))
                        .payerPaysPercent());
        assertEquals(50, oblodai.settings().getPaymentFeeConfig().payerPaysPercent());

        assertFalse(
                oblodai
                        .settings()
                        .setAutoWithdraw(
                                new AutoWithdrawSetRequest()
                                        .currency("USDT")
                                        .network(Network.TRON)
                                        .address(ADDRESS)
                                        .minAmount("100"))
                        .isEmpty());
        assertNotNull(oblodai.settings().listAutoWithdraw());
        assertNotNull(oblodai.settings().deleteAutoWithdraw("USDT"));

        assertTrue(oblodai.settings().addApiAllowlist("203.0.113.0/24").items().contains("203.0.113.0/24"));
        assertTrue(oblodai.settings().listApiAllowlist().items().contains("203.0.113.0/24"));
        assertFalse(oblodai.settings().enableApiAllowlist(false).enabled());
        assertFalse(
                oblodai.settings().removeApiAllowlist("203.0.113.0/24").items().contains("203.0.113.0/24"));
    }

    @Test
    @Order(8)
    void webhooksAndTheSandboxInspector() {
        assertNotNull(oblodai.webhooks().register(Live.hookUrl()).endpointId());
        WebhookSecretRotated rotated = oblodai.webhooks().rotateSecret();
        assertNotNull(rotated.secret());
        assertNotNull(oblodai.webhooks().deliveries(new WebhooksDeliveriesRequest().limit(5)).firstPage().items());
        accept(
                () ->
                        oblodai
                                .webhooks()
                                .testPayment(
                                        new TestWebhookPaymentRequest()
                                                .urlCallback(Live.hookUrl())
                                                .currency("USDT")
                                                .network(Network.TRON)
                                                .status("paid")));
        accept(
                () ->
                        oblodai
                                .webhooks()
                                .testLegacy(new PaymentTestingWebhookRequest().url(Live.hookUrl()).status("paid")));

        List<WebhookDelivery> deliveries = oblodai.sandbox().webhooks(5, 0).firstPage().items();
        assertNotNull(deliveries);
        deliveries.stream()
                .filter(
                        d ->
                                d.status() == com.oblodai.contract.DeliveryStatus.DELIVERED
                                        || d.status() == com.oblodai.contract.DeliveryStatus.DEAD)
                .findFirst()
                .ifPresent(terminal -> accept(() -> oblodai.sandbox().replay(terminal.id())));
    }

    @Test
    @Order(9)
    void walletsAndTransfersAreRefusedForADevStoreAsDocumented() {
        accept(
                () ->
                        oblodai
                                .wallets()
                                .create(
                                        new WalletRequest()
                                                .currency("USDT")
                                                .network(Network.TRON)
                                                .orderId(reference("sw-w"))));
        accept(() -> oblodai.wallets().qr(ADDRESS));
        accept(() -> oblodai.wallets().block(new WalletBlockRequest().address(ADDRESS)));
        accept(
                () ->
                        oblodai
                                .transfers()
                                .toPersonal(new TransferToPersonalRequest().amount("1").currency("USDT")));
    }

    @Test
    @Order(10)
    void documentsWhenTheStandHasARenderer() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                documentsEnabled, "this stand has no document renderer");
        FileResultAssertions.assertPdf(
                oblodai.documents().statement(new DocumentQuery().from("2026-01-01").to("2026-12-31").lang("en")));
        assertTrue(oblodai.documents().feeSchedule().bytes().length > 0);
        assertNotNull(oblodai.documents().ledger(new DocumentQuery().format("csv")).contentType());
        if (cheque != null && cheque.claimToken() != null) {
            assertTrue(
                    oblodai
                            .payoutLinks()
                            .cheque(new PayoutLinkChequeRequest().claimToken(cheque.claimToken()).lang("en"))
                            .contentType()
                            .contains("pdf"));
        }

        DocumentJob job =
                oblodai
                        .documents()
                        .createJob(
                                new DocumentsJobsRequest()
                                        .kind("statement")
                                        .format("csv")
                                        .lang("en")
                                        .from("2026-01-01")
                                        .to("2026-12-31"));
        assertEquals(job.jobId(), oblodai.documents().jobInfo(job.jobId()).jobId());
        accept(() -> oblodai.documents().jobFile(job.jobId()));

        // A signed document link: the SDK must be able to fetch what the gateway handed out.
        URI documentUrl = URI.create(oblodai.payments().get(invoice.uuid()).documentUrl());
        String[] segments = documentUrl.getPath().split("/");
        Map<String, String> query = new java.util.LinkedHashMap<>();
        for (String pair : documentUrl.getQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            query.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        FileResultAssertions.assertPdf(
                anonymous
                        .documents()
                        .download(
                                segments[3],
                                segments[4],
                                Long.parseLong(query.get("exp")),
                                query.get("sig"),
                                new DocumentQuery()));
    }

    @Test
    @Order(11)
    void sandboxResetLast() {
        assertNotNull(oblodai.sandbox().reset().invoicesCancelled());
    }

    /** Small helper so the document assertions read the same everywhere. */
    private static final class FileResultAssertions {
        static void assertPdf(com.oblodai.core.FileResult file) {
            assertTrue(file.bytes().length > 0, "an empty document is a failure");
            assertTrue(file.contentType().contains("pdf"), "content type " + file.contentType());
        }
    }
}
