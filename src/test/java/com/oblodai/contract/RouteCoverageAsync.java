package com.oblodai.contract;

import static com.oblodai.contract.RouteCoverage.query;

import com.oblodai.OblodaiAsync;
import com.oblodai.contract.requests.*;
import com.oblodai.resources.DocumentQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The same coverage table as {@link RouteCoverage}, against the asynchronous client: every call is
 * the blocking one with {@code join()} on the end. Keeping the two side by side is what makes the
 * hand-written async tree testable — the wiring test runs each route through both and demands the
 * identical request.
 */
final class RouteCoverageAsync {

    private RouteCoverageAsync() {}

    /** Every route, called on the asynchronous client. */
    static Map<String, Consumer<OblodaiAsync>> asynchronous() {
        Map<String, Consumer<OblodaiAsync>> map = new LinkedHashMap<>();

        map.put("GET /v1/claim/{token}", o -> o.payoutLinks().claimPreview("tok").join());
        map.put("GET /v1/currencies", o -> o.catalog().currencies().join());
        map.put("GET /v1/documents/balance", o -> o.documents().balanceCertificate().join());
        map.put("GET /v1/documents/batch", o -> o.documents().batchReport("b1", query()).join());
        map.put("GET /v1/documents/fees", o -> o.documents().feeSchedule().join());
        map.put("GET /v1/documents/jobs/file", o -> o.documents().jobFile("j1").join());
        map.put("GET /v1/documents/ledger", o -> o.documents().ledger().join());
        map.put("GET /v1/documents/link", o -> o.documents().linkReport("l1", query()).join());
        map.put("GET /v1/documents/referrals", o -> o.documents().referralsReport().join());
        map.put("GET /v1/documents/split", o -> o.documents().splitReport("i1", query()).join());
        map.put(
                "GET /v1/documents/statement",
                o ->
                        o.documents()
                                .statement(
                                        new DocumentQuery()
                                                .from("2026-01-01")
                                                .to("2026-02-01")).join());
        map.put(
                "GET /v1/documents/wallet/statement",
                o -> o.documents().walletStatement("w1", query()).join());
        map.put(
                "GET /v1/documents/{kind}/{id}",
                o -> o.documents().download("invoice", "i1", 1L, "s", query()).join());
        map.put("GET /v1/link/{id}", o -> o.paymentLinks().publicView("l1").join());
        map.put("GET /v1/pay/{id}", o -> o.payments().publicView("i1").join());
        map.put("GET /v1/pay/{id}/qr", o -> o.payments().publicQr("i1").join());
        map.put("GET /v1/sandbox/webhooks", o -> o.sandbox().webhooks().firstPage().join());
        map.put("POST /v1/api-allowlist/add", o -> o.settings().addApiAllowlist("10.0.0.0/8").join());
        map.put("POST /v1/api-allowlist/enable", o -> o.settings().enableApiAllowlist(true).join());
        map.put("POST /v1/api-allowlist/list", o -> o.settings().listApiAllowlist().join());
        map.put("POST /v1/api-allowlist/remove", o -> o.settings().removeApiAllowlist("10.0.0.0/8").join());
        map.put("POST /v1/auto-withdraw/delete", o -> o.settings().deleteAutoWithdraw("USDT").join());
        map.put("POST /v1/auto-withdraw/list", o -> o.settings().listAutoWithdraw().join());
        map.put(
                "POST /v1/auto-withdraw/set",
                o ->
                        o.settings()
                                .setAutoWithdraw(
                                        new AutoWithdrawSetRequest()
                                                .currency("USDT")
                                                .network(Network.TRON)
                                                .address("T")).join());
        map.put("POST /v1/balance", o -> o.account().balance().join());
        map.put("POST /v1/batch/info", o -> o.batches().info(new BatchInfoRequest().batchId("b1")).join());
        map.put(
                "POST /v1/claim/{token}",
                o -> o.payoutLinks().claim("tok", new ClaimTokenRequest().address("T")).join());
        map.put("POST /v1/exchange-rate/list", o -> o.catalog().exchangeRates().firstPage().join());
        map.put("POST /v1/link/{id}/checkout", o -> o.paymentLinks().checkout("l1").join());
        map.put(
                "POST /v1/pay/{id}/select",
                o ->
                        o.payments()
                                .select(
                                        "i1",
                                        new PayIdSelectRequest().currency("USDT").network(Network.TRON)).join());
        map.put(
                "POST /v1/payment",
                o -> o.payments().create(new PaymentRequest().amount("1").currency("USDT")).join());
        map.put("POST /v1/payment/accepted/list", o -> o.settings().listAccepted().firstPage().join());
        map.put(
                "POST /v1/payment/accepted/set",
                o -> o.settings().setAccepted(new PaymentAcceptedSetRequest().accepted(List.of())).join());
        map.put("POST /v1/payment/accuracy/get", o -> o.settings().getAccuracy().join());
        map.put(
                "POST /v1/payment/accuracy/set",
                o -> o.settings().setAccuracy(new PaymentAccuracySetRequest().enabled(true)).join());
        map.put("POST /v1/payment/autorefund/get", o -> o.settings().getAutoRefund().join());
        map.put(
                "POST /v1/payment/autorefund/set",
                o ->
                        o.settings()
                                .setAutoRefund(
                                        new PaymentAutorefundSetRequest().overpay(true).underpay(false)).join());
        map.put(
                "POST /v1/payment/batch",
                o -> o.payments().batch(new PaymentBatchRequest().payments(List.of())).join());
        map.put("POST /v1/payment/cancel", o -> o.payments().cancel("i1").join());
        map.put("POST /v1/payment/discount/list", o -> o.settings().listDiscounts().firstPage().join());
        map.put(
                "POST /v1/payment/discount/set",
                o -> o.settings().setDiscount(new PaymentDiscountSetRequest().discountPercent(1)).join());
        map.put("POST /v1/payment/fee-config/get", o -> o.settings().getPaymentFeeConfig().join());
        map.put(
                "POST /v1/payment/fee-config/set",
                o ->
                        o.settings()
                                .setPaymentFeeConfig(new PaymentFeeConfigSetRequest().payerPaysPercent(50)).join());
        map.put("POST /v1/payment/history", o -> o.payments().history().firstPage().join());
        map.put("POST /v1/payment/info", o -> o.payments().info("i1").join());
        map.put(
                "POST /v1/payment/link",
                o ->
                        o.paymentLinks()
                                .create(new PaymentLinkRequest().amountMode(AmountMode.OPEN).currency("USDT")).join());
        map.put("POST /v1/payment/link/info", o -> o.paymentLinks().info("l1").join());
        map.put("POST /v1/payment/link/list", o -> o.paymentLinks().list().firstPage().join());
        map.put("POST /v1/payment/link/toggle", o -> o.paymentLinks().toggle("l1", false).join());
        map.put("POST /v1/payment/qr", o -> o.payments().qr("i1").join());
        map.put(
                "POST /v1/payment/refund",
                o -> o.refunds().create(new PaymentRefundRequest().uuid("i1")).join());
        map.put("POST /v1/payment/resend", o -> o.payments().resend("i1").join());
        map.put(
                "POST /v1/payment/resolve",
                o -> o.refunds().resolve(new PaymentResolveRequest().uuid("i1").action("accept")).join());
        map.put(
                "POST /v1/payment/send-email",
                o -> o.payments().sendEmail(new PaymentSendEmailRequest().uuid("i1")).join());
        map.put("POST /v1/payment/services", o -> o.payments().services().firstPage().join());
        map.put(
                "POST /v1/payment/testing-webhook",
                o -> o.webhooks().testLegacy(new PaymentTestingWebhookRequest().url("https://x")).join());
        map.put(
                "POST /v1/payout",
                o ->
                        o.payouts()
                                .create(
                                        new PayoutRequest()
                                                .amount("1")
                                                .currency("USDT")
                                                .address("T")
                                                .orderId("o")).join());
        map.put("POST /v1/payout/approve", o -> o.payouts().approve("p1").join());
        map.put("POST /v1/payout/batch", o -> o.payouts().batch(new PayoutBatchRequest().payouts(List.of())).join());
        map.put(
                "POST /v1/payout/calculate",
                o -> o.payouts().calculate(new PayoutCalculateRequest().amount("1").currency("USDT")).join());
        map.put("POST /v1/payout/cancel", o -> o.payouts().cancel("p1").join());
        map.put("POST /v1/payout/fee-config/get", o -> o.payouts().getFeeConfig().join());
        map.put(
                "POST /v1/payout/fee-config/set",
                o -> o.payouts().setFeeConfig(new PayoutFeeConfigSetRequest().feeOnRecipient(true)).join());
        map.put("POST /v1/payout/history", o -> o.payouts().history().firstPage().join());
        map.put("POST /v1/payout/info", o -> o.payouts().info("p1").join());
        map.put(
                "POST /v1/payout/link",
                o ->
                        o.payoutLinks()
                                .create(
                                        new PayoutLinkRequest()
                                                .amount("1")
                                                .currency("USDT")
                                                .network(Network.TRON)).join());
        map.put(
                "POST /v1/payout/link/batch",
                o -> o.payoutLinks().batch(new PayoutLinkBatchRequest().items(List.of())).join());
        map.put("POST /v1/payout/link/cancel", o -> o.payoutLinks().cancel("l1").join());
        map.put(
                "POST /v1/payout/link/cheque",
                o -> o.payoutLinks().cheque(new PayoutLinkChequeRequest().claimToken("t")).join());
        map.put("POST /v1/payout/link/info", o -> o.payoutLinks().info("l1").join());
        map.put("POST /v1/payout/link/list", o -> o.payoutLinks().list().firstPage().join());
        map.put("POST /v1/payout/mass", o -> o.payouts().mass(new PayoutMassRequest().payouts(List.of())).join());
        map.put("POST /v1/payout/refund-fee-config/get", o -> o.payouts().getRefundFeeConfig().join());
        map.put(
                "POST /v1/payout/refund-fee-config/set",
                o ->
                        o.payouts()
                                .setRefundFeeConfig(
                                        new PayoutRefundFeeConfigSetRequest().feeOnCustomer(true)).join());
        map.put("POST /v1/payout/services", o -> o.payouts().services().firstPage().join());
        map.put(
                "POST /v1/payout/validate",
                o ->
                        o.payouts()
                                .validate(
                                        new PayoutValidateRequest().amount("1").currency("USDT").address("T")).join());
        map.put("POST /v1/referral/info", o -> o.account().referral().join());
        map.put("POST /v1/refund/batch", o -> o.refunds().batch(new RefundBatchRequest().refunds(List.of())).join());
        map.put(
                "POST /v1/sandbox/deposit",
                o -> o.sandbox().deposit(new SandboxDepositRequest().invoiceId("i1")).join());
        map.put(
                "POST /v1/sandbox/faucet",
                o -> o.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("1")).join());
        map.put("POST /v1/sandbox/reset", o -> o.sandbox().reset().join());
        map.put("POST /v1/sandbox/webhooks/replay", o -> o.sandbox().replay("d1").join());
        map.put("POST /v1/split/config/get", o -> o.splits().getConfig().join());
        map.put(
                "POST /v1/split/config/set",
                o -> o.splits().setConfig(new SplitConfigSetRequest().refundHoldSeconds(60)).join());
        map.put("POST /v1/split/recipient/optin", o -> o.splits().setOptIn(true).join());
        map.put("POST /v1/split/recipient/optin/get", o -> o.splits().getOptIn().join());
        map.put("POST /v1/split/rule", o -> o.splits().createRule(new SplitRuleRequest().percent("10")).join());
        map.put("POST /v1/split/rule/delete", o -> o.splits().deleteRule("r1").join());
        map.put("POST /v1/split/rule/list", o -> o.splits().listRules().firstPage().join());
        map.put(
                "POST /v1/test-webhook/payment",
                o -> o.webhooks().testPayment(new TestWebhookPaymentRequest().urlCallback("https://x")).join());
        map.put(
                "POST /v1/test-webhook/payout",
                o -> o.webhooks().testPayout(new TestWebhookPayoutRequest().urlCallback("https://x")).join());
        map.put(
                "POST /v1/test-webhook/wallet",
                o -> o.webhooks().testWallet(new TestWebhookWalletRequest().urlCallback("https://x")).join());
        map.put("POST /v1/transfer/batch", o -> o.transfers().batch(new TransferBatchRequest()).join());
        map.put(
                "POST /v1/transfer/to-personal",
                o -> o.transfers().toPersonal(new TransferToPersonalRequest().amount("1").currency("USDT")).join());
        map.put(
                "POST /v1/transfer/to-user",
                o ->
                        o.transfers()
                                .toUser(
                                        new TransferToUserRequest()
                                                .toUserId("u")
                                                .amount("1")
                                                .currency("USDT")).join());
        map.put("POST /v1/vrcs", o -> o.account().vrcs().join());
        map.put(
                "POST /v1/wallet",
                o -> o.wallets().create(new WalletRequest().currency("USDT").network(Network.TRON)).join());
        map.put("POST /v1/wallet/block", o -> o.wallets().block(new WalletBlockRequest().address("T")).join());
        map.put(
                "POST /v1/wallet/blocked-address-refund",
                o ->
                        o.wallets()
                                .refundBlockedDeposit(
                                        new WalletBlockedAddressRefundRequest().uuid("w1").address("T")).join());
        map.put("POST /v1/wallet/qr", o -> o.wallets().qr("T").join());
        map.put("POST /v1/webhooks", o -> o.webhooks().register("https://x").join());
        map.put("POST /v1/webhooks/deliveries", o -> o.webhooks().deliveries().firstPage().join());
        map.put("POST /v1/webhooks/rotate-secret", o -> o.webhooks().rotateSecret().join());
        map.put(
                "POST /v1/documents/jobs",
                o -> o.documents().createJob(new DocumentsJobsRequest().kind("statement")).join());
        map.put("POST /v1/documents/jobs/info", o -> o.documents().jobInfo("j1").join());
        map.put(
                "POST /v1/merchants",
                o -> o.merchants().create(new MerchantsRequest().email("a@b.c").name("A")).join());
        map.put("POST /v1/merchants/{id}/sandbox", o -> o.merchants().createSandbox("m1").join());

        return map;
    }
}
