package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Both clients expose every generated resource: a new tag in the contract cannot go missing. */
class ClientSurfaceTest {

    private static Set<String> generated(String dir) throws IOException {
        try (Stream<Path> files = Files.list(Path.of(dir))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".java"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> exposed(Class<?> client, String pkg) {
        Set<String> out = new TreeSet<>();
        for (Method m : client.getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getPackageName().equals(pkg)) {
                out.add(m.getReturnType().getSimpleName());
            }
        }
        return out;
    }

    @Test
    void everyGeneratedResourceIsOnBothClients() throws IOException {
        assertEquals(
                generated("src/main/java/com/oblodai/generated/resources"),
                exposed(Oblodai.class, "com.oblodai.generated.resources"));
        assertEquals(
                generated("src/main/java/com/oblodai/generated/resources/async"),
                exposed(OblodaiAsync.class, "com.oblodai.generated.resources.async"));
    }
}
