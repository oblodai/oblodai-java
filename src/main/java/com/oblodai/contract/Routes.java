// GENERATED FILE — do not edit. Source: contract/contract.json (core bfca971cce71).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/**
 * Every merchant-facing route the gateway declares, keyed exactly as its conformance table keys
 * them ({@code "POST /v1/payment"}).
 */
public final class Routes {

    private Routes() {}

    /** {@code POST /v1/api-allowlist/add} */
    public static final RouteSpec POST_V1_API_ALLOWLIST_ADD = new RouteSpec("POST", "/v1/api-allowlist/add", RouteAuth.PAYOUT, false, false, false, ListKind.PLAIN);

    /** {@code POST /v1/api-allowlist/enable} */
    public static final RouteSpec POST_V1_API_ALLOWLIST_ENABLE = new RouteSpec("POST", "/v1/api-allowlist/enable", RouteAuth.PAYOUT, false, false, false, ListKind.PLAIN);

    /** {@code POST /v1/api-allowlist/list} */
    public static final RouteSpec POST_V1_API_ALLOWLIST_LIST = new RouteSpec("POST", "/v1/api-allowlist/list", RouteAuth.PAYOUT, false, true, false, ListKind.PLAIN);

    /** {@code POST /v1/api-allowlist/remove} */
    public static final RouteSpec POST_V1_API_ALLOWLIST_REMOVE = new RouteSpec("POST", "/v1/api-allowlist/remove", RouteAuth.PAYOUT, false, false, false, ListKind.PLAIN);

    /** {@code POST /v1/auto-withdraw/delete} */
    public static final RouteSpec POST_V1_AUTO_WITHDRAW_DELETE = new RouteSpec("POST", "/v1/auto-withdraw/delete", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/auto-withdraw/list} */
    public static final RouteSpec POST_V1_AUTO_WITHDRAW_LIST = new RouteSpec("POST", "/v1/auto-withdraw/list", RouteAuth.PAYOUT, false, true, false, ListKind.PLAIN);

    /** {@code POST /v1/auto-withdraw/set} */
    public static final RouteSpec POST_V1_AUTO_WITHDRAW_SET = new RouteSpec("POST", "/v1/auto-withdraw/set", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/balance} */
    public static final RouteSpec POST_V1_BALANCE = new RouteSpec("POST", "/v1/balance", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/batch/info} */
    public static final RouteSpec POST_V1_BATCH_INFO = new RouteSpec("POST", "/v1/batch/info", RouteAuth.ANY, false, true, false, null);

    /** {@code GET /v1/claim/{token}} */
    public static final RouteSpec GET_V1_CLAIM_TOKEN = new RouteSpec("GET", "/v1/claim/{token}", RouteAuth.PUBLIC, false, true, false, null);

    /** {@code POST /v1/claim/{token}} */
    public static final RouteSpec POST_V1_CLAIM_TOKEN = new RouteSpec("POST", "/v1/claim/{token}", RouteAuth.PUBLIC, false, false, false, null);

    /** {@code GET /v1/currencies} */
    public static final RouteSpec GET_V1_CURRENCIES = new RouteSpec("GET", "/v1/currencies", RouteAuth.PUBLIC, false, true, false, null);

    /** {@code GET /v1/documents/balance} */
    public static final RouteSpec GET_V1_DOCUMENTS_BALANCE = new RouteSpec("GET", "/v1/documents/balance", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/batch} */
    public static final RouteSpec GET_V1_DOCUMENTS_BATCH = new RouteSpec("GET", "/v1/documents/batch", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/fees} */
    public static final RouteSpec GET_V1_DOCUMENTS_FEES = new RouteSpec("GET", "/v1/documents/fees", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code POST /v1/documents/jobs} */
    public static final RouteSpec POST_V1_DOCUMENTS_JOBS = new RouteSpec("POST", "/v1/documents/jobs", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code GET /v1/documents/jobs/file} */
    public static final RouteSpec GET_V1_DOCUMENTS_JOBS_FILE = new RouteSpec("GET", "/v1/documents/jobs/file", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code POST /v1/documents/jobs/info} */
    public static final RouteSpec POST_V1_DOCUMENTS_JOBS_INFO = new RouteSpec("POST", "/v1/documents/jobs/info", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code GET /v1/documents/ledger} */
    public static final RouteSpec GET_V1_DOCUMENTS_LEDGER = new RouteSpec("GET", "/v1/documents/ledger", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/link} */
    public static final RouteSpec GET_V1_DOCUMENTS_LINK = new RouteSpec("GET", "/v1/documents/link", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/referrals} */
    public static final RouteSpec GET_V1_DOCUMENTS_REFERRALS = new RouteSpec("GET", "/v1/documents/referrals", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/split} */
    public static final RouteSpec GET_V1_DOCUMENTS_SPLIT = new RouteSpec("GET", "/v1/documents/split", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/statement} */
    public static final RouteSpec GET_V1_DOCUMENTS_STATEMENT = new RouteSpec("GET", "/v1/documents/statement", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/wallet/statement} */
    public static final RouteSpec GET_V1_DOCUMENTS_WALLET_STATEMENT = new RouteSpec("GET", "/v1/documents/wallet/statement", RouteAuth.PAYMENT, false, true, true, null);

    /** {@code GET /v1/documents/{kind}/{id}} */
    public static final RouteSpec GET_V1_DOCUMENTS_KIND_ID = new RouteSpec("GET", "/v1/documents/{kind}/{id}", RouteAuth.PUBLIC, false, true, true, null);

    /** {@code POST /v1/exchange-rate/list} */
    public static final RouteSpec POST_V1_EXCHANGE_RATE_LIST = new RouteSpec("POST", "/v1/exchange-rate/list", RouteAuth.PUBLIC, false, true, false, ListKind.PAGED);

    /** {@code GET /v1/link/{id}} */
    public static final RouteSpec GET_V1_LINK_ID = new RouteSpec("GET", "/v1/link/{id}", RouteAuth.PUBLIC, false, true, false, null);

    /** {@code POST /v1/link/{id}/checkout} */
    public static final RouteSpec POST_V1_LINK_ID_CHECKOUT = new RouteSpec("POST", "/v1/link/{id}/checkout", RouteAuth.PUBLIC, false, false, false, null);

    /** {@code POST /v1/merchants} */
    public static final RouteSpec POST_V1_MERCHANTS = new RouteSpec("POST", "/v1/merchants", RouteAuth.ONBOARD, false, false, false, null);

    /** {@code POST /v1/merchants/{id}/sandbox} */
    public static final RouteSpec POST_V1_MERCHANTS_ID_SANDBOX = new RouteSpec("POST", "/v1/merchants/{id}/sandbox", RouteAuth.ONBOARD, false, false, false, null);

    /** {@code GET /v1/pay/{id}} */
    public static final RouteSpec GET_V1_PAY_ID = new RouteSpec("GET", "/v1/pay/{id}", RouteAuth.PUBLIC, false, true, false, null);

    /** {@code GET /v1/pay/{id}/qr} */
    public static final RouteSpec GET_V1_PAY_ID_QR = new RouteSpec("GET", "/v1/pay/{id}/qr", RouteAuth.PUBLIC, false, true, false, null);

    /** {@code POST /v1/pay/{id}/select} */
    public static final RouteSpec POST_V1_PAY_ID_SELECT = new RouteSpec("POST", "/v1/pay/{id}/select", RouteAuth.PUBLIC, false, false, false, null);

    /** {@code POST /v1/payment} */
    public static final RouteSpec POST_V1_PAYMENT = new RouteSpec("POST", "/v1/payment", RouteAuth.PAYMENT, true, false, false, null);

    /** {@code POST /v1/payment/accepted/list} */
    public static final RouteSpec POST_V1_PAYMENT_ACCEPTED_LIST = new RouteSpec("POST", "/v1/payment/accepted/list", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payment/accepted/set} */
    public static final RouteSpec POST_V1_PAYMENT_ACCEPTED_SET = new RouteSpec("POST", "/v1/payment/accepted/set", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/accuracy/get} */
    public static final RouteSpec POST_V1_PAYMENT_ACCURACY_GET = new RouteSpec("POST", "/v1/payment/accuracy/get", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/accuracy/set} */
    public static final RouteSpec POST_V1_PAYMENT_ACCURACY_SET = new RouteSpec("POST", "/v1/payment/accuracy/set", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/autorefund/get} */
    public static final RouteSpec POST_V1_PAYMENT_AUTOREFUND_GET = new RouteSpec("POST", "/v1/payment/autorefund/get", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/autorefund/set} */
    public static final RouteSpec POST_V1_PAYMENT_AUTOREFUND_SET = new RouteSpec("POST", "/v1/payment/autorefund/set", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/batch} */
    public static final RouteSpec POST_V1_PAYMENT_BATCH = new RouteSpec("POST", "/v1/payment/batch", RouteAuth.PAYMENT, true, false, false, null);

    /** {@code POST /v1/payment/cancel} */
    public static final RouteSpec POST_V1_PAYMENT_CANCEL = new RouteSpec("POST", "/v1/payment/cancel", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/discount/list} */
    public static final RouteSpec POST_V1_PAYMENT_DISCOUNT_LIST = new RouteSpec("POST", "/v1/payment/discount/list", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payment/discount/set} */
    public static final RouteSpec POST_V1_PAYMENT_DISCOUNT_SET = new RouteSpec("POST", "/v1/payment/discount/set", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/fee-config/get} */
    public static final RouteSpec POST_V1_PAYMENT_FEE_CONFIG_GET = new RouteSpec("POST", "/v1/payment/fee-config/get", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/fee-config/set} */
    public static final RouteSpec POST_V1_PAYMENT_FEE_CONFIG_SET = new RouteSpec("POST", "/v1/payment/fee-config/set", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/history} */
    public static final RouteSpec POST_V1_PAYMENT_HISTORY = new RouteSpec("POST", "/v1/payment/history", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payment/info} */
    public static final RouteSpec POST_V1_PAYMENT_INFO = new RouteSpec("POST", "/v1/payment/info", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/link} */
    public static final RouteSpec POST_V1_PAYMENT_LINK = new RouteSpec("POST", "/v1/payment/link", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/link/info} */
    public static final RouteSpec POST_V1_PAYMENT_LINK_INFO = new RouteSpec("POST", "/v1/payment/link/info", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/link/list} */
    public static final RouteSpec POST_V1_PAYMENT_LINK_LIST = new RouteSpec("POST", "/v1/payment/link/list", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payment/link/toggle} */
    public static final RouteSpec POST_V1_PAYMENT_LINK_TOGGLE = new RouteSpec("POST", "/v1/payment/link/toggle", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/qr} */
    public static final RouteSpec POST_V1_PAYMENT_QR = new RouteSpec("POST", "/v1/payment/qr", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/payment/refund} */
    public static final RouteSpec POST_V1_PAYMENT_REFUND = new RouteSpec("POST", "/v1/payment/refund", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payment/resend} */
    public static final RouteSpec POST_V1_PAYMENT_RESEND = new RouteSpec("POST", "/v1/payment/resend", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/resolve} */
    public static final RouteSpec POST_V1_PAYMENT_RESOLVE = new RouteSpec("POST", "/v1/payment/resolve", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payment/send-email} */
    public static final RouteSpec POST_V1_PAYMENT_SEND_EMAIL = new RouteSpec("POST", "/v1/payment/send-email", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payment/services} */
    public static final RouteSpec POST_V1_PAYMENT_SERVICES = new RouteSpec("POST", "/v1/payment/services", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payment/testing-webhook} */
    public static final RouteSpec POST_V1_PAYMENT_TESTING_WEBHOOK = new RouteSpec("POST", "/v1/payment/testing-webhook", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/payout} */
    public static final RouteSpec POST_V1_PAYOUT = new RouteSpec("POST", "/v1/payout", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payout/approve} */
    public static final RouteSpec POST_V1_PAYOUT_APPROVE = new RouteSpec("POST", "/v1/payout/approve", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/payout/batch} */
    public static final RouteSpec POST_V1_PAYOUT_BATCH = new RouteSpec("POST", "/v1/payout/batch", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payout/calculate} */
    public static final RouteSpec POST_V1_PAYOUT_CALCULATE = new RouteSpec("POST", "/v1/payout/calculate", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/payout/cancel} */
    public static final RouteSpec POST_V1_PAYOUT_CANCEL = new RouteSpec("POST", "/v1/payout/cancel", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/payout/fee-config/get} */
    public static final RouteSpec POST_V1_PAYOUT_FEE_CONFIG_GET = new RouteSpec("POST", "/v1/payout/fee-config/get", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/payout/fee-config/set} */
    public static final RouteSpec POST_V1_PAYOUT_FEE_CONFIG_SET = new RouteSpec("POST", "/v1/payout/fee-config/set", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/payout/history} */
    public static final RouteSpec POST_V1_PAYOUT_HISTORY = new RouteSpec("POST", "/v1/payout/history", RouteAuth.PAYOUT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payout/info} */
    public static final RouteSpec POST_V1_PAYOUT_INFO = new RouteSpec("POST", "/v1/payout/info", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/payout/link} */
    public static final RouteSpec POST_V1_PAYOUT_LINK = new RouteSpec("POST", "/v1/payout/link", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payout/link/batch} */
    public static final RouteSpec POST_V1_PAYOUT_LINK_BATCH = new RouteSpec("POST", "/v1/payout/link/batch", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payout/link/cancel} */
    public static final RouteSpec POST_V1_PAYOUT_LINK_CANCEL = new RouteSpec("POST", "/v1/payout/link/cancel", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/payout/link/cheque} */
    public static final RouteSpec POST_V1_PAYOUT_LINK_CHEQUE = new RouteSpec("POST", "/v1/payout/link/cheque", RouteAuth.PAYOUT, false, false, true, null);

    /** {@code POST /v1/payout/link/info} */
    public static final RouteSpec POST_V1_PAYOUT_LINK_INFO = new RouteSpec("POST", "/v1/payout/link/info", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/payout/link/list} */
    public static final RouteSpec POST_V1_PAYOUT_LINK_LIST = new RouteSpec("POST", "/v1/payout/link/list", RouteAuth.PAYOUT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payout/mass} */
    public static final RouteSpec POST_V1_PAYOUT_MASS = new RouteSpec("POST", "/v1/payout/mass", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/payout/refund-fee-config/get} */
    public static final RouteSpec POST_V1_PAYOUT_REFUND_FEE_CONFIG_GET = new RouteSpec("POST", "/v1/payout/refund-fee-config/get", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/payout/refund-fee-config/set} */
    public static final RouteSpec POST_V1_PAYOUT_REFUND_FEE_CONFIG_SET = new RouteSpec("POST", "/v1/payout/refund-fee-config/set", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/payout/services} */
    public static final RouteSpec POST_V1_PAYOUT_SERVICES = new RouteSpec("POST", "/v1/payout/services", RouteAuth.PAYOUT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/payout/validate} */
    public static final RouteSpec POST_V1_PAYOUT_VALIDATE = new RouteSpec("POST", "/v1/payout/validate", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/referral/info} */
    public static final RouteSpec POST_V1_REFERRAL_INFO = new RouteSpec("POST", "/v1/referral/info", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/refund/batch} */
    public static final RouteSpec POST_V1_REFUND_BATCH = new RouteSpec("POST", "/v1/refund/batch", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/sandbox/deposit} */
    public static final RouteSpec POST_V1_SANDBOX_DEPOSIT = new RouteSpec("POST", "/v1/sandbox/deposit", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/sandbox/faucet} */
    public static final RouteSpec POST_V1_SANDBOX_FAUCET = new RouteSpec("POST", "/v1/sandbox/faucet", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/sandbox/reset} */
    public static final RouteSpec POST_V1_SANDBOX_RESET = new RouteSpec("POST", "/v1/sandbox/reset", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code GET /v1/sandbox/webhooks} */
    public static final RouteSpec GET_V1_SANDBOX_WEBHOOKS = new RouteSpec("GET", "/v1/sandbox/webhooks", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/sandbox/webhooks/replay} */
    public static final RouteSpec POST_V1_SANDBOX_WEBHOOKS_REPLAY = new RouteSpec("POST", "/v1/sandbox/webhooks/replay", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/split/config/get} */
    public static final RouteSpec POST_V1_SPLIT_CONFIG_GET = new RouteSpec("POST", "/v1/split/config/get", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/split/config/set} */
    public static final RouteSpec POST_V1_SPLIT_CONFIG_SET = new RouteSpec("POST", "/v1/split/config/set", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/split/recipient/optin} */
    public static final RouteSpec POST_V1_SPLIT_RECIPIENT_OPTIN = new RouteSpec("POST", "/v1/split/recipient/optin", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/split/recipient/optin/get} */
    public static final RouteSpec POST_V1_SPLIT_RECIPIENT_OPTIN_GET = new RouteSpec("POST", "/v1/split/recipient/optin/get", RouteAuth.PAYOUT, false, true, false, null);

    /** {@code POST /v1/split/rule} */
    public static final RouteSpec POST_V1_SPLIT_RULE = new RouteSpec("POST", "/v1/split/rule", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/split/rule/delete} */
    public static final RouteSpec POST_V1_SPLIT_RULE_DELETE = new RouteSpec("POST", "/v1/split/rule/delete", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/split/rule/list} */
    public static final RouteSpec POST_V1_SPLIT_RULE_LIST = new RouteSpec("POST", "/v1/split/rule/list", RouteAuth.PAYOUT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/test-webhook/payment} */
    public static final RouteSpec POST_V1_TEST_WEBHOOK_PAYMENT = new RouteSpec("POST", "/v1/test-webhook/payment", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/test-webhook/payout} */
    public static final RouteSpec POST_V1_TEST_WEBHOOK_PAYOUT = new RouteSpec("POST", "/v1/test-webhook/payout", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/test-webhook/wallet} */
    public static final RouteSpec POST_V1_TEST_WEBHOOK_WALLET = new RouteSpec("POST", "/v1/test-webhook/wallet", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/transfer/batch} */
    public static final RouteSpec POST_V1_TRANSFER_BATCH = new RouteSpec("POST", "/v1/transfer/batch", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/transfer/to-personal} */
    public static final RouteSpec POST_V1_TRANSFER_TO_PERSONAL = new RouteSpec("POST", "/v1/transfer/to-personal", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/transfer/to-user} */
    public static final RouteSpec POST_V1_TRANSFER_TO_USER = new RouteSpec("POST", "/v1/transfer/to-user", RouteAuth.PAYOUT, true, false, false, null);

    /** {@code POST /v1/vrcs} */
    public static final RouteSpec POST_V1_VRCS = new RouteSpec("POST", "/v1/vrcs", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/wallet} */
    public static final RouteSpec POST_V1_WALLET = new RouteSpec("POST", "/v1/wallet", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/wallet/block} */
    public static final RouteSpec POST_V1_WALLET_BLOCK = new RouteSpec("POST", "/v1/wallet/block", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/wallet/blocked-address-refund} */
    public static final RouteSpec POST_V1_WALLET_BLOCKED_ADDRESS_REFUND = new RouteSpec("POST", "/v1/wallet/blocked-address-refund", RouteAuth.PAYOUT, false, false, false, null);

    /** {@code POST /v1/wallet/qr} */
    public static final RouteSpec POST_V1_WALLET_QR = new RouteSpec("POST", "/v1/wallet/qr", RouteAuth.PAYMENT, false, true, false, null);

    /** {@code POST /v1/webhooks} */
    public static final RouteSpec POST_V1_WEBHOOKS = new RouteSpec("POST", "/v1/webhooks", RouteAuth.PAYMENT, false, false, false, null);

    /** {@code POST /v1/webhooks/deliveries} */
    public static final RouteSpec POST_V1_WEBHOOKS_DELIVERIES = new RouteSpec("POST", "/v1/webhooks/deliveries", RouteAuth.PAYMENT, false, true, false, ListKind.PAGED);

    /** {@code POST /v1/webhooks/rotate-secret} */
    public static final RouteSpec POST_V1_WEBHOOKS_ROTATE_SECRET = new RouteSpec("POST", "/v1/webhooks/rotate-secret", RouteAuth.PAYOUT, false, false, false, null);

    /** Every route by {@code "METHOD /path"}. */
    public static final Map<String, RouteSpec> ALL;

    static {
        Map<String, RouteSpec> all = new LinkedHashMap<>();
        all.put(POST_V1_API_ALLOWLIST_ADD.key(), POST_V1_API_ALLOWLIST_ADD);
        all.put(POST_V1_API_ALLOWLIST_ENABLE.key(), POST_V1_API_ALLOWLIST_ENABLE);
        all.put(POST_V1_API_ALLOWLIST_LIST.key(), POST_V1_API_ALLOWLIST_LIST);
        all.put(POST_V1_API_ALLOWLIST_REMOVE.key(), POST_V1_API_ALLOWLIST_REMOVE);
        all.put(POST_V1_AUTO_WITHDRAW_DELETE.key(), POST_V1_AUTO_WITHDRAW_DELETE);
        all.put(POST_V1_AUTO_WITHDRAW_LIST.key(), POST_V1_AUTO_WITHDRAW_LIST);
        all.put(POST_V1_AUTO_WITHDRAW_SET.key(), POST_V1_AUTO_WITHDRAW_SET);
        all.put(POST_V1_BALANCE.key(), POST_V1_BALANCE);
        all.put(POST_V1_BATCH_INFO.key(), POST_V1_BATCH_INFO);
        all.put(GET_V1_CLAIM_TOKEN.key(), GET_V1_CLAIM_TOKEN);
        all.put(POST_V1_CLAIM_TOKEN.key(), POST_V1_CLAIM_TOKEN);
        all.put(GET_V1_CURRENCIES.key(), GET_V1_CURRENCIES);
        all.put(GET_V1_DOCUMENTS_BALANCE.key(), GET_V1_DOCUMENTS_BALANCE);
        all.put(GET_V1_DOCUMENTS_BATCH.key(), GET_V1_DOCUMENTS_BATCH);
        all.put(GET_V1_DOCUMENTS_FEES.key(), GET_V1_DOCUMENTS_FEES);
        all.put(POST_V1_DOCUMENTS_JOBS.key(), POST_V1_DOCUMENTS_JOBS);
        all.put(GET_V1_DOCUMENTS_JOBS_FILE.key(), GET_V1_DOCUMENTS_JOBS_FILE);
        all.put(POST_V1_DOCUMENTS_JOBS_INFO.key(), POST_V1_DOCUMENTS_JOBS_INFO);
        all.put(GET_V1_DOCUMENTS_LEDGER.key(), GET_V1_DOCUMENTS_LEDGER);
        all.put(GET_V1_DOCUMENTS_LINK.key(), GET_V1_DOCUMENTS_LINK);
        all.put(GET_V1_DOCUMENTS_REFERRALS.key(), GET_V1_DOCUMENTS_REFERRALS);
        all.put(GET_V1_DOCUMENTS_SPLIT.key(), GET_V1_DOCUMENTS_SPLIT);
        all.put(GET_V1_DOCUMENTS_STATEMENT.key(), GET_V1_DOCUMENTS_STATEMENT);
        all.put(GET_V1_DOCUMENTS_WALLET_STATEMENT.key(), GET_V1_DOCUMENTS_WALLET_STATEMENT);
        all.put(GET_V1_DOCUMENTS_KIND_ID.key(), GET_V1_DOCUMENTS_KIND_ID);
        all.put(POST_V1_EXCHANGE_RATE_LIST.key(), POST_V1_EXCHANGE_RATE_LIST);
        all.put(GET_V1_LINK_ID.key(), GET_V1_LINK_ID);
        all.put(POST_V1_LINK_ID_CHECKOUT.key(), POST_V1_LINK_ID_CHECKOUT);
        all.put(POST_V1_MERCHANTS.key(), POST_V1_MERCHANTS);
        all.put(POST_V1_MERCHANTS_ID_SANDBOX.key(), POST_V1_MERCHANTS_ID_SANDBOX);
        all.put(GET_V1_PAY_ID.key(), GET_V1_PAY_ID);
        all.put(GET_V1_PAY_ID_QR.key(), GET_V1_PAY_ID_QR);
        all.put(POST_V1_PAY_ID_SELECT.key(), POST_V1_PAY_ID_SELECT);
        all.put(POST_V1_PAYMENT.key(), POST_V1_PAYMENT);
        all.put(POST_V1_PAYMENT_ACCEPTED_LIST.key(), POST_V1_PAYMENT_ACCEPTED_LIST);
        all.put(POST_V1_PAYMENT_ACCEPTED_SET.key(), POST_V1_PAYMENT_ACCEPTED_SET);
        all.put(POST_V1_PAYMENT_ACCURACY_GET.key(), POST_V1_PAYMENT_ACCURACY_GET);
        all.put(POST_V1_PAYMENT_ACCURACY_SET.key(), POST_V1_PAYMENT_ACCURACY_SET);
        all.put(POST_V1_PAYMENT_AUTOREFUND_GET.key(), POST_V1_PAYMENT_AUTOREFUND_GET);
        all.put(POST_V1_PAYMENT_AUTOREFUND_SET.key(), POST_V1_PAYMENT_AUTOREFUND_SET);
        all.put(POST_V1_PAYMENT_BATCH.key(), POST_V1_PAYMENT_BATCH);
        all.put(POST_V1_PAYMENT_CANCEL.key(), POST_V1_PAYMENT_CANCEL);
        all.put(POST_V1_PAYMENT_DISCOUNT_LIST.key(), POST_V1_PAYMENT_DISCOUNT_LIST);
        all.put(POST_V1_PAYMENT_DISCOUNT_SET.key(), POST_V1_PAYMENT_DISCOUNT_SET);
        all.put(POST_V1_PAYMENT_FEE_CONFIG_GET.key(), POST_V1_PAYMENT_FEE_CONFIG_GET);
        all.put(POST_V1_PAYMENT_FEE_CONFIG_SET.key(), POST_V1_PAYMENT_FEE_CONFIG_SET);
        all.put(POST_V1_PAYMENT_HISTORY.key(), POST_V1_PAYMENT_HISTORY);
        all.put(POST_V1_PAYMENT_INFO.key(), POST_V1_PAYMENT_INFO);
        all.put(POST_V1_PAYMENT_LINK.key(), POST_V1_PAYMENT_LINK);
        all.put(POST_V1_PAYMENT_LINK_INFO.key(), POST_V1_PAYMENT_LINK_INFO);
        all.put(POST_V1_PAYMENT_LINK_LIST.key(), POST_V1_PAYMENT_LINK_LIST);
        all.put(POST_V1_PAYMENT_LINK_TOGGLE.key(), POST_V1_PAYMENT_LINK_TOGGLE);
        all.put(POST_V1_PAYMENT_QR.key(), POST_V1_PAYMENT_QR);
        all.put(POST_V1_PAYMENT_REFUND.key(), POST_V1_PAYMENT_REFUND);
        all.put(POST_V1_PAYMENT_RESEND.key(), POST_V1_PAYMENT_RESEND);
        all.put(POST_V1_PAYMENT_RESOLVE.key(), POST_V1_PAYMENT_RESOLVE);
        all.put(POST_V1_PAYMENT_SEND_EMAIL.key(), POST_V1_PAYMENT_SEND_EMAIL);
        all.put(POST_V1_PAYMENT_SERVICES.key(), POST_V1_PAYMENT_SERVICES);
        all.put(POST_V1_PAYMENT_TESTING_WEBHOOK.key(), POST_V1_PAYMENT_TESTING_WEBHOOK);
        all.put(POST_V1_PAYOUT.key(), POST_V1_PAYOUT);
        all.put(POST_V1_PAYOUT_APPROVE.key(), POST_V1_PAYOUT_APPROVE);
        all.put(POST_V1_PAYOUT_BATCH.key(), POST_V1_PAYOUT_BATCH);
        all.put(POST_V1_PAYOUT_CALCULATE.key(), POST_V1_PAYOUT_CALCULATE);
        all.put(POST_V1_PAYOUT_CANCEL.key(), POST_V1_PAYOUT_CANCEL);
        all.put(POST_V1_PAYOUT_FEE_CONFIG_GET.key(), POST_V1_PAYOUT_FEE_CONFIG_GET);
        all.put(POST_V1_PAYOUT_FEE_CONFIG_SET.key(), POST_V1_PAYOUT_FEE_CONFIG_SET);
        all.put(POST_V1_PAYOUT_HISTORY.key(), POST_V1_PAYOUT_HISTORY);
        all.put(POST_V1_PAYOUT_INFO.key(), POST_V1_PAYOUT_INFO);
        all.put(POST_V1_PAYOUT_LINK.key(), POST_V1_PAYOUT_LINK);
        all.put(POST_V1_PAYOUT_LINK_BATCH.key(), POST_V1_PAYOUT_LINK_BATCH);
        all.put(POST_V1_PAYOUT_LINK_CANCEL.key(), POST_V1_PAYOUT_LINK_CANCEL);
        all.put(POST_V1_PAYOUT_LINK_CHEQUE.key(), POST_V1_PAYOUT_LINK_CHEQUE);
        all.put(POST_V1_PAYOUT_LINK_INFO.key(), POST_V1_PAYOUT_LINK_INFO);
        all.put(POST_V1_PAYOUT_LINK_LIST.key(), POST_V1_PAYOUT_LINK_LIST);
        all.put(POST_V1_PAYOUT_MASS.key(), POST_V1_PAYOUT_MASS);
        all.put(POST_V1_PAYOUT_REFUND_FEE_CONFIG_GET.key(), POST_V1_PAYOUT_REFUND_FEE_CONFIG_GET);
        all.put(POST_V1_PAYOUT_REFUND_FEE_CONFIG_SET.key(), POST_V1_PAYOUT_REFUND_FEE_CONFIG_SET);
        all.put(POST_V1_PAYOUT_SERVICES.key(), POST_V1_PAYOUT_SERVICES);
        all.put(POST_V1_PAYOUT_VALIDATE.key(), POST_V1_PAYOUT_VALIDATE);
        all.put(POST_V1_REFERRAL_INFO.key(), POST_V1_REFERRAL_INFO);
        all.put(POST_V1_REFUND_BATCH.key(), POST_V1_REFUND_BATCH);
        all.put(POST_V1_SANDBOX_DEPOSIT.key(), POST_V1_SANDBOX_DEPOSIT);
        all.put(POST_V1_SANDBOX_FAUCET.key(), POST_V1_SANDBOX_FAUCET);
        all.put(POST_V1_SANDBOX_RESET.key(), POST_V1_SANDBOX_RESET);
        all.put(GET_V1_SANDBOX_WEBHOOKS.key(), GET_V1_SANDBOX_WEBHOOKS);
        all.put(POST_V1_SANDBOX_WEBHOOKS_REPLAY.key(), POST_V1_SANDBOX_WEBHOOKS_REPLAY);
        all.put(POST_V1_SPLIT_CONFIG_GET.key(), POST_V1_SPLIT_CONFIG_GET);
        all.put(POST_V1_SPLIT_CONFIG_SET.key(), POST_V1_SPLIT_CONFIG_SET);
        all.put(POST_V1_SPLIT_RECIPIENT_OPTIN.key(), POST_V1_SPLIT_RECIPIENT_OPTIN);
        all.put(POST_V1_SPLIT_RECIPIENT_OPTIN_GET.key(), POST_V1_SPLIT_RECIPIENT_OPTIN_GET);
        all.put(POST_V1_SPLIT_RULE.key(), POST_V1_SPLIT_RULE);
        all.put(POST_V1_SPLIT_RULE_DELETE.key(), POST_V1_SPLIT_RULE_DELETE);
        all.put(POST_V1_SPLIT_RULE_LIST.key(), POST_V1_SPLIT_RULE_LIST);
        all.put(POST_V1_TEST_WEBHOOK_PAYMENT.key(), POST_V1_TEST_WEBHOOK_PAYMENT);
        all.put(POST_V1_TEST_WEBHOOK_PAYOUT.key(), POST_V1_TEST_WEBHOOK_PAYOUT);
        all.put(POST_V1_TEST_WEBHOOK_WALLET.key(), POST_V1_TEST_WEBHOOK_WALLET);
        all.put(POST_V1_TRANSFER_BATCH.key(), POST_V1_TRANSFER_BATCH);
        all.put(POST_V1_TRANSFER_TO_PERSONAL.key(), POST_V1_TRANSFER_TO_PERSONAL);
        all.put(POST_V1_TRANSFER_TO_USER.key(), POST_V1_TRANSFER_TO_USER);
        all.put(POST_V1_VRCS.key(), POST_V1_VRCS);
        all.put(POST_V1_WALLET.key(), POST_V1_WALLET);
        all.put(POST_V1_WALLET_BLOCK.key(), POST_V1_WALLET_BLOCK);
        all.put(POST_V1_WALLET_BLOCKED_ADDRESS_REFUND.key(), POST_V1_WALLET_BLOCKED_ADDRESS_REFUND);
        all.put(POST_V1_WALLET_QR.key(), POST_V1_WALLET_QR);
        all.put(POST_V1_WEBHOOKS.key(), POST_V1_WEBHOOKS);
        all.put(POST_V1_WEBHOOKS_DELIVERIES.key(), POST_V1_WEBHOOKS_DELIVERIES);
        all.put(POST_V1_WEBHOOKS_ROTATE_SECRET.key(), POST_V1_WEBHOOKS_ROTATE_SECRET);
        ALL = Collections.unmodifiableMap(all);
    }

    /** Looks a route up by {@code "METHOD /path"}; throws when unknown. */
    public static RouteSpec of(String key) {
        RouteSpec spec = ALL.get(key);
        if (spec == null) throw new IllegalArgumentException("unknown route: " + key);
        return spec;
    }
}
