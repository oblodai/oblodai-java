package com.oblodai;

import com.oblodai.generated.Facts;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Which operations are long-running, and how to follow them - views of the contract's {@code
 * x-sdk-poll} table ({@link Facts#LRO}), which the generator writes; this class keeps no list of
 * its own. A create call listed in {@link #LRO} answers with an acknowledgement; {@link
 * Oblodai#jobs()} turns it into a {@link com.oblodai.core.Job} whose {@code waitFor()} polls the
 * operation named here until the status is terminal for that job.
 */
public final class Lro {

    private Lro() {}

    /** {@code create operationId -> poll operationId}, in the order of the contract. */
    public static final Map<String, String> LRO = lro();

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
    public static final Map<String, Poll> POLLS = polls();

    /**
     * Every status after which some job no longer changes (each job stops only at the statuses of
     * its own operation, {@link Facts.Poll#terminal()}).
     */
    public static final Set<String> TERMINAL_STATUSES = terminal();

    private static Map<String, String> lro() {
        Map<String, String> out = new LinkedHashMap<>();
        Facts.LRO.forEach((create, poll) -> out.put(create, poll.operation()));
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Poll> polls() {
        Map<String, Poll> out = new LinkedHashMap<>();
        for (Facts.Poll p : Facts.LRO.values()) {
            out.putIfAbsent(p.operation(), new Poll(p.idField(), p.parse(), p.download()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Set<String> terminal() {
        Set<String> out = new LinkedHashSet<>();
        Facts.LRO.values().forEach(p -> out.addAll(p.terminal()));
        return Collections.unmodifiableSet(out);
    }
}
