package codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates {@code src/main/java/com/oblodai/contract/**} from {@code contract/contract.json} (the
 * gateway's own conformance export) and {@code contract/descriptions.en.json} (English field docs).
 *
 * <p>Nothing in the generated files is edited by hand. Run {@code codegen/run.sh}; {@code
 * codegen/run.sh --check} regenerates in memory and fails when the committed files disagree, which
 * is what {@code mvn verify} runs as its drift gate.
 */
public final class Codegen {

    private static final String PKG = "com.oblodai.contract";
    private static final String REQ_PKG = PKG + ".requests";

    /** Routes the gateway serves but the merchant SDK does not model (health, docs, internals). */
    private static final String SKIP_PATHS = "^/(healthz|readyz|docs|openapi\\.json|internal).*";

    private final Path root;
    private final Map<String, Object> contract;
    private final Map<String, Object> descriptions;
    private final String header;
    private final Map<Path, String> files = new TreeMap<>();
    private final List<String> missingDescriptions = new ArrayList<>();

    private Codegen(Path root) throws IOException {
        this.root = root;
        byte[] raw = Files.readAllBytes(root.resolve("contract/contract.json"));
        this.contract = Json.obj(Json.parse(new String(raw, StandardCharsets.UTF_8)));
        Path descPath = root.resolve("contract/descriptions.en.json");
        this.descriptions =
                Files.exists(descPath)
                        ? Json.obj(Json.parse(Files.readString(descPath, StandardCharsets.UTF_8)))
                        : Map.of("request", Map.of(), "response", Map.of());
        this.header =
                "// GENERATED FILE — do not edit. Source: contract/contract.json (core "
                        + Json.str(contract.get("core_commit")).substring(0, 12)
                        + ").\n// Regenerate with: codegen/run.sh\n";
    }

    public static void main(String[] args) throws Exception {
        boolean check = args.length > 0 && args[0].equals("--check");
        Path root = Path.of(System.getProperty("oblodai.root", ".")).toAbsolutePath().normalize();
        Codegen gen = new Codegen(root);
        gen.generate();
        if (check) {
            gen.check();
        } else {
            gen.write();
        }
    }

    private void generate() throws IOException {
        List<Map<String, Object>> routes = routes();
        emitRoutes(routes);
        new Enums(this).emit(Json.obj(contract.get("enums")), strings(contract.get("event_types")));
        emitErrorCodes();
        emitVersion();
        RequestTypes requests = new RequestTypes(this);
        requests.emit(routes);
        System.out.printf(
                "codegen: %d routes, %d error codes, %d request types%n",
                routes.size(), Json.arr(contract.get("error_codes")).size(), requests.count());
        if (!missingDescriptions.isEmpty()) {
            System.out.println(
                    "codegen: "
                            + missingDescriptions.size()
                            + " request fields lack an English description in"
                            + " contract/descriptions.en.json:\n  "
                            + String.join("\n  ", missingDescriptions));
        }
    }

    private void write() throws IOException {
        for (Map.Entry<Path, String> e : files.entrySet()) {
            Files.createDirectories(e.getKey().getParent());
            Files.writeString(e.getKey(), e.getValue(), StandardCharsets.UTF_8);
        }
        // Remove generated files that the current contract no longer produces.
        for (Path dir : List.of(root.resolve(srcDir(PKG)), root.resolve(srcDir(REQ_PKG)))) {
            if (!Files.isDirectory(dir)) continue;
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    if (Files.isDirectory(p) || files.containsKey(p)) continue;
                    if (Files.readString(p).startsWith("// GENERATED FILE")) Files.delete(p);
                }
            }
        }
        System.out.println("codegen: wrote " + files.size() + " files");
    }

    private void check() throws IOException {
        List<String> drifted = new ArrayList<>();
        for (Map.Entry<Path, String> e : files.entrySet()) {
            if (!Files.exists(e.getKey())
                    || !Files.readString(e.getKey(), StandardCharsets.UTF_8).equals(e.getValue())) {
                drifted.add(root.relativize(e.getKey()).toString());
            }
        }
        for (Path dir : List.of(root.resolve(srcDir(PKG)), root.resolve(srcDir(REQ_PKG)))) {
            if (!Files.isDirectory(dir)) continue;
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    if (Files.isDirectory(p) || files.containsKey(p)) continue;
                    if (Files.readString(p).startsWith("// GENERATED FILE")) {
                        drifted.add(root.relativize(p) + " (stale)");
                    }
                }
            }
        }
        if (!drifted.isEmpty()) {
            System.err.println(
                    "contract drift: "
                            + String.join(", ", drifted)
                            + " differ from contract/contract.json — run codegen/run.sh and commit"
                            + " the regenerated files");
            System.exit(1);
        }
        System.out.println("check-drift: " + files.size() + " generated files are in sync");
    }

    // --- routes ---------------------------------------------------------------------------------

    private List<Map<String, Object>> routes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : Json.arr(contract.get("routes"))) {
            Map<String, Object> r = Json.obj(o);
            if (Json.str(r.get("path")).matches(SKIP_PATHS)) continue;
            out.add(r);
        }
        out.sort(
                (a, b) -> {
                    int byPath = Json.str(a.get("path")).compareTo(Json.str(b.get("path")));
                    return byPath != 0
                            ? byPath
                            : Json.str(a.get("method")).compareTo(Json.str(b.get("method")));
                });
        return out;
    }

    /**
     * Whether a transport failure may be re-sent without an idempotency key. This is the core's own
     * hand-classified verdict, carried in the contract as {@code safe}; the SDK never guesses it from
     * the path, because a guess that says "safe" about a route that moves money is a double spend.
     */
    private static boolean isSafe(Map<String, Object> r) {
        Object safe = r.get("safe");
        if (!(safe instanceof Boolean flag)) {
            throw new IllegalStateException(
                    "route "
                            + Json.str(r.get("method"))
                            + " "
                            + Json.str(r.get("path"))
                            + " has no boolean \"safe\" field — re-export contract/contract.json from a"
                            + " core that declares it");
        }
        return flag;
    }

    private static String constantName(Map<String, Object> r) {
        String path = Json.str(r.get("path")).replaceAll("[{}]", "").replaceAll("[^A-Za-z0-9]+", "_");
        return (Json.str(r.get("method")) + path).toUpperCase().replaceAll("_+", "_");
    }

    private void emitRoutes(List<Map<String, Object>> routes) {
        StringBuilder sb = new StringBuilder(header);
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("import java.util.LinkedHashMap;\nimport java.util.Collections;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n * Every merchant-facing route the gateway declares, keyed exactly as its")
                .append(" conformance table keys\n * them ({@code \"POST /v1/payment\"}).\n */\n");
        sb.append("public final class Routes {\n\n    private Routes() {}\n\n");
        for (Map<String, Object> r : routes) {
            String key = Json.str(r.get("method")) + " " + Json.str(r.get("path"));
            sb.append("    /** {@code ").append(key).append("} */\n");
            sb.append("    public static final RouteSpec ")
                    .append(constantName(r))
                    .append(" = new RouteSpec(\"")
                    .append(Json.str(r.get("method")))
                    .append("\", \"")
                    .append(Json.str(r.get("path")))
                    .append("\", RouteAuth.")
                    .append(Json.str(r.get("auth")).toUpperCase())
                    .append(", ")
                    .append(r.get("idempotent"))
                    .append(", ")
                    .append(isSafe(r))
                    .append(", ")
                    .append(r.get("bare"))
                    .append(", ")
                    .append(
                            r.get("list") == null
                                    ? "null"
                                    : "ListKind." + Json.str(r.get("list")).toUpperCase())
                    .append(");\n\n");
        }
        sb.append("    /** Every route by {@code \"METHOD /path\"}. */\n");
        sb.append("    public static final Map<String, RouteSpec> ALL;\n\n");
        sb.append("    static {\n        Map<String, RouteSpec> all = new LinkedHashMap<>();\n");
        for (Map<String, Object> r : routes) {
            sb.append("        all.put(")
                    .append(constantName(r))
                    .append(".key(), ")
                    .append(constantName(r))
                    .append(");\n");
        }
        sb.append("        ALL = Collections.unmodifiableMap(all);\n    }\n\n");
        sb.append("    /** Looks a route up by {@code \"METHOD /path\"}; throws when unknown. */\n");
        sb.append("    public static RouteSpec of(String key) {\n");
        sb.append("        RouteSpec spec = ALL.get(key);\n");
        sb.append("        if (spec == null) throw new IllegalArgumentException(\"unknown route: \""
                + " + key);\n");
        sb.append("        return spec;\n    }\n}\n");
        files.put(javaFile(PKG, "Routes"), sb.toString());
    }

    // --- error codes ----------------------------------------------------------------------------

    private void emitErrorCodes() {
        List<String> codes = strings(contract.get("error_codes"));
        StringBuilder sb = new StringBuilder(header);
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("import java.util.List;\nimport java.util.Set;\n\n");
        sb.append("/**\n * Every error code the gateway source can emit, as {@code family.reason}.")
                .append("\n *\n * <p>{@code OblodaiException#code()} carries one of these (or a")
                .append(" transport/SDK code); the list is a\n * ledger, not a gate — an unknown")
                .append(" code is still delivered to the caller.\n */\n");
        sb.append("public final class ErrorCodes {\n\n    private ErrorCodes() {}\n\n");
        sb.append("    /** All ").append(codes.size()).append(" codes, sorted. */\n");
        sb.append("    public static final List<String> ALL = List.of(\n");
        for (int i = 0; i < codes.size(); i += 3) {
            List<String> chunk = codes.subList(i, Math.min(i + 3, codes.size()));
            StringBuilder line = new StringBuilder("            ");
            for (int j = 0; j < chunk.size(); j++) {
                line.append('"').append(chunk.get(j)).append('"');
                if (i + j < codes.size() - 1) line.append(", ");
            }
            sb.append(line).append('\n');
        }
        sb.append("            );\n\n");
        sb.append("    private static final Set<String> INDEX = Set.copyOf(ALL);\n\n");
        sb.append("    /** Whether the code belongs to this contract snapshot. */\n");
        sb.append("    public static boolean isKnown(String code) {\n");
        sb.append("        return code != null && INDEX.contains(code);\n    }\n}\n");
        files.put(javaFile(PKG, "ErrorCodes"), sb.toString());
    }

    private void emitVersion() throws IOException {
        byte[] raw = Files.readAllBytes(root.resolve("contract/contract.json"));
        String hash;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            hash = hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String sb =
                header
                        + "package "
                        + PKG
                        + ";\n\n/** Which contract snapshot this SDK was generated from. */\n"
                        + "public final class ContractVersion {\n\n"
                        + "    private ContractVersion() {}\n\n"
                        + "    /** Gateway commit the snapshot was exported from. */\n"
                        + "    public static final String CORE_COMMIT = \""
                        + Json.str(contract.get("core_commit"))
                        + "\";\n\n"
                        + "    /** When the snapshot was exported. */\n"
                        + "    public static final String EXPORTED_AT = \""
                        + Json.str(contract.get("exported_at"))
                        + "\";\n\n"
                        + "    /** SHA-256 of contract/contract.json. */\n"
                        + "    public static final String HASH = \""
                        + hash
                        + "\";\n}\n";
        files.put(javaFile(PKG, "ContractVersion"), sb);
    }

    // --- names ----------------------------------------------------------------------------------

    static String pascal(String text) {
        String camel = camel(text);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    static String camel(String wire) {
        String[] parts = wire.split("[^A-Za-z0-9]+");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /** The generated-file banner every emitted source starts with. */
    String header() {
        return header;
    }

    /** The English field documentation, by route. */
    Map<String, Object> descriptions() {
        return descriptions;
    }

    /** Records a file to write (or to compare against, under --check). */
    void putFile(java.nio.file.Path path, String content) {
        files.put(path, content);
    }

    /** Records a documented field with no English description, for the run's summary. */
    void noteMissingDescription(String field) {
        missingDescriptions.add(field);
    }

    Path javaFile(String pkg, String name) {
        return root.resolve(srcDir(pkg)).resolve(name + ".java");
    }

    private static String srcDir(String pkg) {
        return "src/main/java/" + pkg.replace('.', '/');
    }

    static List<String> strings(Object array) {
        List<String> out = new ArrayList<>();
        for (Object o : Json.arr(array)) out.add(Json.str(o));
        return out;
    }
}
