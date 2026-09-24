package com.oblodai.support;

import com.oblodai.Oblodai;
import com.oblodai.core.RetryOptions;
import com.oblodai.core.Sleeper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** A client over a {@link MockHttpClient} whose pauses are recorded, not slept. */
public final class Clients {

    /** The public id every test client signs with. */
    public static final String PUBLIC_ID = "pk_test_1";

    /** The secret every test client signs with. */
    public static final String SECRET = "secret-1";

    private Clients() {}

    /** Pauses the transport asked for, in milliseconds. */
    public static final class RecordingSleeper implements Sleeper {
        /** Every pause, in order. */
        public final List<Long> pauses = Collections.synchronizedList(new ArrayList<>());

        @Override
        public CompletableFuture<Void> sleep(long millis) {
            pauses.add(millis);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * @param http the scripted HTTP client
     * @param sleeper where pauses are recorded
     * @return a builder signed with the test key, two retries, no environment
     */
    public static Oblodai.Builder builder(MockHttpClient http, Sleeper sleeper) {
        return Oblodai.builder()
                .publicId(PUBLIC_ID)
                .secret(SECRET)
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .retry(new RetryOptions(2, 1, 2, 30_000))
                .sleeper(sleeper);
    }

    /**
     * @param http the scripted HTTP client
     * @return a client whose pauses return at once
     */
    public static Oblodai client(MockHttpClient http) {
        return builder(http, new RecordingSleeper()).build();
    }
}
