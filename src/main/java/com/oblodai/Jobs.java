package com.oblodai;

import com.oblodai.core.Job;
import com.oblodai.core.JobSupport;
import com.oblodai.core.Transport;
import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.BatchSubmitResponse;
import com.oblodai.generated.models.DocumentJobAccepted;
import com.oblodai.generated.models.DocumentJobView;

/**
 * Waiters for long-running operations (blocking): a batch submitted by {@code batches().create*} or
 * {@code payouts().createTransferBatch}, a document export started by {@code documents().createJob}.
 * Which operations those are is {@link Lro}.
 *
 * <pre>{@code
 * BatchSubmitResponse submitted = oblodai.batches().createPayout(request);
 * BatchInfoResponse done = oblodai.jobs().batch(submitted).waitFor();
 *
 * DocumentJobAccepted accepted = oblodai.documents().createJob(request);
 * FileResult file = oblodai.jobs().document(accepted).download();   // after waitFor()
 * }</pre>
 */
public final class Jobs {

    private final Transport transport;

    Jobs(Transport transport) {
        this.transport = transport;
    }

    /**
     * @param submitted the answer of a batch create call
     * @return its job; {@code waitFor()} polls {@code batches().getInfo}
     */
    public Job<BatchInfoResponse> batch(BatchSubmitResponse submitted) {
        return batch(submitted, null);
    }

    /**
     * @param submitted the answer of a batch create call
     * @param options options for the polls: timeout, retries, headers
     * @return its job; {@code waitFor()} polls {@code batches().getInfo}
     */
    public Job<BatchInfoResponse> batch(BatchSubmitResponse submitted, RequestOptions options) {
        String id = JobSupport.id("createPayoutBatch", submitted);
        return JobSupport.job(transport, "createPayoutBatch", id, submitted, options);
    }

    /**
     * @param batchId a batch's id
     * @return its job
     */
    public Job<BatchInfoResponse> batch(String batchId) {
        return JobSupport.job(transport, "createPayoutBatch", batchId, null, null);
    }

    /**
     * @param accepted the answer of {@code documents().createJob}
     * @return its job; {@code waitFor()} polls {@code documents().getJob}, {@code download()}
     *     fetches the file
     */
    public Job<DocumentJobView> document(DocumentJobAccepted accepted) {
        return document(accepted, null);
    }

    /**
     * @param accepted the answer of {@code documents().createJob}
     * @param options options for the polls and the download: timeout, retries, headers
     * @return its job
     */
    public Job<DocumentJobView> document(DocumentJobAccepted accepted, RequestOptions options) {
        String id = JobSupport.id("createDocumentJob", accepted);
        return JobSupport.job(transport, "createDocumentJob", id, accepted, options);
    }

    /**
     * @param jobId a document job's id
     * @return its job
     */
    public Job<DocumentJobView> document(String jobId) {
        return JobSupport.job(transport, "createDocumentJob", jobId, null, null);
    }
}
