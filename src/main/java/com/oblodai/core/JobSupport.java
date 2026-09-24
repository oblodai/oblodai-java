package com.oblodai.core;

import com.oblodai.RequestOptions;
import com.oblodai.errors.ConfigException;
import com.oblodai.errors.ContractException;
import com.oblodai.errors.JobTimeoutException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.Routes;
import com.oblodai.generated.models.WireObject;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds {@link Job}s and {@link AsyncJob}s from the contract's long-running operations ({@link
 * Facts#LRO}, generated from {@code x-sdk-poll}): the poll, the id and status fields, the terminal
 * statuses and the download all come from there.
 */
public final class JobSupport {

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
        return field(answer, "status");
    }

    private static String field(Object answer, String name) {
        Object json = answer instanceof WireObject w ? w.toJson() : answer;
        Object value = json instanceof Map<?, ?> map ? map.get(name) : null;
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * @param createOperationId the create operation, a key of {@link Facts#LRO}
     * @return how to follow its jobs
     * @throws ConfigException {@code sdk.lro_unresolved} when the operation is not long-running
     */
    static Facts.Poll poll(String createOperationId) {
        Facts.Poll poll = createOperationId == null ? null : Facts.LRO.get(createOperationId);
        if (poll == null) {
            throw new ConfigException(
                    "sdk.lro_unresolved",
                    createOperationId + " is not a long-running operation",
                    null);
        }
        return poll;
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
        Facts.Poll poll = poll(createOperationId);
        String value = field(answer, poll.idField());
        if (value.isEmpty()) {
            Object json = answer instanceof WireObject w ? w.toJson() : answer;
            throw new ContractException("long-running call answered without " + poll.idField(), 200, json);
        }
        return value;
    }

    /**
     * A blocking job.
     *
     * @param transport the engine
     * @param createOperationId the create operation, a key of {@link Facts#LRO}
     * @param id the job's id
     * @param result the create answer, or null
     * @param options options for the polls (their idempotency key is dropped)
     * @param <T> the poll answer
     * @return the job
     */
    @SuppressWarnings("unchecked")
    public static <T> Job<T> job(
            Transport transport, String createOperationId, String id, Object result, RequestOptions options) {
        Facts.Poll poll = poll(createOperationId);
        RouteSpec pollRoute = route(poll.operation());
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
                answer -> field(answer, poll.statusField()),
                poll.terminal(),
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
     * @param createOperationId the create operation, a key of {@link Facts#LRO}
     * @param id the job's id
     * @param result the create answer, or null
     * @param options options for the polls (their idempotency key is dropped)
     * @param <T> the poll answer
     * @return the job
     */
    @SuppressWarnings("unchecked")
    public static <T> AsyncJob<T> asyncJob(
            Transport transport, String createOperationId, String id, Object result, RequestOptions options) {
        Facts.Poll poll = poll(createOperationId);
        RouteSpec pollRoute = route(poll.operation());
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
                answer -> field(answer, poll.statusField()),
                poll.terminal(),
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
