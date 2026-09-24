package com.oblodai;

import com.oblodai.core.AsyncJob;
import com.oblodai.core.JobSupport;
import com.oblodai.core.Transport;
import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.BatchSubmitResponse;
import com.oblodai.generated.models.DocumentJobAccepted;
import com.oblodai.generated.models.DocumentJobView;

/**
 * Waiters for long-running operations (non-blocking): a batch submitted by {@code batches().create*} or
 * {@code payouts().createTransferBatch}, a document export started by {@code documents().createJob}.
 * Which operations those are is {@link Lro}; a typed waiter finds its own there by the model its
 * poll answers with ({@link JobSupport#createOf}), naming no operation.
 *
 * <pre>{@code
 * BatchSubmitResponse submitted = oblodai.batches().createPayout(request);
 * BatchInfoResponse done = oblodai.jobs().batch(submitted).waitFor();
 *
 * DocumentJobAccepted accepted = oblodai.documents().createJob(request);
 * FileResult file = oblodai.jobs().document(accepted).download();   // after waitFor()
 * }</pre>
 */
public final class AsyncJobs {

    private final Transport transport;

    AsyncJobs(Transport transport) {
        this.transport = transport;
    }

    /**
     * @param submitted the answer of a batch create call
     * @return its job; {@code waitFor()} polls {@code batches().getInfo}
     */
    public AsyncJob<BatchInfoResponse> batch(BatchSubmitResponse submitted) {
        return batch(submitted, null);
    }

    /**
     * @param submitted the answer of a batch create call
     * @param options options for the polls: timeout, retries, headers
     * @return its job; {@code waitFor()} polls {@code batches().getInfo}
     */
    public AsyncJob<BatchInfoResponse> batch(BatchSubmitResponse submitted, RequestOptions options) {
        String create = JobSupport.createOf(BatchInfoResponse.class);
        String id = JobSupport.id(create, submitted);
        return JobSupport.asyncJob(transport, create, id, submitted, options);
    }

    /**
     * @param batchId a batch's id
     * @return its job
     */
    public AsyncJob<BatchInfoResponse> batch(String batchId) {
        return JobSupport.asyncJob(transport, JobSupport.createOf(BatchInfoResponse.class), batchId, null, null);
    }

    /**
     * @param accepted the answer of {@code documents().createJob}
     * @return its job; {@code waitFor()} polls {@code documents().getJob}, {@code download()}
     *     fetches the file
     */
    public AsyncJob<DocumentJobView> document(DocumentJobAccepted accepted) {
        return document(accepted, null);
    }

    /**
     * @param accepted the answer of {@code documents().createJob}
     * @param options options for the polls and the download: timeout, retries, headers
     * @return its job
     */
    public AsyncJob<DocumentJobView> document(DocumentJobAccepted accepted, RequestOptions options) {
        String create = JobSupport.createOf(DocumentJobView.class);
        String id = JobSupport.id(create, accepted);
        return JobSupport.asyncJob(transport, create, id, accepted, options);
    }

    /**
     * @param jobId a document job's id
     * @return its job
     */
    public AsyncJob<DocumentJobView> document(String jobId) {
        return JobSupport.asyncJob(transport, JobSupport.createOf(DocumentJobView.class), jobId, null, null);
    }

    /**
     * Any long-running call of the contract, by its create {@code operationId} - a key of {@link
     * com.oblodai.generated.Facts#LRO}, including one newer than the typed waiters above.
     *
     * @param createOperationId the create operation, a key of {@link com.oblodai.generated.Facts#LRO}
     * @param answer the create call's answer
     * @return its job; {@code waitFor()} returns the poll answer as its generated model
     * @throws com.oblodai.errors.ConfigException {@code sdk.lro_unresolved} when the operation is
     *     not long-running
     */
    public AsyncJob<Object> follow(String createOperationId, Object answer) {
        return follow(createOperationId, answer, null);
    }

    /**
     * @param createOperationId the create operation, a key of {@link com.oblodai.generated.Facts#LRO}
     * @param answer the create call's answer
     * @param options options for the polls and the download: timeout, retries, headers
     * @return its job
     * @throws com.oblodai.errors.ConfigException {@code sdk.lro_unresolved} when the operation is
     *     not long-running
     */
    public AsyncJob<Object> follow(String createOperationId, Object answer, RequestOptions options) {
        String id = JobSupport.id(createOperationId, answer);
        return JobSupport.asyncJob(transport, createOperationId, id, answer, options);
    }
}
