package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.AsyncJob;
import com.oblodai.core.FileResult;
import com.oblodai.core.Job;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.JobTimeoutException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.Routes;
import com.oblodai.generated.SigningProtocol;
import com.oblodai.generated.models.BatchInfoResponse;
import com.oblodai.generated.models.BatchStatus;
import com.oblodai.generated.models.BatchSubmitResponse;
import com.oblodai.generated.models.DocumentJobAccepted;
import com.oblodai.generated.models.DocumentJobKind;
import com.oblodai.generated.models.DocumentJobRequest;
import com.oblodai.generated.models.DocumentJobView;
import com.oblodai.generated.models.PayoutBatchRequest;
import com.oblodai.generated.models.PayoutRequest;
import com.oblodai.support.Clients;
import com.oblodai.support.Fixtures;
import com.oblodai.support.MockHttpClient;
import com.oblodai.core.JobSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Spec §3.8: long-running operations - batches and document exports - have a waiter. */
class JobsTest {

    private static PayoutBatchRequest batch() {
        return PayoutBatchRequest.builder()
                .payouts(
                        List.of(
                                PayoutRequest.builder()
                                        .amount("10")
                                        .currency("USDT")
                                        .address("T")
                                        .orderId("o-1")
                                        .build()))
                .build();
    }

    @Test
    void aBatchIsFollowedUntilItsStatusIsTerminal() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.batchSubmitted())
                        .ok(Fixtures.batchInfo("pending"))
                        .ok(Fixtures.batchInfo("processing"))
                        .ok(Fixtures.batchInfo("completed"));
        Clients.RecordingSleeper sleeper = new Clients.RecordingSleeper();
        Oblodai oblodai = Clients.builder(http, sleeper).build();

        BatchSubmitResponse submitted = oblodai.batches().createPayout(batch());
        Job<BatchInfoResponse> job = oblodai.jobs().batch(submitted);
        assertEquals("b1", job.id());
        assertEquals(submitted, job.result());

        BatchInfoResponse done = job.waitFor(Duration.ofMinutes(1), Duration.ofMillis(250));

        assertEquals(BatchStatus.COMPLETED, done.status());
        assertEquals(4, http.calls().size());
        assertEquals("https://api.test/v1/batch/info", http.calls().get(1).uri().toString());
        assertTrue(http.calls().get(1).body().contains("\"batch_id\":\"b1\""));
        assertEquals(List.of(250L, 250L), sleeper.pauses, "a pause between polls, none after the last");
        assertEquals(null, http.calls().get(1).header(SigningProtocol.REQUEST_HEADER_IDEMPOTENCY_KEY), "polls carry no key");
    }

    @Test
    void aJobThatDoesNotFinishInTimeIsAnError() {
        MockHttpClient http = new MockHttpClient().ok(Fixtures.batchInfo("processing"));
        Job<BatchInfoResponse> job = Clients.client(http).jobs().batch("b1");
        JobTimeoutException error =
                assertThrows(JobTimeoutException.class, () -> job.waitFor(Duration.ZERO, Duration.ofMillis(1)));
        assertEquals("sdk.job_timeout", error.code());
        assertTrue(error.getMessage().contains("processing"), error.getMessage());
    }

    @Test
    void aDocumentJobIsWaitedForAndDownloaded() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.documentAccepted())
                        .ok(Fixtures.documentJob("processing"))
                        .ok(Fixtures.documentJob("done"))
                        .raw(200, "%PDF", "content-type", "application/pdf");
        Oblodai oblodai = Clients.client(http);

        DocumentJobAccepted accepted =
                oblodai.documents().createJob(DocumentJobRequest.builder().kind(DocumentJobKind.STATEMENT).build());
        Job<DocumentJobView> job = oblodai.jobs().document(accepted);
        assertEquals("done", job.waitFor().status().value());
        FileResult file = job.download();

        assertEquals("application/pdf", file.contentType());
        assertEquals("https://api.test/v1/documents/jobs/file?job_id=j1", http.calls().get(3).uri().toString());
        assertThrows(UnsupportedOperationException.class, () -> oblodai.jobs().batch("b1").download());
    }

    @Test
    void theAsyncWaiterPollsWithoutBlocking() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.batchSubmitted())
                        .ok(Fixtures.batchInfo("processing"))
                        .ok(Fixtures.batchInfo("stopped"));
        OblodaiAsync async = Clients.client(http).async();

        AsyncJob<BatchInfoResponse> job = async.jobs().batch(async.batches().createPayout(batch()).join());
        assertEquals(BatchStatus.STOPPED, job.waitFor().join().status(), "stopped is terminal too");
        assertEquals(3, http.calls().size());
    }

    @Test
    void theLroTableNamesRealRoutes() {
        for (var entry : Lro.LRO.entrySet()) {
            assertTrue(Routes.BY_OPERATION_ID.containsKey(entry.getKey()), entry.getKey());
            assertTrue(Routes.BY_OPERATION_ID.containsKey(entry.getValue()), entry.getValue());
            Lro.Poll poll = Lro.POLLS.get(entry.getValue());
            if (poll.download() != null) {
                assertTrue(Routes.BY_OPERATION_ID.get(poll.download()).bare(), poll.download());
            }
        }
    }

    @Test
    void theLroTableIsTheContracts() {
        assertEquals(Facts.LRO.keySet(), Lro.LRO.keySet(), "the runtime keeps no list of its own");
        for (var entry : Facts.LRO.entrySet()) {
            assertEquals(entry.getValue().operation(), Lro.LRO.get(entry.getKey()));
            assertTrue(Lro.TERMINAL_STATUSES.containsAll(entry.getValue().terminal()), entry.getKey());
        }
        Set<String> union = new TreeSet<>();
        Facts.LRO.values().forEach(p -> union.addAll(p.terminal()));
        assertEquals(union, new TreeSet<>(Lro.TERMINAL_STATUSES));
    }

    @Test
    void aJobStopsOnlyAtItsOwnTerminalStatuses() {
        // "completed" ends a batch, not a document job: each job waits for its own statuses.
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.documentJob("completed"))
                        .ok(Fixtures.documentJob("expired"));
        Job<DocumentJobView> job = Clients.client(http).jobs().document("j1");
        assertEquals("expired", job.waitFor(Duration.ofMinutes(1), Duration.ZERO).status().value());
        assertEquals(2, http.calls().size());
    }

    @Test
    void anyLongRunningCallIsFollowedByItsOperationId() {
        MockHttpClient http =
                new MockHttpClient()
                        .ok(Fixtures.batchSubmitted())
                        .ok(Fixtures.batchInfo("processing"))
                        .ok(Fixtures.batchInfo("completed"))
                        .ok(Fixtures.batchInfo("stopped"));
        Oblodai oblodai = Clients.client(http);

        BatchSubmitResponse submitted = oblodai.batches().createPayout(batch());
        Job<?> job = oblodai.jobs().follow("createPayoutBatch", submitted);
        assertEquals("b1", job.id());
        Object done = job.waitFor(Duration.ofMinutes(1), Duration.ZERO);
        assertTrue(done instanceof BatchInfoResponse info && info.status() == BatchStatus.COMPLETED, String.valueOf(done));
        assertEquals(3, http.calls().size());

        AsyncJob<?> async = oblodai.async().jobs().follow("createPayoutBatch", submitted);
        assertEquals(BatchStatus.STOPPED, ((BatchInfoResponse) async.waitFor().join()).status());

        ConfigException notLro =
                assertThrows(ConfigException.class, () -> oblodai.jobs().follow("getBatchInfo", submitted));
        assertEquals("sdk.lro_unresolved", notLro.code());
    }

    @Test
    void aTypedWaiterFindsItsOperationByThePollModel() {
        String batch = JobSupport.createOf(BatchInfoResponse.class);
        assertEquals(BatchInfoResponse.class, Facts.LRO.get(batch).model());
        String document = JobSupport.createOf(DocumentJobView.class);
        assertEquals(DocumentJobView.class, Facts.LRO.get(document).model());
        ConfigException none = assertThrows(ConfigException.class, () -> JobSupport.createOf(String.class));
        assertEquals("sdk.lro_unresolved", none.code());
    }

    @Test
    void theTypedWaitersNameNoOperation() throws IOException {
        for (String file : List.of("Jobs.java", "AsyncJobs.java")) {
            String src = Files.readString(Path.of("src/main/java/com/oblodai", file));
            for (String operationId : Routes.BY_OPERATION_ID.keySet()) {
                assertTrue(
                        !src.contains("\"" + operationId + "\""),
                        file + " names the operation " + operationId + ": resolve it from Facts.LRO by the poll model");
            }
        }
    }
}
