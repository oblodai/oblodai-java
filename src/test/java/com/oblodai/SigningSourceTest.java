package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblodai.generated.SigningProtocol;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * The signing protocol has one source: the contract's {@code x-oblodai-signing}, generated into
 * {@link SigningProtocol}. The generated constants are the spec's, and no hand-written source spells
 * a header name of its own — a header renamed in the contract reaches the SDK by regeneration alone.
 */
class SigningSourceTest {

    private static JsonNode signing() throws IOException {
        String backend = System.getenv("OBLODAI_BACKEND");
        Path root = backend != null && !backend.isEmpty() ? Path.of(backend) : Path.of("..", "oblodai-backend");
        Path spec = root.resolve("services/core/api/openapi.json");
        if (!Files.isRegularFile(spec)) {
            if (backend != null && !backend.isEmpty()) {
                fail("backend spec not found at " + spec);
            }
            Assumptions.abort("backend spec not found at " + spec + "; set OBLODAI_BACKEND");
        }
        return new ObjectMapper().readTree(Files.readAllBytes(spec)).path("x-oblodai-signing");
    }

    private static List<String> names(JsonNode list) {
        List<String> out = new ArrayList<>();
        list.forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    void generatedConstantsAreTheSpecs() throws IOException {
        JsonNode s = signing();
        assertEquals(names(s.path("headers")), SigningProtocol.REQUEST_HEADERS);
        assertEquals(names(s.path("webhook").path("headers")), SigningProtocol.WEBHOOK_HEADERS);
        assertEquals(s.path("webhook").path("test_header").asText(), SigningProtocol.HEADER_WEBHOOK_TEST);
        assertEquals(s.path("skew_seconds").asInt(), SigningProtocol.SKEW_SECONDS);
        assertEquals(s.path("max_body").asLong(), SigningProtocol.MAX_BODY);
        assertEquals(s.path("max_idempotency_key_length").asInt(), SigningProtocol.MAX_IDEMPOTENCY_KEY_LENGTH);
        assertEquals(s.path("algorithm").asText(), SigningProtocol.SIGNATURE_ALGORITHM);
    }

    @Test
    void noSigningHeaderIsSpelledOutsideGenerated() throws IOException {
        JsonNode s = signing();
        List<String> wanted = new ArrayList<>(names(s.path("headers")));
        wanted.addAll(names(s.path("webhook").path("headers")));
        wanted.add(s.path("webhook").path("test_header").asText());
        List<String> lower = wanted.stream().map(n -> n.toLowerCase(Locale.ROOT)).toList();
        List<String> offenders = new ArrayList<>();
        Path main = Path.of("src", "main");
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                if (file.toString().contains("/generated/")) {
                    continue;
                }
                String text = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                for (String name : lower) {
                    if (text.contains(name)) {
                        offenders.add(main.relativize(file) + ": " + name);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders);
    }

    /**
     * No hand-written source spells a literal of the body or idempotency-key limit — decimal (with or
     * without an {@code L} suffix), or {@code 1 << n} for a power of two; digit separators
     * ({@code 1_048_576}) do not hide one. Both are read from {@link SigningProtocol}, so a changed
     * limit reaches the SDK by regeneration alone. The skew is not scanned for: its value is also an
     * HTTP status class ({@code < 300}); the alias tests hold it.
     */
    @Test
    void noSigningLimitIsSpelledOutsideGenerated() throws IOException {
        List<Pattern> pats = new ArrayList<>();
        for (long limit : new long[] {SigningProtocol.MAX_BODY, SigningProtocol.MAX_IDEMPOTENCY_KEY_LENGTH}) {
            pats.add(Pattern.compile("(?<![\\w.])" + limit + "[lL]?(?![\\w.])"));
        }
        if (Long.bitCount(SigningProtocol.MAX_BODY) == 1) {
            pats.add(Pattern.compile(
                    "\\b1[lL]?\\s*<<\\s*" + Long.numberOfTrailingZeros(SigningProtocol.MAX_BODY) + "\\b"));
        }
        List<String> offenders = new ArrayList<>();
        Path main = Path.of("src", "main");
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                if (file.toString().contains("/generated/")) {
                    continue;
                }
                String text = Files.readString(file, StandardCharsets.UTF_8).replaceAll("(?<=\\d)_(?=\\d)", "");
                for (Pattern p : pats) {
                    if (p.matcher(text).find()) {
                        offenders.add(main.relativize(file) + ": " + p.pattern());
                    }
                }
            }
        }
        assertEquals(List.of(), offenders);
    }
}
