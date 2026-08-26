package com.oblodai.resources.async;

import com.oblodai.RequestOptions;
import com.oblodai.contract.Routes;
import com.oblodai.contract.requests.DocumentsJobsInfoRequest;
import com.oblodai.contract.requests.DocumentsJobsRequest;
import com.oblodai.core.FileResult;
import com.oblodai.core.Transport;
import com.oblodai.models.DocumentJob;
import com.oblodai.resources.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * The document-job routes of {@link Documents}: {@code POST /v1/documents/jobs},
 * {@code POST /v1/documents/jobs/info} and {@code GET /v1/documents/jobs/file} — queue a report
 * too large to render inline, poll it, then fetch its bytes.
 *
 * <p>It holds no state of its own and adds nothing to the API: it is a base class of
 * {@link Documents} and exists only to keep source files small. Reach every method here through
 * {@code documents()}.
 */
public abstract sealed class DocumentsJobRoutes extends Resource permits Documents {

    /**
     * @param transport the engine to call through
     */
    protected DocumentsJobRoutes(Transport transport) {
        super(transport);
    }

    /**
     * {@code POST /v1/documents/jobs} — queues a large report; poll {@code jobInfo}, then fetch
     * {@code jobFile}.
     *
     * @param request which report, in which language, format and period
     * @return a future of the queued job
     */
    public CompletableFuture<DocumentJob> createJob(DocumentsJobsRequest request) {
        return createJob(request, RequestOptions.none());
    }

    /**
     * {@code POST /v1/documents/jobs}.
     *
     * @param request which report, in which language, format and period
     * @param options per-call options
     * @return a future of the queued job
     */
    public CompletableFuture<DocumentJob> createJob(
            DocumentsJobsRequest request, RequestOptions options) {
        return callAsync(Routes.POST_V1_DOCUMENTS_JOBS, request, options, DocumentJob.class);
    }

    /**
     * {@code POST /v1/documents/jobs/info} — where a queued report has got to.
     *
     * @param jobId the job's id
     * @return a future of the job and its status
     */
    public CompletableFuture<DocumentJob> jobInfo(String jobId) {
        return jobInfo(jobId, RequestOptions.none());
    }

    /**
     * {@code POST /v1/documents/jobs/info}.
     *
     * @param jobId the job's id
     * @param options per-call options
     * @return a future of the job and its status
     */
    public CompletableFuture<DocumentJob> jobInfo(String jobId, RequestOptions options) {
        return callAsync(
                Routes.POST_V1_DOCUMENTS_JOBS_INFO,
                new DocumentsJobsInfoRequest().jobId(jobId),
                options,
                DocumentJob.class);
    }

    /**
     * {@code GET /v1/documents/jobs/file} — the finished job's bytes.
     *
     * @param jobId the job's id
     * @return a future of the rendered document
     */
    public CompletableFuture<FileResult> jobFile(String jobId) {
        return jobFile(jobId, RequestOptions.none());
    }

    /**
     * {@code GET /v1/documents/jobs/file}.
     *
     * @param jobId the job's id
     * @param options per-call options
     * @return a future of the rendered document
     */
    public CompletableFuture<FileResult> jobFile(String jobId, RequestOptions options) {
        return fileAsync(
                Routes.GET_V1_DOCUMENTS_JOBS_FILE, options(options).query("job_id", jobId));
    }
}
