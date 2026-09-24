package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * MIGRATION-2.0.md maps every 2.0 method: the frozen list {@code names.2.0.txt}, not the current
 * {@code names.lock}, which grows with the contract without anyone editing the migration guide.
 */
class MigrationTest {

    private static List<String> lines(String file) throws IOException {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(file))) {
            if (!line.isBlank()) out.add(line.trim());
        }
        return out;
    }

    private static String camel(String snake) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    @Test
    void theGuideMapsEveryNameOf20() throws IOException {
        String guide = Files.readString(Path.of("MIGRATION-2.0.md"));
        List<String> frozen = lines("names.2.0.txt");
        assertTrue(frozen.size() > 100, "names.2.0.txt holds the 2.0 names");
        List<String> missing = new ArrayList<>();
        for (String name : frozen) {
            String[] parts = name.split("\\.", 2);
            String java = "`" + camel(parts[0]) + "()." + camel(parts[1]) + "`";
            if (!guide.contains(java)) missing.add(java);
        }
        assertTrue(missing.isEmpty(), "MIGRATION-2.0.md lacks " + missing);
    }

    @Test
    void noNameOf20IsLost() throws IOException {
        List<String> lock = lines("names.lock");
        List<String> lost = new ArrayList<>(lines("names.2.0.txt"));
        lost.removeAll(lock);
        assertTrue(lost.isEmpty(), "names.lock lost " + lost);
    }
}
