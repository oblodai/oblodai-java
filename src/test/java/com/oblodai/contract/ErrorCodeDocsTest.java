package com.oblodai.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.oblodai.support.Contract;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * An error code named in a method's documentation must be one the gateway can actually emit.
 *
 * <p>A doc that tells a caller to branch on {@code wallet.blocked} when the gateway has no such code
 * is worse than silence: the branch is written, it never fires, and the failure it was meant to
 * handle lands in the default arm. So every {@code family.reason} the SDK's own sources mention is
 * checked against the contract's catalogue.
 */
class ErrorCodeDocsTest {

    /** A lowercase dotted token, as an error code is written in {@code {@code …}}. */
    private static final Pattern TOKEN =
            Pattern.compile("\\{@code ([a-z][a-z0-9_]*\\.[a-z][a-z0-9_]+)}");

    /** The same token as the markdown documentation writes it: in backticks. */
    private static final Pattern MARKDOWN_TOKEN =
            Pattern.compile("`([a-z][a-z0-9_]*\\.[a-z][a-z0-9_]+)`");

    /** Codes the SDK raises itself; they are not in the gateway's catalogue by design. */
    private static final Pattern SDK_OWN = Pattern.compile("^(sdk|transport|webhook)\\.");

    /** Every source file whose documentation a caller reads. */
    private static List<Path> sources() {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The prose documentation, which names codes in backticks rather than {@code {@code …}}. */
    private static List<Path> markdown() {
        return Stream.of("README.md", "AGENTS.md", "CHANGELOG.md", "MIGRATION-1.3.md")
                .map(Path::of)
                .filter(Files::exists)
                .toList();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The families the catalogue actually uses; anything else is not an error code at all. */
    private static Set<String> families() {
        Set<String> out = new LinkedHashSet<>();
        for (String code : ErrorCodes.ALL) {
            int dot = code.indexOf('.');
            if (dot > 0) out.add(code.substring(0, dot));
        }
        return out;
    }

    /** Webhook event types read like codes ({@code invoice.paid}) and are not codes. */
    private static Set<String> eventTypes() {
        Set<String> out = new LinkedHashSet<>();
        for (JsonNode type : Contract.contract().path("event_types")) out.add(type.asText());
        return out;
    }

    @Test
    void everyErrorCodeNamedInTheSourcesExists() {
        Set<String> families = families();
        Set<String> events = eventTypes();
        List<String> unknown = new ArrayList<>();

        List<Path> documents = new ArrayList<>(sources());
        documents.addAll(markdown());
        for (Path source : documents) {
            String text = read(source);
            Matcher matcher =
                    (source.toString().endsWith(".md") ? MARKDOWN_TOKEN : TOKEN).matcher(text);
            while (matcher.find()) {
                String token = matcher.group(1);
                if (SDK_OWN.matcher(token).find()) continue;
                if (events.contains(token)) continue;
                if (!families.contains(token.substring(0, token.indexOf('.')))) continue;
                if (!ErrorCodes.isKnown(token)) {
                    unknown.add(source.getFileName() + ": " + token);
                }
            }
        }

        assertEquals(List.of(), unknown, "these documented codes are not in the contract catalogue");
    }

    @Test
    void theCheckWouldCatchACodeTheGatewayDoesNotHave() {
        // The one that was documented and does not exist: the wallet MODEL has a `blocked` field,
        // but there is no `wallet.blocked` error code.
        assertFalse(ErrorCodes.isKnown("wallet.blocked"));
        assertTrue(families().contains("wallet"), "so the check does look at it");
        assertTrue(TOKEN.matcher("{@code wallet.blocked}").find(), "and does match how it is written");

        for (String real :
                List.of(
                        "wallet.bad_uuid",
                        "refund.no_address",
                        "refund.nothing_to_refund",
                        "refund.dust",
                        "refund.destination_internal")) {
            assertTrue(ErrorCodes.isKnown(real), real);
        }
    }

    @Test
    void theMoneyMovingMethodsAllDocumentCodesToBranchOn() {
        // C12: a caller of a money-moving method must be told which failures to expect.
        List<String> missing = new ArrayList<>();
        for (Path source : sources()) {
            String name = source.getFileName().toString();
            if (!List.of(
                            "Payments.java",
                            "Payouts.java",
                            "Refunds.java",
                            "PaymentLinks.java",
                            "PayoutLinks.java",
                            "Transfers.java",
                            "Wallets.java")
                    .contains(name)) {
                continue;
            }
            if (!read(source).contains("Errors worth branching on")) missing.add(source.toString());
        }
        assertEquals(List.of(), missing, "these namespaces document no error codes");
    }
}
