package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.PaymentBatchRequest;
import com.oblodai.contract.requests.PaymentCancelRequest;
import com.oblodai.contract.requests.PaymentHistoryRequest;
import com.oblodai.contract.requests.PaymentInfoRequest;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.contract.requests.PaymentServicesRequest;
import com.oblodai.core.Pager;
import com.oblodai.core.Transport;
import com.oblodai.models.BatchSubmitted;
import com.oblodai.models.Payment;
import com.oblodai.models.ServiceMethod;

/**
 * Invoices: create, look up, cancel, list, and the payer-facing checkout endpoints.
 *
 * <p>Lookups take either the invoice's {@code uuid} or your {@code order_id}: pass the uuid as a
 * string, or a request object carrying whichever you have.
 */
public final class Payments extends PaymentsCheckoutRoutes {

    /**
     * @param transport the engine to call through
     */
    public Payments(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/payment} — creates an invoice. Idempotent by {@code order_id} and by
     * {@code Idempotency-Key}, which the SDK generates when you do not.
     *
     * @param request the invoice to create
     * @return the invoice, in status {@code created} (or {@code select} when no network was pinned)
     */
    public Payment create(PaymentRequest request) {
        return create(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment} — creates an invoice.
     *
     * <p>Errors worth branching on: {@code invoice.bad_price} — the price is not positive;
     * {@code invoice.quote_failed} — the fiat price could not be quoted, so try again;
     * {@code accepted.unknown_method} — the currency and network pair is not one you accept;
     * {@code idempotency.in_progress} — an identical call is still running, retry once it settles;
     * {@code idempotency.key_reused} — the same {@code Idempotency-Key} came back with a different
     * body.
     *
     * @param request the invoice to create
     * @param options per-call options
     * @return the invoice
     */
    public Payment create(PaymentRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT, request, options, Payment.class);
    }

    /**
     * {@code POST /v1/payment/info} — the invoice, including its refunds and refund status.
     *
     * @param uuid the invoice's uuid
     * @return the invoice
     */
    public Payment info(String uuid) {
        return info(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return the invoice
     */
    public Payment info(String uuid, RequestOptions options) {
        return info(new PaymentInfoRequest().uuid(uuid), options);
    }

    /**
     * {@code POST /v1/payment/info} — by {@code uuid} or by {@code order_id}.
     *
     * @param lookup which invoice to read
     * @return the invoice
     */
    public Payment info(PaymentInfoRequest lookup) {
        return info(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info}.
     *
     * @param lookup which invoice to read
     * @param options per-call options
     * @return the invoice
     */
    public Payment info(PaymentInfoRequest lookup, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_INFO, lookup, options, Payment.class);
    }

    /**
     * Alias of {@link #info(String)}.
     *
     * @param uuid the invoice's uuid
     * @return the invoice
     */
    public Payment get(String uuid) {
        return get(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info} — alias of {@link #info(String, RequestOptions)}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return the invoice
     */
    public Payment get(String uuid, RequestOptions options) {
        return info(uuid, options);
    }

    /**
     * Alias of {@link #info(PaymentInfoRequest)}.
     *
     * @param lookup which invoice to read
     * @return the invoice
     */
    public Payment get(PaymentInfoRequest lookup) {
        return get(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/info} — alias of {@link #info(PaymentInfoRequest, RequestOptions)}.
     *
     * @param lookup which invoice to read
     * @param options per-call options
     * @return the invoice
     */
    public Payment get(PaymentInfoRequest lookup, RequestOptions options) {
        return info(lookup, options);
    }

    /**
     * {@code POST /v1/payment/cancel} — cancels an unpaid invoice. Once a deposit has been seen the
     * gateway answers 409 {@code invoice.not_payable}.
     *
     * @param uuid the invoice's uuid
     * @return the cancelled invoice
     */
    public Payment cancel(String uuid) {
        return cancel(uuid, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/cancel}.
     *
     * @param uuid the invoice's uuid
     * @param options per-call options
     * @return the cancelled invoice
     */
    public Payment cancel(String uuid, RequestOptions options) {
        return cancel(new PaymentCancelRequest().uuid(uuid), options);
    }

    /**
     * {@code POST /v1/payment/cancel}.
     *
     * @param lookup which invoice to cancel
     * @return the cancelled invoice
     */
    public Payment cancel(PaymentCancelRequest lookup) {
        return cancel(lookup, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/cancel}.
     *
     * @param lookup which invoice to cancel
     * @param options per-call options
     * @return the cancelled invoice
     */
    public Payment cancel(PaymentCancelRequest lookup, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_CANCEL, lookup, options, Payment.class);
    }

    /**
     * {@code POST /v1/payment/history} — newest first. Read one page, iterate, or stream.
     *
     * @return a lazy pager over the merchant's invoices
     */
    public Pager<Payment> history() {
        return history(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history}.
     *
     * @param options per-call options
     * @return a lazy pager over the merchant's invoices
     */
    public Pager<Payment> history(RequestOptions options) {
        return history(new PaymentHistoryRequest(), options);
    }

    /**
     * {@code POST /v1/payment/history}.
     *
     * @param params filters and page bounds
     * @return a lazy pager over the matching invoices
     */
    public Pager<Payment> history(PaymentHistoryRequest params) {
        return history(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history}.
     *
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy pager over the matching invoices
     */
    public Pager<Payment> history(PaymentHistoryRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYMENT_HISTORY, params, options, Payment.class);
    }

    /**
     * Alias of {@link #history()}.
     *
     * @return a lazy pager over the merchant's invoices
     */
    public Pager<Payment> list() {
        return list(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history} — alias of {@link #history(RequestOptions)}.
     *
     * @param options per-call options
     * @return a lazy pager over the merchant's invoices
     */
    public Pager<Payment> list(RequestOptions options) {
        return history(options);
    }

    /**
     * Alias of {@link #history(PaymentHistoryRequest)}.
     *
     * @param params filters and page bounds
     * @return a lazy pager over the matching invoices
     */
    public Pager<Payment> list(PaymentHistoryRequest params) {
        return list(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/history} — alias of
     * {@link #history(PaymentHistoryRequest, RequestOptions)}.
     *
     * @param params filters and page bounds
     * @param options per-call options
     * @return a lazy pager over the matching invoices
     */
    public Pager<Payment> list(PaymentHistoryRequest params, RequestOptions options) {
        return history(params, options);
    }

    /**
     * {@code POST /v1/payment/batch} — creates up to 5000 invoices asynchronously; follow it with
     * {@code batches().info(...)}.
     *
     * @param request the invoices to create
     * @return the batch ticket
     */
    public BatchSubmitted batch(PaymentBatchRequest request) {
        return batch(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/batch}.
     *
     * @param request the invoices to create
     * @param options per-call options
     * @return the batch ticket
     */
    public BatchSubmitted batch(PaymentBatchRequest request, RequestOptions options) {
        return call(Routes.POST_V1_PAYMENT_BATCH, request, options, BatchSubmitted.class);
    }

    /**
     * {@code POST /v1/payment/services} — the currency and network pairs deposits are accepted in,
     * with their limits and fees.
     *
     * @return a lazy pager over the accepted methods
     */
    public Pager<ServiceMethod> services() {
        return services(RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/services}.
     *
     * @param options per-call options
     * @return a lazy pager over the accepted methods
     */
    public Pager<ServiceMethod> services(RequestOptions options) {
        return services(new PaymentServicesRequest(), options);
    }

    /**
     * {@code POST /v1/payment/services}.
     *
     * @param params page bounds
     * @return a lazy pager over the accepted methods
     */
    public Pager<ServiceMethod> services(PaymentServicesRequest params) {
        return services(params, RequestOptions.none());
    }

    /**
     * {@code POST /v1/payment/services}.
     *
     * @param params page bounds
     * @param options per-call options
     * @return a lazy pager over the accepted methods
     */
    public Pager<ServiceMethod> services(PaymentServicesRequest params, RequestOptions options) {
        return pager(Routes.POST_V1_PAYMENT_SERVICES, params, options, ServiceMethod.class);
    }
}
