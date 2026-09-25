package com.oblodai.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.Oblodai;
import com.oblodai.OblodaiAsync;
import com.oblodai.RequestOptions;
import com.oblodai.core.RouteSpec;
import com.oblodai.core.Signing;
import com.oblodai.errors.OblodaiException;
import com.oblodai.errors.SignatureException;
import com.oblodai.errors.WebhookPayloadException;
import com.oblodai.generated.Facts;
import com.oblodai.generated.Routes;
import com.oblodai.generated.models.WireObject;
import com.oblodai.support.Clients;
import com.oblodai.support.MockHttpClient;
import com.oblodai.webhooks.WebhookDeliveryInfo;
import com.oblodai.webhooks.WebhookHeaders;
import com.oblodai.webhooks.WebhookVerifier;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The shared conformance suite every Oblodai SDK runs (backend {@code tools/sdkgen/conformance}).
 *
 * <p>Scenarios are read from {@code $SDKGEN_CONFORMANCE}, else from {@code tools/sdkgen/conformance}
 * of the backend checkout ({@code $OBLODAI_BACKEND}, else {@code ../oblodai-backend}). Signing
 * vectors are not in the scenario files: each suite names the backend {@code openapi.json} and a
 * pointer into its {@code x-oblodai-signing}, and the vectors are read from there.
 *
 * <p>Every call scenario runs on the blocking and on the async client over a scripted HTTP client;
 * retry pauses are recorded instead of slept.
 */
class ConformanceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static Path suiteDir() {
        String explicit = System.getenv("SDKGEN_CONFORMANCE");
        if (explicit != null && !explicit.isEmpty()) {
            return Path.of(explicit);
        }
        String backend = System.getenv("OBLODAI_BACKEND");
        Path root = backend != null && !backend.isEmpty() ? Path.of(backend) : Path.of("..", "oblodai-backend");
        return root.resolve("tools").resolve("sdkgen").resolve("conformance");
    }

    private static Path requireSuite() {
        Path dir = suiteDir();
        if (!Files.isDirectory(dir)) {
            boolean named = System.getenv("SDKGEN_CONFORMANCE") != null || System.getenv("OBLODAI_BACKEND") != null;
            if (named) {
                fail("conformance suite not found at " + dir);
            }
            Assumptions.abort("conformance suite not found at " + dir + "; set OBLODAI_BACKEND or SDKGEN_CONFORMANCE");
        }
        return dir;
    }

    private static JsonNode read(Path path) {
        try {
            return JSON.readTree(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    private static JsonNode pointer(JsonNode doc, String pointer) {
        JsonNode cur = doc;
        for (String part : pointer.replaceFirst("^/", "").split("/")) {
            cur = cur.get(part.replace("~1", "/").replace("~0", "~"));
        }
        return cur;
    }

    private record Source(JsonNode signing, List<JsonNode> vectors) {}

    private static Source source(Path dir, JsonNode suite) {
        JsonNode spec = read(dir.resolve(suite.path("source").path("spec").asText()).normalize());
        List<JsonNode> vectors = new ArrayList<>();
        pointer(spec, suite.path("source").path("pointer").asText()).forEach(vectors::add);
        assertTrue(!vectors.isEmpty(), "no vectors in the spec");
        return new Source(spec.get("x-oblodai-signing"), vectors);
    }

    // --- signing ----------------------------------------------------------------------------------

    @TestFactory
    List<DynamicTest> requestSigning() {
        Path dir = requireSuite();
        JsonNode suite = read(dir.resolve("signing.json"));
        Source src = source(dir, suite);
        List<DynamicTest> out = new ArrayList<>();
        for (JsonNode check : suite.path("checks")) {
            for (int i = 0; i < src.vectors().size(); i++) {
                JsonNode v = src.vectors().get(i);
                out.add(
                        DynamicTest.dynamicTest(
                                check.path("name").asText() + "#" + i,
                                () -> {
                                    String key = v.path("idempotency_key").asText();
                                    String idem = key.isEmpty() ? null : key;
                                    long ts = v.path("ts").asLong();
                                    String method = v.path("method").asText();
                                    String uri = v.path("request_uri").asText();
                                    String body = v.path("body").asText();
                                    if (check.path("kind").asText().equals("request_canonical")) {
                                        assertEquals(
                                                v.path("canonical").asText(),
                                                Signing.canonicalString(ts, method, uri, idem, body));
                                    } else {
                                        assertEquals("request_signature", check.path("kind").asText());
                                        assertEquals(
                                                v.path("signature").asText(),
                                                Signing.signRequest(
                                                        v.path("secret").asText(), ts, method, uri, idem, body));
                                    }
                                }));
            }
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> webhooks() {
        Path dir = requireSuite();
        JsonNode suite = read(dir.resolve("webhook.json"));
        Source src = source(dir, suite);
        long skew = src.signing().path("skew_seconds").asLong();
        List<DynamicTest> out = new ArrayList<>();
        for (JsonNode check : suite.path("checks")) {
            for (int i = 0; i < src.vectors().size(); i++) {
                JsonNode v = src.vectors().get(i);
                out.add(DynamicTest.dynamicTest(check.path("name").asText() + "#" + i, () -> webhook(check, v, skew)));
            }
        }
        return out;
    }

    private static void webhook(JsonNode check, JsonNode v, long skew) {
        String secret = v.path("secret").asText();
        long ts = v.path("ts").asLong();
        String payload = v.path("payload").asText();
        if (check.path("kind").asText().equals("webhook_signature")) {
            assertEquals(v.path("signature").asText(), Signing.signWebhook(secret, ts, payload));
            return;
        }
        assertEquals("webhook_verify", check.path("kind").asText());
        JsonNode from = check.path("now_from_ts");
        long offset =
                from.isNumber()
                        ? from.asLong()
                        : switch (from.asText()) {
                            case "skew" -> skew;
                            case "skew+1" -> skew + 1;
                            default -> throw new AssertionError("now_from_ts " + from);
                        };
        String signature = v.path("signature").asText();
        switch (check.path("mutate").asText()) {
            case "payload" -> payload = payload + " ";
            case "signature" -> signature = (signature.charAt(0) != '0' ? "0" : "1") + signature.substring(1);
            default -> {}
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Webhook-Timestamp", Long.toString(ts));
        headers.put("X-Webhook-Signature", signature);
        long now = ts + offset;
        WebhookVerifier.Options options =
                WebhookVerifier.options(secret).tolerance(Duration.ofSeconds(skew)).clock(() -> now);
        String body = payload;
        if (check.path("expect").asText().equals("ok")) {
            // The vectors sign bare payloads, not whole events: verify() gets past the MAC and the
            // freshness window and only then may refuse to parse - that refusal still is a pass.
            try {
                WebhookVerifier.verify(body, WebhookHeaders.of(headers), options);
            } catch (WebhookPayloadException afterTheChecks) {
                // passed the signature and the window
            }
            return;
        }
        try {
            WebhookVerifier.verify(body, WebhookHeaders.of(headers), options);
        } catch (SignatureException e) {
            assertEquals("webhook." + check.path("expect").asText(), e.code());
            return;
        }
        fail("expected webhook." + check.path("expect").asText());
    }

    /**
     * A real delivery of every event of the contract verifies (with the current secret and, as a
     * receiver that has not swapped yet, the previous one), parses into its kind's model and exposes
     * every delivery header of the spec.
     */
    @TestFactory
    List<DynamicTest> webhookDeliveries() {
        Path dir = requireSuite();
        JsonNode suite = read(dir.resolve("webhook_delivery.json"));
        Source src = source(dir, suite);
        List<DynamicTest> out = new ArrayList<>();
        Set<String> events = new HashSet<>();
        src.vectors().forEach(d -> events.add(d.path("event").asText()));
        Set<String> known = new HashSet<>();
        Facts.WEBHOOK_KINDS.values().forEach(k -> known.addAll(k.events()));
        out.add(DynamicTest.dynamicTest("a delivery of every event this release knows", () -> assertEquals(known, events)));
        for (JsonNode check : suite.path("checks")) {
            assertEquals("webhook_delivery", check.path("kind").asText());
            for (JsonNode d : src.vectors()) {
                String name = check.path("name").asText() + " - " + d.path("event").asText() + " (" + check.path("key").asText() + ")";
                out.add(DynamicTest.dynamicTest(name, () -> delivery(check, d, suite.path("headers"))));
            }
        }
        return out;
    }

    private static void delivery(JsonNode check, JsonNode d, JsonNode fields) {
        String secret =
                switch (check.path("key").asText()) {
                    case "current" -> d.path("secret").asText();
                    case "previous" -> d.path("previous_secret").asText();
                    default -> throw new AssertionError("key " + check.path("key"));
                };
        Map<String, String> headers = new LinkedHashMap<>();
        d.path("headers").fields().forEachRemaining(e -> headers.put(e.getKey(), e.getValue().asText()));
        long ts = d.path("ts").asLong();
        WebhookDeliveryInfo delivery =
                WebhookVerifier.verifyDelivery(
                        d.path("payload").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        WebhookHeaders.of(headers),
                        WebhookVerifier.options(secret).clock(() -> ts));
        String kind = d.path("kind").asText();
        assertTrue(WebhookVerifier.isKnownEvent(delivery.event()), kind);
        assertEquals(kind, delivery.event().type());
        assertInstanceOf(Facts.WEBHOOK_KINDS.get(kind).model(), delivery.event().typed());
        fields.fields()
                .forEachRemaining(
                        e -> {
                            String want = headers.get(e.getKey());
                            Object got =
                                    switch (e.getValue().asText()) {
                                        case "" -> want;
                                        case "id" -> delivery.id();
                                        case "event_id" -> delivery.eventId();
                                        case "event_type" -> delivery.eventType();
                                        case "event_time" -> String.valueOf(delivery.eventTime());
                                        case "sent_at" -> String.valueOf(delivery.sentAt());
                                        default -> throw new AssertionError(
                                                "the delivery info has no field " + e.getValue() + " for " + e.getKey());
                                    };
                            assertEquals(want, got, e.getValue().asText() + " != " + e.getKey());
                        });
    }

    // --- calls ------------------------------------------------------------------------------------

    /** {@code operationId -> (resource class, method)}, read from the generated source. */
    private record Target(String resource, String method) {}

    private static Map<String, Target> operations() throws IOException, IllegalAccessException {
        Map<RouteSpec, String> constants = new LinkedHashMap<>();
        for (Field f : Routes.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == RouteSpec.class) {
                constants.put((RouteSpec) f.get(null), f.getName());
            }
        }
        Map<String, String> byConstant = new LinkedHashMap<>();
        Routes.BY_OPERATION_ID.forEach((op, route) -> byConstant.put(constants.get(route), op));

        Pattern method = Pattern.compile("^    public [\\w<>, ]+ (\\w+)\\(");
        Pattern route = Pattern.compile("Routes\\.(\\w+)");
        Map<String, Target> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(Path.of("src/main/java/com/oblodai/generated/resources"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String cls = file.getFileName().toString().replace(".java", "");
                String current = null;
                for (String line : Files.readAllLines(file)) {
                    Matcher m = method.matcher(line);
                    if (m.find()) {
                        current = m.group(1);
                    }
                    Matcher r = route.matcher(line);
                    while (r.find() && current != null) {
                        String op = byConstant.get(r.group(1));
                        if (op != null) {
                            out.putIfAbsent(op, new Target(cls, current));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Replays a scenario's responses into the scripted client. */
    private static MockHttpClient script(JsonNode responses) throws IOException {
        MockHttpClient http = new MockHttpClient();
        for (JsonNode next : responses) {
            if (next.path("transport_error").asText().equals("timeout")) {
                http.fails(new HttpTimeoutException("scripted timeout"));
                continue;
            }
            List<String> headers = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = next.path("headers").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> h = it.next();
                headers.add(h.getKey());
                headers.add(h.getValue().asText());
            }
            if (next.has("json")) {
                headers.add("content-type");
                headers.add("application/json");
                http.raw(next.path("status").asInt(), JSON.writeValueAsString(next.get("json")), headers.toArray(String[]::new));
            } else {
                headers.add("content-type");
                headers.add("text/html");
                http.raw(next.path("status").asInt(), "<html>proxy</html>", headers.toArray(String[]::new));
            }
        }
        return http;
    }

    private static Object resource(Object client, String cls) throws ReflectiveOperationException {
        for (Method m : client.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().equals(cls)
                    && m.getReturnType().getPackageName().startsWith("com.oblodai.generated.resources")) {
                return m.invoke(client);
            }
        }
        throw new AssertionError("no resource " + cls + " on " + client.getClass().getSimpleName());
    }

    /** The widest overload: every argument, {@code RequestOptions} last. */
    private static Method full(Object resource, String name) {
        Method best = null;
        for (Method m : resource.getClass().getMethods()) {
            if (m.getName().equals(name)
                    && m.getParameterCount() > 0
                    && m.getParameterTypes()[m.getParameterCount() - 1] == RequestOptions.class
                    && (best == null || m.getParameterCount() > best.getParameterCount())) {
                best = m;
            }
        }
        assertNotNull(best, name);
        return best;
    }

    /** Arguments from the scenario: the request model parsed from {@code args}, then the options. */
    private static Object[] arguments(Method m, JsonNode args) throws ReflectiveOperationException, IOException {
        Class<?>[] types = m.getParameterTypes();
        Object[] out = new Object[types.length];
        Object tree = JSON.readValue(JSON.writeValueAsBytes(args), Object.class); // floats stay double
        for (int i = 0; i < types.length - 1; i++) {
            if (WireObject.class.isAssignableFrom(types[i])) {
                Method fromJson = types[i].getMethod("fromJson", Object.class);
                out[i] = fromJson.invoke(null, tree);
            } else if (types[i] == String.class && tree instanceof Map<?, ?> map) {
                Object v = map.get(m.getParameters()[i].getName());
                out[i] = v == null ? null : String.valueOf(v);
            }
        }
        return out;
    }

    private static Throwable cause(Throwable t) {
        Throwable c = t;
        while ((c instanceof InvocationTargetException || c instanceof CompletionException) && c.getCause() != null) {
            c = c.getCause();
        }
        return c;
    }

    private static void check(JsonNode scenario, MockHttpClient http, List<Long> delays, Object result, Throwable error)
            throws IOException {
        JsonNode expect = scenario.path("expect");
        assertEquals(expect.path("requests").asInt(), http.calls().size(), "requests");
        List<String> keys = new ArrayList<>();
        http.calls().forEach(c -> keys.add(c.header("idempotency-key")));
        if (expect.path("idempotency_key").asText().equals("absent")) {
            keys.forEach(k -> assertEquals(null, k, "no key expected"));
        } else if (expect.path("idempotency_key").asText().equals("present")) {
            keys.forEach(k -> assertNotNull(k, "a key expected"));
        }
        if (expect.path("same_idempotency_key").asBoolean(false)) {
            Set<String> distinct = new HashSet<>(keys);
            assertTrue(keys.get(0) != null && distinct.size() == 1, keys.toString());
        }
        if (expect.has("delays_ms")) {
            List<Long> want = new ArrayList<>();
            expect.get("delays_ms").forEach(d -> want.add(d.asLong()));
            assertEquals(want, delays, "pauses");
        }
        Iterator<Map.Entry<String, JsonNode>> bodyFields = expect.path("request_body_field").fields();
        while (bodyFields.hasNext()) {
            Map.Entry<String, JsonNode> f = bodyFields.next();
            JsonNode sent = JSON.readTree(http.calls().get(http.calls().size() - 1).body());
            assertEquals(f.getValue(), sent.get(f.getKey()), f.getKey());
        }
        if (expect.has("error_code")) {
            OblodaiException e = assertInstanceOf(OblodaiException.class, error);
            assertEquals(expect.path("error_code").asText(), e.code());
            return;
        }
        if (error != null) {
            throw new AssertionError("unexpected " + error, error);
        }
        Iterator<Map.Entry<String, JsonNode>> resultFields = expect.path("result_field").fields();
        while (resultFields.hasNext()) {
            Map.Entry<String, JsonNode> f = resultFields.next();
            assertEquals(f.getValue().asText(), plain(accessor(result, f.getKey())), f.getKey());
        }
    }

    private static Object accessor(Object result, String json) {
        StringBuilder name = new StringBuilder();
        boolean up = false;
        for (char c : json.toCharArray()) {
            if (c == '_') {
                up = true;
            } else {
                name.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        try {
            return result.getClass().getMethod(name.toString()).invoke(result);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no accessor " + name + " on " + result.getClass().getSimpleName(), e);
        }
    }

    private static String plain(Object value) {
        if (value instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (value instanceof WireObject w) {
            return String.valueOf(w.toJson());
        }
        return String.valueOf(value);
    }

    /** forward_compat webhooks: a body parses, keeps its raw type, and is known exactly as expected. */
    @TestFactory
    List<DynamicTest> webhookParse() {
        JsonNode suite = read(requireSuite().resolve("forward_compat.json"));
        List<DynamicTest> out = new ArrayList<>();
        for (JsonNode c : suite.path("webhooks")) {
            out.add(
                    DynamicTest.dynamicTest(
                            c.path("name").asText(),
                            () -> {
                                com.oblodai.webhooks.WebhookEvent event =
                                        WebhookVerifier.parse(JSON.writeValueAsString(c.path("body")));
                                assertEquals(c.path("expect").path("type").asText(), event.type());
                                assertEquals(
                                        c.path("expect").path("known").asBoolean(),
                                        WebhookVerifier.isKnownEvent(event));
                            }));
        }
        assertTrue(!out.isEmpty(), "forward_compat.json has no webhook bodies");
        return out;
    }

    private static List<JsonNode> scenarios(Path dir) {
        List<JsonNode> out = new ArrayList<>();
        for (String name : List.of("retry", "money", "forward_compat")) {
            read(dir.resolve(name + ".json")).path("scenarios").forEach(out::add);
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> calls() throws Exception {
        Path dir = requireSuite();
        Map<String, Target> operations = operations();
        List<DynamicTest> out = new ArrayList<>();
        for (JsonNode scenario : scenarios(dir)) {
            String op = scenario.path("call").path("operation").asText();
            Target target = operations.get(op);
            assertNotNull(target, "no generated method serves " + op);
            JsonNode args = scenario.path("call").path("args");
            out.add(
                    DynamicTest.dynamicTest(
                            "sync/" + scenario.path("name").asText(),
                            () -> {
                                MockHttpClient http = script(scenario.path("responses"));
                                Clients.RecordingSleeper sleeper = new Clients.RecordingSleeper();
                                Oblodai client = defaults(http, sleeper).build();
                                Object result = null;
                                Throwable error = null;
                                try {
                                    Object res = resource(client, target.resource());
                                    Method m = full(res, target.method());
                                    result = m.invoke(res, arguments(m, args));
                                } catch (Throwable t) {
                                    error = cause(t);
                                }
                                check(scenario, http, sleeper.pauses, result, error);
                            }));
            out.add(
                    DynamicTest.dynamicTest(
                            "async/" + scenario.path("name").asText(),
                            () -> {
                                MockHttpClient http = script(scenario.path("responses"));
                                Clients.RecordingSleeper sleeper = new Clients.RecordingSleeper();
                                OblodaiAsync client = defaults(http, sleeper).buildAsync();
                                Object result = null;
                                Throwable error = null;
                                try {
                                    Object res = resource(client, target.resource());
                                    Method m = full(res, target.method());
                                    result = ((CompletableFuture<?>) m.invoke(res, arguments(m, args))).join();
                                } catch (Throwable t) {
                                    error = cause(t);
                                }
                                check(scenario, http, sleeper.pauses, result, error);
                            }));
        }
        return out;
    }

    /** The shipped defaults (retry policy included) over the scripted client. */
    private static Oblodai.Builder defaults(MockHttpClient http, Clients.RecordingSleeper sleeper) {
        return Oblodai.builder()
                .publicId(Clients.PUBLIC_ID)
                .secret(Clients.SECRET)
                .baseUrl("https://api.test")
                .httpClient(http)
                .environment(Map.of())
                .sleeper(sleeper);
    }
}
