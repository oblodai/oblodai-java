package com.oblodai.resources;

import com.oblodai.RequestOptions;
import com.oblodai.contract.RouteSpec;
import com.oblodai.contract.Routes;
import com.oblodai.core.FileResult;
import com.oblodai.core.Transport;

/**
 * Generated PDF and CSV documents. Every report method answers with the bytes themselves
 * ({@link FileResult}); a range too large to render inline goes through an asynchronous job
 * ({@code createJob} then {@code jobInfo} then {@code jobFile}). Payment key.
 *
 * <p>What each document accepts — language, format, period — is carried by {@link DocumentQuery}.
 */
public final class Documents extends DocumentsJobRoutes {

    /**
     * @param transport the engine to call through
     */
    public Documents(Transport transport) {
        super(transport);
    }

    /**
     * {@code GET /v1/documents/statement} — account statement for a period (PDF or CSV).
     *
     * @return the rendered document
     */
    public FileResult statement() {
        return statement(RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/statement}.
     *
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult statement(RequestOptions options) {
        return statement(new DocumentQuery(), options);
    }

    /**
     * {@code GET /v1/documents/statement}.
     *
     * @param query language, format and period
     * @return the rendered document
     */
    public FileResult statement(DocumentQuery query) {
        return statement(query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/statement}.
     *
     * @param query language, format and period
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult statement(DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_STATEMENT, query, options);
    }

    /**
     * {@code GET /v1/documents/balance} — balance certificate (PDF).
     *
     * @return the rendered document
     */
    public FileResult balanceCertificate() {
        return balanceCertificate(RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/balance}.
     *
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult balanceCertificate(RequestOptions options) {
        return balanceCertificate(new DocumentQuery(), options);
    }

    /**
     * {@code GET /v1/documents/balance}.
     *
     * @param query language
     * @return the rendered document
     */
    public FileResult balanceCertificate(DocumentQuery query) {
        return balanceCertificate(query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/balance}.
     *
     * @param query language
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult balanceCertificate(DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_BALANCE, query, options);
    }

    /**
     * {@code GET /v1/documents/fees} — the fee schedule in force for the merchant (PDF).
     *
     * @return the rendered document
     */
    public FileResult feeSchedule() {
        return feeSchedule(RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/fees}.
     *
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult feeSchedule(RequestOptions options) {
        return feeSchedule(new DocumentQuery(), options);
    }

    /**
     * {@code GET /v1/documents/fees}.
     *
     * @param query language
     * @return the rendered document
     */
    public FileResult feeSchedule(DocumentQuery query) {
        return feeSchedule(query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/fees}.
     *
     * @param query language
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult feeSchedule(DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_FEES, query, options);
    }

    /**
     * {@code GET /v1/documents/ledger} — full ledger export for a period (PDF or CSV).
     *
     * @return the rendered document
     */
    public FileResult ledger() {
        return ledger(RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/ledger}.
     *
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult ledger(RequestOptions options) {
        return ledger(new DocumentQuery(), options);
    }

    /**
     * {@code GET /v1/documents/ledger}.
     *
     * @param query language, format and period
     * @return the rendered document
     */
    public FileResult ledger(DocumentQuery query) {
        return ledger(query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/ledger}.
     *
     * @param query language, format and period
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult ledger(DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_LEDGER, query, options);
    }

    /**
     * {@code GET /v1/documents/referrals} — referral earnings report.
     *
     * @return the rendered document
     */
    public FileResult referralsReport() {
        return referralsReport(RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/referrals}.
     *
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult referralsReport(RequestOptions options) {
        return referralsReport(new DocumentQuery(), options);
    }

    /**
     * {@code GET /v1/documents/referrals}.
     *
     * @param query language, format and period
     * @return the rendered document
     */
    public FileResult referralsReport(DocumentQuery query) {
        return referralsReport(query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/referrals}.
     *
     * @param query language, format and period
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult referralsReport(DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_REFERRALS, query, options);
    }

    /**
     * {@code GET /v1/documents/split} — how one payment was split between partners (PDF).
     *
     * @param paymentUuid the invoice's uuid, sent as {@code uuid}
     * @param query language
     * @return the rendered document
     */
    public FileResult splitReport(String paymentUuid, DocumentQuery query) {
        return splitReport(paymentUuid, query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/split}.
     *
     * @param paymentUuid the invoice's uuid, sent as {@code uuid}
     * @param query language
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult splitReport(String paymentUuid, DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_SPLIT, query, options, paymentUuid);
    }

    /**
     * {@code GET /v1/documents/batch} — per-row report of an asynchronous batch.
     *
     * @param batchId the batch's id, sent as {@code uuid}
     * @param query language and format
     * @return the rendered document
     */
    public FileResult batchReport(String batchId, DocumentQuery query) {
        return batchReport(batchId, query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/batch}.
     *
     * @param batchId the batch's id, sent as {@code uuid}
     * @param query language and format
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult batchReport(String batchId, DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_BATCH, query, options, batchId);
    }

    /**
     * {@code GET /v1/documents/link} — payment-link report, covering the link's invoices.
     *
     * @param linkId the link's id, sent as {@code uuid}
     * @param query language and format
     * @return the rendered document
     */
    public FileResult linkReport(String linkId, DocumentQuery query) {
        return linkReport(linkId, query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/link}.
     *
     * @param linkId the link's id, sent as {@code uuid}
     * @param query language and format
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult linkReport(String linkId, DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_LINK, query, options, linkId);
    }

    /**
     * {@code GET /v1/documents/wallet/statement} — static-wallet statement.
     *
     * @param walletUuid the wallet's uuid, sent as {@code uuid}
     * @param query language, format and period
     * @return the rendered document
     */
    public FileResult walletStatement(String walletUuid, DocumentQuery query) {
        return walletStatement(walletUuid, query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/wallet/statement}.
     *
     * @param walletUuid the wallet's uuid, sent as {@code uuid}
     * @param query language, format and period
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult walletStatement(
            String walletUuid, DocumentQuery query, RequestOptions options) {
        return report(Routes.GET_V1_DOCUMENTS_WALLET_STATEMENT, query, options, walletUuid);
    }

    /**
     * {@code GET /v1/documents/&#123;kind&#125;/&#123;id&#125;} — a public document by its signed
     * link. {@code exp} and {@code sig} come from a {@code document_url} the gateway handed you; no
     * credentials are needed, and fetching that URL directly is usually simpler.
     *
     * @param kind the document's kind, from the link's path
     * @param id the document's id, from the link's path
     * @param exp the link's expiry, from its {@code exp} parameter
     * @param sig the link's signature, from its {@code sig} parameter
     * @param query language and format
     * @return the rendered document
     */
    public FileResult download(String kind, String id, long exp, String sig, DocumentQuery query) {
        return download(kind, id, exp, sig, query, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/&#123;kind&#125;/&#123;id&#125;}.
     *
     * @param kind the document's kind, from the link's path
     * @param id the document's id, from the link's path
     * @param exp the link's expiry, from its {@code exp} parameter
     * @param sig the link's signature, from its {@code sig} parameter
     * @param query language and format
     * @param options per-call options
     * @return the rendered document
     */
    public FileResult download(
            String kind,
            String id,
            long exp,
            String sig,
            DocumentQuery query,
            RequestOptions options) {
        return file(
                Routes.GET_V1_DOCUMENTS_KIND_ID,
                options(options)
                        .pathParam("kind", kind)
                        .pathParam("id", id)
                        .query(DocumentQuery.orEmpty(query).toQuery())
                        .query("exp", exp)
                        .query("sig", sig));
    }

    private FileResult report(RouteSpec route, DocumentQuery query, RequestOptions options) {
        return file(route, options(options).query(DocumentQuery.orEmpty(query).toQuery()));
    }

    private FileResult report(
            RouteSpec route, DocumentQuery query, RequestOptions options, String uuid) {
        return file(
                route,
                options(options).query(DocumentQuery.orEmpty(query).toQuery()).query("uuid", uuid));
    }
}
