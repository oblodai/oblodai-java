package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The README shows code; {@code examples/} compiles it. This test is the wire between them.
 *
 * <p>A snippet nobody compiles rots silently: the SDK changes, the README keeps promising the old
 * shape, and the first person to notice is a reader who copied it. {@code DocSnippets.java} and
 * {@code DocSnippetsKotlin.kt} are compiled with the test sources, so a stale snippet breaks the
 * build — but only for the snippets that are actually there. This test closes the other half: every
 * Java and Kotlin block in README.md must appear, statement for statement, in the compiled files.
 *
 * <p>Comparison is on the code with comments removed and whitespace squeezed out, so the README may
 * wrap a call chain where {@code google-java-format} would not, and the example may take parameters
 * where the README shows a bare variable. What it may not do is say something different.
 *
 * <p>It also holds the two READMEs together: README.ru.md translates the prose, never the code, so
 * its blocks must be byte-identical to the English ones. A Russian reader who copies a snippet gets
 * exactly what an English reader gets.
 */
class DocSnippetsTest {

    /** One fenced block of a markdown file. */
    private record Block(String language, String code) {}

    private static final Path README = Path.of("README.md");
    private static final Path README_RU = Path.of("README.ru.md");
    private static final Path JAVA_SNIPPETS =
            Path.of("examples/java/com/oblodai/examples/DocSnippets.java");
    private static final Path KOTLIN_SNIPPETS =
            Path.of("examples/kotlin/com/oblodai/examples/DocSnippetsKotlin.kt");

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every fenced block of a markdown file, in order, with the language tag it declares. */
    private static List<Block> blocks(Path markdown) {
        List<Block> blocks = new ArrayList<>();
        String[] lines = read(markdown).split("\n", -1);
        String language = null;
        StringBuilder body = null;
        for (String line : lines) {
            if (line.startsWith("```")) {
                if (body == null) {
                    language = line.substring(3).trim();
                    body = new StringBuilder();
                } else {
                    blocks.add(new Block(language, body.toString()));
                    body = null;
                }
                continue;
            }
            if (body != null) body.append(line).append('\n');
        }
        assertEquals(null, body, markdown + " has an unclosed code fence");
        return blocks;
    }

    /**
     * The code with comments dropped and every whitespace character removed.
     *
     * <p>String and character literals are honoured, so a {@code //} inside {@code
     * "https://shop.example/hook"} is not mistaken for a comment.
     */
    static String normalise(String code) {
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                while (i < code.length() && code.charAt(i) != '\n') i++;
                continue;
            }
            if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < code.length() && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                i = Math.min(i + 2, code.length());
                continue;
            }
            if (c == '"' || c == '\'') {
                char quote = c;
                out.append(c);
                i++;
                while (i < code.length()) {
                    char inner = code.charAt(i);
                    out.append(inner);
                    i++;
                    if (inner == '\\' && i < code.length()) {
                        out.append(code.charAt(i));
                        i++;
                        continue;
                    }
                    if (inner == quote) break;
                }
                continue;
            }
            if (!Character.isWhitespace(c)) out.append(c);
            i++;
        }
        // Whitespace inside a literal survived the copy above; squeeze it out too, on both sides of
        // the comparison, so the two are still compared like for like.
        StringBuilder squeezed = new StringBuilder(out.length());
        for (int j = 0; j < out.length(); j++) {
            char c = out.charAt(j);
            if (!Character.isWhitespace(c)) squeezed.append(c);
        }
        return squeezed.toString();
    }

    @Test
    void theRussianReadmeShowsExactlyTheSameCode() {
        List<Block> english = blocks(README);
        List<Block> russian = blocks(README_RU);

        assertEquals(
                english.size(),
                russian.size(),
                "README.ru.md must carry the same code blocks as README.md, in the same order");
        for (int i = 0; i < english.size(); i++) {
            assertEquals(
                    english.get(i).language(),
                    russian.get(i).language(),
                    "block " + (i + 1) + " has a different language tag");
            assertEquals(
                    english.get(i).code(),
                    russian.get(i).code(),
                    "block " + (i + 1) + " differs; translate the prose, never the code");
        }
    }

    @Test
    void everyJavaSnippetInTheReadmeIsCompiled() {
        String compiled = normalise(read(JAVA_SNIPPETS));
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Block block : blocks(README)) {
            if (!"java".equals(block.language())) continue;
            checked++;
            if (!compiled.contains(normalise(block.code()))) missing.add(block.code());
        }

        assertTrue(checked >= 10, "the README should still be showing Java, found " + checked);
        assertEquals(
                List.of(),
                missing,
                "these README snippets are not in " + JAVA_SNIPPETS + ", so nothing compiles them");
    }

    @Test
    void everyKotlinSnippetInTheReadmeIsCompiled() {
        String compiled = normalise(read(KOTLIN_SNIPPETS));
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Block block : blocks(README)) {
            if (!"kotlin".equals(block.language())) continue;
            checked++;
            if (!compiled.contains(normalise(block.code()))) missing.add(block.code());
        }

        assertTrue(checked >= 1, "the README should still be showing Kotlin");
        assertEquals(List.of(), missing, "these README snippets are not in " + KOTLIN_SNIPPETS);
    }

    @Test
    void theCheckWouldCatchASnippetThatDriftedApart() {
        String compiled = normalise(read(JAVA_SNIPPETS));

        // A statement no example makes: the check must not find it.
        assertFalse(compiled.contains(normalise("oblodai.payments().create(new PaymentRequest().amount(25))")));
        // And a real one, wrapped differently than the example wraps it: the check must find it.
        assertTrue(
                compiled.contains(
                        normalise("Oblodai.builder()\n  .baseUrl(\"http://127.0.0.1:8095\")\n  .build();")));
        // A `//` inside a URL is not a comment.
        assertEquals(
                "\"https://shop.example/hook\";",
                normalise("\"https://shop.example/hook\"; // a comment, though"));
    }
}
