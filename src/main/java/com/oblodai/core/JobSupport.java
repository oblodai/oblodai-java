package com.oblodai.core;

import com.oblodai.Lro;
import com.oblodai.RequestOptions;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.JobTimeoutException;
import com.oblodai.generated.Routes;
import com.oblodai.generated.models.WireObject;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Builds {@link Job}s and {@link AsyncJob}s from the {@link Lro} table. */
public final class JobSupport {

    /** Statuses after which a job no longer changes. */
    static final Set<String> TERMINAL = Lro.TERMINAL_STATUSES;

    private JobSupport() {}

    static JobTimeoutException timeout(String id, Duration timeout, String status) {
        return new JobTimeoutException(
                "job "
                        + id
                        + " is still "
                        + (status == null || status.isEmpty() ? "unfinished" : status)
                        + " after "
                        + timeout);
    }

    /**
     * @param answer a poll answer
     * @return its {@code status}, or an empty string
     */
    public static String status(Object answer) {
        Object json = answer instanceof WireObject w ? w.toJson() : answer;
        Object status = json instanceof Map<?, ?> map ? map.get("status") : null;
        return status == null ? "" : String.valueOf(status);
    }

    /**
     * @param createOperationId the create operation, a key of {@link Lro#LRO}
     * @return how to follow its jobs
     */
    static Lro.Poll poll(String createOperationId) {
        String pollId = Lro.LRO.get(createOperationId);
        if (pollId == null) {
            throw new ConfigException(
                    "sdk.lro_unresolved",
                    createOperationId + " is not a long-running operation",
                    null);
        }
        return Lro.POLLS.get(pollId);
    }

    private static RouteSpec route(String operationId) {
        RouteSpec route = Routes.BY_OPERATION_ID.get(operationId);
        if (route == null) {
            throw new ConfigException(
                    "sdk.lro_unresolved",
                    "no route for operation " + operationId + ", needed to follow a long-running call",
                    null);
        }
        return route;
    }

    /**
     * @param createOperationId the create operation
     * @param answer its answer (a model or a JSON tree)
     * @return the job's id
     */
    public static String id(String createOperationId, Object answer) {
        Lro.Poll poll = poll(createOperationId);
        Object json = answer instanceof WireObject w ? w.toJson() : answer;
        Object value = json instanceof Map<?, ?> map ? map.get(poll.idField()) : null;
        if (value == null || String.valueOf(value).isEmpty()) {
            throw new ContractException(
                    "long-running call answered without " + poll.idField(), 200, json);
        }
        return String.valueOf(value);
    }

    /**
     * A blocking job.
     *
     * @param transport the engine
     * @param createOperationId the create operation, a key of {@link Lro#LRO}
     * @param id the job's id
     * @param result the create answer, or null
     * @param options options for the polls (their idempotency key is dropped)
     * @param <T> the poll answer
     * @return the job
     */
    @SuppressWarnings("unchecked")
    public static <T> Job<T> job(
            Transport transport, String createOperationId, String id, Object result, RequestOptions options) {
        Lro.Poll poll = poll(createOperationId);
        RouteSpec pollRoute = route(Lro.LRO.get(createOperationId));
        RouteSpec downloadRoute = poll.download() == null ? null : route(poll.download());
        RequestOptions follow = options == null ? null : options.withoutIdempotencyKey().requestId(null);
        Function<Object, T> parse = (Function<Object, T>) poll.parse();
        return new Job<>(
                id,
                result,
                () ->
                        Calls.parse(
                                pollRoute,
                                transport.call(
                                        pollRoute,
                                        Calls.callOptions(null, null, Map.of(poll.idField(), id), follow)),
                                parse),
                JobSupport::status,
                downloadRoute == null
                        ? null
                        : () ->
                                Calls.fileResult(
                                        transport.callRaw(
                                                downloadRoute,
                                                Calls.callOptions(
                                                        null, Map.of(poll.idField(), id), null, follow))),
                transport.config().sleeper());
    }

    /**
     * A non-blocking job.
     *
     * @param transport the engine
     * @param createOperationId the create operation, a key of {@link Lro#LRO}
     * @param id the job's id
     * @param result the create answer, or null
     * @param options options for the polls (their idempotency key is dropped)
     * @param <T> the poll answer
     * @return the job
     */
    @SuppressWarnings("unchecked")
    public static <T> AsyncJob<T> asyncJob(
            Transport transport, String createOperationId, String id, Object result, RequestOptions options) {
        Lro.Poll poll = poll(createOperationId);
        RouteSpec pollRoute = route(Lro.LRO.get(createOperationId));
        RouteSpec downloadRoute = poll.download() == null ? null : route(poll.download());
        RequestOptions follow = options == null ? null : options.withoutIdempotencyKey().requestId(null);
        Function<Object, T> parse = (Function<Object, T>) poll.parse();
        return new AsyncJob<>(
                id,
                result,
                () ->
                        transport
                                .callAsync(
                                        pollRoute,
                                        Calls.callOptions(null, null, Map.of(poll.idField(), id), follow))
                                .thenApply(answer -> Calls.parse(pollRoute, answer, parse)),
                JobSupport::status,
                downloadRoute == null
                        ? null
                        : () ->
                                transport
                                        .callRawAsync(
                                                downloadRoute,
                                                Calls.callOptions(
                                                        null, Map.of(poll.idField(), id), null, follow))
                                        .thenApply(Calls::fileResult),
                transport.config().sleeper());
    }
}
