package com.oblodai.contract;

import com.oblodai.Oblodai;
import com.oblodai.contract.requests.*;
import com.oblodai.resources.DocumentQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * One call per gateway route on the blocking client. {@link RouteCoverageAsync} is the same table
 * against the asynchronous client, and the same test drives both: the async tree is a mirror of this
 * one, method for method, so a route wired to the wrong path, gate or idempotency behaviour on
 * one side and not the other fails the build.
 *
 * <p>Both tables must have key sets equal to {@link Routes#ALL}.
 */
final class RouteCoverage {

    private RouteCoverage() {}

    /** A document query with a language, so the covered document calls all look alike. */
    static DocumentQuery query() {
        return new DocumentQuery().lang("en");
    }

    /** Every route, called on the blocking client. */
    static Map<String, Consumer<Oblodai>> blocking() {
        Map<String, Consumer<Oblodai>> map = new LinkedHashMap<>();

        map.put("GET /v1/claim/{token}", o -> o.payoutLinks().claimPreview("tok"));
        map.put("GET /v1/currencies", o -> o.catalog().currencies());
        map.put("GET /v1/documents/balance", o -> o.documents().balanceCertificate());
        map.put("GET /v1/documents/batch", o -> o.documents().batchReport("b1", query()));
        map.put("GET /v1/documents/fees", o -> o.documents().feeSchedule());
        map.put("GET /v1/documents/jobs/file", o -> o.documents().jobFile("j1"));
        map.put("GET /v1/documents/ledger", o -> o.documents().ledger());
        map.put("GET /v1/documents/link", o -> o.documents().linkReport("l1", query()));
        map.put("GET /v1/documents/referrals", o -> o.documents().referralsReport());
        map.put("GET /v1/documents/split", o -> o.documents().splitReport("i1", query()));
        map.put(
                "GET /v1/documents/statement",
                o ->
                        o.documents()
                                .statement(
                                        new DocumentQuery()
                                                .from("2026-01-01")
                                                .to("2026-02-01")));
        map.put(
                "GET /v1/documents/wallet/statement",
                o -> o.documents().walletStatement("w1", query()));
        map.put(
                "GET /v1/documents/{kind}/{id}",
                o -> o.documents().download("invoice", "i1", 1L, "s", query()));
        map.put("GET /v1/link/{id}", o -> o.paymentLinks().publicView("l1"));
        map.put("GET /v1/pay/{id}", o -> o.payments().publicView("i1"));
        map.put("GET /v1/pay/{id}/qr", o -> o.payments().publicQr("i1"));
        map.put("GET /v1/sandbox/webhooks", o -> o.sandbox().webhooks().firstPage());
        map.put("POST /v1/api-allowlist/add", o -> o.settings().addApiAllowlist("10.0.0.0/8"));
        map.put("POST /v1/api-allowlist/enable", o -> o.settings().enableApiAllowlist(true));
        map.put("POST /v1/api-allowlist/list", o -> o.settings().listApiAllowlist());
        map.put("POST /v1/api-allowlist/remove", o -> o.settings().removeApiAllowlist("10.0.0.0/8"));
        map.put("POST /v1/auto-withdraw/delete", o -> o.settings().deleteAutoWithdraw("USDT"));
        map.put("POST /v1/auto-withdraw/list", o -> o.settings().listAutoWithdraw());
        map.put(
                "POST /v1/auto-withdraw/set",
                o ->
                        o.settings()
                                .setAutoWithdraw(
                                        new AutoWithdrawSetRequest()
                                                .currency("USDT")
                                                .network(Network.TRON)
                                                .address("T")));
        map.put("POST /v1/balance", o -> o.account().balance());
        map.put("POST /v1/batch/info", o -> o.batches().info(new BatchInfoRequest().batchId("b1")));
        map.put(
                "POST /v1/claim/{token}",
                o -> o.payoutLinks().claim("tok", new ClaimTokenRequest().address("T")));
        map.put("POST /v1/exchange-rate/list", o -> o.catalog().exchangeRates().firstPage());
        map.put("POST /v1/link/{id}/checkout", o -> o.paymentLinks().checkout("l1"));
        map.put(
                "POST /v1/pay/{id}/select",
                o ->
                        o.payments()
                                .select(
                                        "i1",
                                        new PayIdSelectRequest().currency("USDT").network(Network.TRON)));
        map.put(
                "POST /v1/payment",
                o -> o.payments().create(new PaymentRequest().amount("1").currency("USDT")));
        map.put("POST /v1/payment/accepted/list", o -> o.settings().listAccepted().firstPage());
        map.put(
                "POST /v1/payment/accepted/set",
                o -> o.settings().setAccepted(new PaymentAcceptedSetRequest().accepted(List.of())));
        map.put("POST /v1/payment/accuracy/get", o -> o.settings().getAccuracy());
        map.put(
                "POST /v1/payment/accuracy/set",
                o -> o.settings().setAccuracy(new PaymentAccuracySetRequest().enabled(true)));
        map.put("POST /v1/payment/autorefund/get", o -> o.settings().getAutoRefund());
        map.put(
                "POST /v1/payment/autorefund/set",
                o ->
                        o.settings()
                                .setAutoRefund(
                                        new PaymentAutorefundSetRequest().overpay(true).underpay(false)));
        map.put(
                "POST /v1/payment/batch",
                o -> o.payments().batch(new PaymentBatchRequest().payments(List.of())));
        map.put("POST /v1/payment/cancel", o -> o.payments().cancel("i1"));
        map.put("POST /v1/payment/discount/list", o -> o.settings().listDiscounts().firstPage());
        map.put(
                "POST /v1/payment/discount/set",
                o -> o.settings().setDiscount(new PaymentDiscountSetRequest().discountPercent(1)));
        map.put("POST /v1/payment/fee-config/get", o -> o.settings().getPaymentFeeConfig());
        map.put(
                "POST /v1/payment/fee-config/set",
                o ->
                        o.settings()
                                .setPaymentFeeConfig(new PaymentFeeConfigSetRequest().payerPaysPercent(50)));
        map.put("POST /v1/payment/history", o -> o.payments().history().firstPage());
        map.put("POST /v1/payment/info", o -> o.payments().info("i1"));
        map.put(
                "POST /v1/payment/link",
                o ->
                        o.paymentLinks()
                                .create(new PaymentLinkRequest().amountMode(AmountMode.OPEN).currency("USDT")));
        map.put("POST /v1/payment/link/info", o -> o.paymentLinks().info("l1"));
        map.put("POST /v1/payment/link/list", o -> o.paymentLinks().list().firstPage());
        map.put("POST /v1/payment/link/toggle", o -> o.paymentLinks().toggle("l1", false));
        map.put("POST /v1/payment/qr", o -> o.payments().qr("i1"));
        map.put(
                "POST /v1/payment/refund",
                o -> o.refunds().create(new PaymentRefundRequest().uuid("i1")));
        map.put("POST /v1/payment/resend", o -> o.payments().resend("i1"));
        map.put(
                "POST /v1/payment/resolve",
                o -> o.refunds().resolve(new PaymentResolveRequest().uuid("i1").action("accept")));
        map.put(
                "POST /v1/payment/send-email",
                o -> o.payments().sendEmail(new PaymentSendEmailRequest().uuid("i1")));
        map.put("POST /v1/payment/services", o -> o.payments().services().firstPage());
        map.put(
                "POST /v1/payment/testing-webhook",
                o -> o.webhooks().testLegacy(new PaymentTestingWebhookRequest().url("https://x")));
        map.put(
                "POST /v1/payout",
                o ->
                        o.payouts()
                                .create(
                                        new PayoutRequest()
                                                .amount("1")
                                                .currency("USDT")
                                                .address("T")
                                                .orderId("o")));
        map.put("POST /v1/payout/approve", o -> o.payouts().approve("p1"));
        map.put("POST /v1/payout/batch", o -> o.payouts().batch(new PayoutBatchRequest().payouts(List.of())));
        map.put(
                "POST /v1/payout/calculate",
                o -> o.payouts().calculate(new PayoutCalculateRequest().amount("1").currency("USDT")));
        map.put("POST /v1/payout/cancel", o -> o.payouts().cancel("p1"));
        map.put("POST /v1/payout/fee-config/get", o -> o.payouts().getFeeConfig());
        map.put(
                "POST /v1/payout/fee-config/set",
                o -> o.payouts().setFeeConfig(new PayoutFeeConfigSetRequest().feeOnRecipient(true)));
        map.put("POST /v1/payout/history", o -> o.payouts().history().firstPage());
        map.put("POST /v1/payout/info", o -> o.payouts().info("p1"));
        map.put(
                "POST /v1/payout/link",
                o ->
                        o.payoutLinks()
                                .create(
                                        new PayoutLinkRequest()
                                                .amount("1")
                                                .currency("USDT")
                                                .network(Network.TRON)));
        map.put(
                "POST /v1/payout/link/batch",
                o -> o.payoutLinks().batch(new PayoutLinkBatchRequest().items(List.of())));
        map.put("POST /v1/payout/link/cancel", o -> o.payoutLinks().cancel("l1"));
        map.put(
                "POST /v1/payout/link/cheque",
                o -> o.payoutLinks().cheque(new PayoutLinkChequeRequest().claimToken("t")));
        map.put("POST /v1/payout/link/info", o -> o.payoutLinks().info("l1"));
        map.put("POST /v1/payout/link/list", o -> o.payoutLinks().list().firstPage());
        map.put("POST /v1/payout/mass", o -> o.payouts().mass(new PayoutMassRequest().payouts(List.of())));
        map.put("POST /v1/payout/refund-fee-config/get", o -> o.payouts().getRefundFeeConfig());
        map.put(
                "POST /v1/payout/refund-fee-config/set",
                o ->
                        o.payouts()
                                .setRefundFeeConfig(
                                        new PayoutRefundFeeConfigSetRequest().feeOnCustomer(true)));
        map.put("POST /v1/payout/services", o -> o.payouts().services().firstPage());
        map.put(
                "POST /v1/payout/validate",
                o ->
                        o.payouts()
                                .validate(
                                        new PayoutValidateRequest().amount("1").currency("USDT").address("T")));
        map.put("POST /v1/referral/info", o -> o.account().referral());
        map.put("POST /v1/refund/batch", o -> o.refunds().batch(new RefundBatchRequest().refunds(List.of())));
        map.put(
                "POST /v1/sandbox/deposit",
                o -> o.sandbox().deposit(new SandboxDepositRequest().invoiceId("i1")));
        map.put(
                "POST /v1/sandbox/faucet",
                o -> o.sandbox().faucet(new SandboxFaucetRequest().asset("USDT").amount("1")));
        map.put("POST /v1/sandbox/reset", o -> o.sandbox().reset());
        map.put("POST /v1/sandbox/webhooks/replay", o -> o.sandbox().replay("d1"));
        map.put("POST /v1/split/config/get", o -> o.splits().getConfig());
        map.put(
                "POST /v1/split/config/set",
                o -> o.splits().setConfig(new SplitConfigSetRequest().refundHoldSeconds(60)));
        map.put("POST /v1/split/recipient/optin", o -> o.splits().setOptIn(true));
        map.put("POST /v1/split/recipient/optin/get", o -> o.splits().getOptIn());
        map.put("POST /v1/split/rule", o -> o.splits().createRule(new SplitRuleRequest().percent("10")));
        map.put("POST /v1/split/rule/delete", o -> o.splits().deleteRule("r1"));
        map.put("POST /v1/split/rule/list", o -> o.splits().listRules().firstPage());
        map.put(
                "POST /v1/test-webhook/payment",
                o -> o.webhooks().testPayment(new TestWebhookPaymentRequest().urlCallback("https://x")));
        map.put(
                "POST /v1/test-webhook/payout",
                o -> o.webhooks().testPayout(new TestWebhookPayoutRequest().urlCallback("https://x")));
        map.put(
                "POST /v1/test-webhook/wallet",
                o -> o.webhooks().testWallet(new TestWebhookWalletRequest().urlCallback("https://x")));
        map.put("POST /v1/transfer/batch", o -> o.transfers().batch(new TransferBatchRequest()));
        map.put(
                "POST /v1/transfer/to-personal",
                o -> o.transfers().toPersonal(new TransferToPersonalRequest().amount("1").currency("USDT")));
        map.put(
                "POST /v1/transfer/to-user",
                o ->
                        o.transfers()
                                .toUser(
                                        new TransferToUserRequest()
                                                .toUserId("u")
                                                .amount("1")
                                                .currency("USDT")));
        map.put("POST /v1/vrcs", o -> o.account().vrcs());
        map.put(
                "POST /v1/wallet",
                o -> o.wallets().create(new WalletRequest().currency("USDT").network(Network.TRON)));
        map.put("POST /v1/wallet/block", o -> o.wallets().block(new WalletBlockRequest().address("T")));
        map.put(
                "POST /v1/wallet/blocked-address-refund",
                o ->
                        o.wallets()
                                .refundBlockedDeposit(
                                        new WalletBlockedAddressRefundRequest().uuid("w1").address("T")));
        map.put("POST /v1/wallet/qr", o -> o.wallets().qr("T"));
        map.put("POST /v1/webhooks", o -> o.webhooks().register("https://x"));
        map.put("POST /v1/webhooks/deliveries", o -> o.webhooks().deliveries().firstPage());
        map.put("POST /v1/webhooks/rotate-secret", o -> o.webhooks().rotateSecret());
        map.put(
                "POST /v1/documents/jobs",
                o -> o.documents().createJob(new DocumentsJobsRequest().kind("statement")));
        map.put("POST /v1/documents/jobs/info", o -> o.documents().jobInfo("j1"));
        map.put(
                "POST /v1/merchants",
                o -> o.merchants().create(new MerchantsRequest().email("a@b.c").name("A")));
        map.put("POST /v1/merchants/{id}/sandbox", o -> o.merchants().createSandbox("m1"));

        return map;
    }
}
