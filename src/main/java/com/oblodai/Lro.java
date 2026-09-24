package com.oblodai;

import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.DocumentJobView;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Which operations are long-running, and how to follow them - a decision of this SDK, not of the
 * API (Ruling 3): the generator knows nothing of this table. A create call listed in {@link #LRO}
 * answers with an acknowledgement; {@link Oblodai#jobs()} turns it into a {@link com.oblodai.core.Job}
 * whose {@code waitFor()} polls the operation named here until the status is terminal.
 */
public final class Lro {

    private Lro() {}

    /** {@code create operationId -> poll operationId}. */
    public static final Map<String, String> LRO =
            Map.of(
                    "createPaymentBatch", "getBatchInfo",
                    "createPayoutBatch", "getBatchInfo",
                    "createRefundBatch", "getBatchInfo",
                    "createTransferBatch", "getBatchInfo",
                    "createDocumentJob", "getDocumentJob");

    /**
     * How to follow one kind of job.
     *
     * @param idField the job's id in the create answer; sent under the same name to the poll (and
     *     the download)
     * @param parse the parser of the poll answer
     * @param download the {@code operationId} that returns the finished job's file, or null
     */
    public record Poll(String idField, Function<Object, ?> parse, String download) {}

    /** {@code poll operationId -> how to follow it}. */
    public static final Map<String, Poll> POLLS =
            Map.of(
                    "getBatchInfo", new Poll("batch_id", BatchInfoResponse::fromJson, null),
                    "getDocumentJob",
                    new Poll("job_id", DocumentJobView::fromJson, "downloadDocumentJobFile"));

    /**
     * Statuses after which a job no longer changes: a batch ends {@code completed} or {@code
     * stopped} ({@code on_error=stop}), a document job {@code done}, {@code failed} or {@code
     * expired}.
     */
    public static final Set<String> TERMINAL_STATUSES =
            Set.of("completed", "stopped", "done", "failed", "expired");
}
