package com.oblodai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.core.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Access to the contract snapshot the tests are written against: the route registry with its signing
 * and webhook vectors, the golden response bodies recorded from a live gateway, the error samples and
 * the real signed webhook deliveries.
 */
public final class Contract {

    private static final ObjectMapper MAPPER = Json.newMapper();
    private static final Path DIR = Path.of("contract");

    private Contract() {}

    /** One recorded exchange with the gateway. */
    public record Fixture(String route, int status, JsonNode request, JsonNode response, JsonNode headers) {

        /** The {@code result} of a success envelope. */
        public JsonNode result() {
            return response == null ? null : response.path("result");
        }

        /** The {@code error} of a failure envelope. */
        public JsonNode error() {
            return response == null ? null : response.path("error");
        }

        /** Whether the recording is a success. */
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }

        /** Whether the recorded body is JSON (as opposed to a rendered document). */
        public boolean isJson() {
            String type = headers == null ? null : headers.path("Content-Type").asText("");
            return type != null && type.contains("json");
        }
    }

    /** The whole contract snapshot. */
    public static JsonNode contract() {
        return read(DIR.resolve("contract.json"));
    }

    /** Every recorded exchange, keyed by {@code "METHOD /path"}. */
    public static Map<String, Fixture> fixtures() {
        Map<String, Fixture> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(DIR.resolve("fixtures"))) {
            for (Path file : files.sorted().toList()) {
                JsonNode node = read(file);
                Fixture fixture =
                        new Fixture(
                                node.path("route").asText(),
                                node.path("status").asInt(),
                                node.get("request"),
                                node.get("response"),
                                node.get("headers"));
                out.put(fixture.route(), fixture);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** The recorded exchange for one route. */
    public static Fixture fixture(String route) {
        Fixture fixture = fixtures().get(route);
        if (fixture == null) throw new AssertionError("no fixture for " + route);
        return fixture;
    }

    /** The recorded success {@code result} for one route. */
    public static JsonNode result(String route) {
        Fixture fixture = fixture(route);
        if (!fixture.isSuccess()) {
            throw new AssertionError("fixture for " + route + " is a refusal (" + fixture.status() + ")");
        }
        return fixture.result();
    }

    /** Error envelope samples, keyed by their error code. */
    public static Map<String, Fixture> errorSamples() {
        Map<String, Fixture> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(DIR.resolve("errors"))) {
            for (Path file : files.sorted().toList()) {
                JsonNode node = read(file);
                String code = file.getFileName().toString().replaceAll("\\.json$", "");
                out.put(
                        code,
                        new Fixture(
                                node.path("route").asText(),
                                node.path("status").asInt(),
                                node.get("request"),
                                node.get("response"),
                                node.get("headers")));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** Real signed webhook deliveries: headers, decoded body and the exact bytes delivered. */
    public static List<JsonNode> webhookSamples() {
        List<JsonNode> out = new ArrayList<>();
        read(DIR.resolve("webhook-samples.json")).forEach(out::add);
        return out;
    }

    private static JsonNode read(Path path) {
        try {
            return MAPPER.readTree(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path.toAbsolutePath(), e);
        }
    }
}
