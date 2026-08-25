package codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** Read-only routes: a transport failure may be retried without risking a duplicate side effect. */
    private static final String SAFE_SUFFIX =
            ".*/(info|history|list|calculate|validate|services|get|balance|qr|deliveries)$";

    /** Paths that look read-only but whose body can mutate state. */
    private static final Set<String> NOT_SAFE = Set.of("POST /v1/vrcs");

    /** Routes the gateway serves but the merchant SDK does not model (health, docs, internals). */
    private static final String SKIP_PATHS = "^/(healthz|readyz|docs|openapi\\.json|internal).*";

    private static final Map<String, String> ENUM_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> FIELD_ENUMS = new LinkedHashMap<>();
    private static final Map<String, String> ROUTE_FIELD_VALUES = new LinkedHashMap<>();
    private static final Map<String, List<String>> REQUIRED_OVERRIDES = new LinkedHashMap<>();

    static {
        ENUM_NAMES.put("payment_status", "PaymentStatus");
        ENUM_NAMES.put("payout_status", "PayoutStatus");
        ENUM_NAMES.put("payout_link_status", "PayoutLinkStatus");
        ENUM_NAMES.put("delivery_status", "DeliveryStatus");
        ENUM_NAMES.put("network", "Network");
        ENUM_NAMES.put("fee_bearer", "FeeBearer");
        ENUM_NAMES.put("fee_bearer_result", "FeeBearerResult");
        ENUM_NAMES.put("batch_on_error", "BatchOnError");
        ENUM_NAMES.put("webhook_kind", "WebhookKind");
        ENUM_NAMES.put("error_kind", "ErrorKind");

        // Request fields drawn from a generated vocabulary: the setter takes the enum, and a String
        // overload keeps a value newer than this snapshot callable.
        FIELD_ENUMS.put("network", "Network");
        FIELD_ENUMS.put("pinned_network", "Network");
        FIELD_ENUMS.put("on_error", "BatchOnError");
        FIELD_ENUMS.put("fee_bearer", "FeeBearer");
        FIELD_ENUMS.put("amount_mode", "AmountMode");

        // Vocabularies the core does not export as enums; documented on the field instead.
        ROUTE_FIELD_VALUES.put("POST /v1/payment/history#status", "PaymentStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/payout/history#status", "PayoutStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/payout/history#kind", "\"payout\" | \"refund\"");
        ROUTE_FIELD_VALUES.put("POST /v1/payment/resolve#action", "\"accept\" | \"refund\"");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/payment#status", "PaymentStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/payout#status", "PayoutStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/wallet#status", "\"paid\"");
        ROUTE_FIELD_VALUES.put("POST /v1/payment/testing-webhook#status", "PaymentStatus");

        // Fields the handler requires although the shared DTO marks them optional (batch items reuse
        // the single-create DTO, where the core backfills the key from the Idempotency-Key header).
        REQUIRED_OVERRIDES.put("POST /v1/payment/batch", List.of("payments.order_id"));
        REQUIRED_OVERRIDES.put("POST /v1/payout/batch", List.of("payouts.order_id"));
        REQUIRED_OVERRIDES.put("POST /v1/refund/batch", List.of("refunds.reference"));
        REQUIRED_OVERRIDES.put(
                "POST /v1/transfer/batch",
                List.of("transfers.order_id", "transfers.amount", "transfers.currency"));
        REQUIRED_OVERRIDES.put("POST /v1/payout/link/batch", List.of("items.reference"));
        REQUIRED_OVERRIDES.put("POST /v1/transfer/to-user", List.of("amount", "currency"));
        REQUIRED_OVERRIDES.put("POST /v1/claim/{token}", List.of("address"));
    }

    /** Request schemas for routes whose core DTO is not declared in the gateway's docs module. */
    private static Map<String, Object> requestOverride(String key) {
        if (!key.equals("POST /v1/merchants")) return null;
        Map<String, Object> email = new LinkedHashMap<>();
        email.put("type", "string");
        email.put("example", "owner@shop.example");
        Map<String, Object> name = new LinkedHashMap<>();
        name.put("type", "string");
        name.put("example", "Acme");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("email", email);
        props.put("name", name);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("email"));
        return schema;
    }

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
        emitEnums();
        emitErrorCodes();
        emitVersion();
        emitRequests(routes);
        System.out.printf(
                "codegen: %d routes, %d error codes, %d request types%n",
                routes.size(), Json.arr(contract.get("error_codes")).size(), requestCount);
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

    private static boolean isSafe(Map<String, Object> r) {
        String key = Json.str(r.get("method")) + " " + Json.str(r.get("path"));
        if (NOT_SAFE.contains(key)) return false;
        return Json.str(r.get("method")).equals("GET") || Json.str(r.get("path")).matches(SAFE_SUFFIX);
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

    // --- enums ----------------------------------------------------------------------------------

    private void emitEnums() {
        Map<String, Object> enums = Json.obj(contract.get("enums"));
        for (Map.Entry<String, String> e : ENUM_NAMES.entrySet()) {
            Object values = enums.get(e.getKey());
            if (values == null) throw new IllegalStateException("enum " + e.getKey() + " missing");
            emitEnum(e.getValue(), strings(values), enumDoc(e.getValue()));
        }
        emitEnum(
                "AmountMode",
                List.of("fixed", "open", "range"),
                "How a payment link prices a checkout. Not exported by the gateway as an enum;"
                        + " pinned here from its handlers.");
        emitEnum(
                "EventType",
                strings(contract.get("event_types")),
                "Webhook event types: {@code invoice.<status>}, {@code payout.<status>}, {@code"
                        + " wallet.paid}.");
    }

    private static String enumDoc(String name) {
        return switch (name) {
            case "PaymentStatus" -> "Invoice lifecycle: {@code select → created → confirm_check →"
                    + " paid | paid_over | wrong_amount | expired | cancelled}.";
            case "PayoutStatus" -> "Payout lifecycle: {@code pending → approved → awaiting_cosign →"
                    + " broadcasting → sent → confirmed | failed | cancelled}.";
            case "PayoutLinkStatus" -> "Lifecycle of a payout link (cheque).";
            case "DeliveryStatus" -> "State of one webhook delivery.";
            case "Network" -> "Blockchain networks the gateway settles on.";
            case "FeeBearer" -> "Who pays the network fee of a payout.";
            case "FeeBearerResult" -> "Who paid the network fee, as reported on a priced result.";
            case "BatchOnError" -> "What an asynchronous batch does after a failing element.";
            case "WebhookKind" -> "Kinds of rehearsal webhook the gateway can deliver.";
            case "ErrorKind" -> "Error families behind the HTTP statuses the gateway returns.";
            default -> name + " vocabulary.";
        };
    }

    private void emitEnum(String name, List<String> values, String doc) {
        StringBuilder sb = new StringBuilder(header);
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonCreator;\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n\n");
        sb.append("/**\n * ").append(doc).append("\n *\n");
        sb.append(" * <p>A value this snapshot does not know decodes to {@link #UNKNOWN} rather than")
                .append(" failing, so a\n * gateway that grows its vocabulary cannot break a")
                .append(" deployed client.\n */\n");
        sb.append("public enum ").append(name).append(" {\n\n");
        for (String v : values) {
            sb.append("    /** {@code ").append(v).append("} */\n");
            sb.append("    ").append(enumConstant(v)).append("(\"").append(v).append("\"),\n\n");
        }
        sb.append("    /** A value outside this snapshot's vocabulary. Serializes as an empty")
                .append(" string. */\n    UNKNOWN(\"\");\n\n");
        sb.append("    private final String wire;\n\n");
        sb.append("    ").append(name).append("(String wire) {\n        this.wire = wire;\n    }\n\n");
        sb.append("    /** The exact string the API uses. */\n");
        sb.append("    @JsonValue\n    public String wire() {\n        return wire;\n    }\n\n");
        sb.append("    /** Decodes a wire value; anything unknown becomes {@link #UNKNOWN}. */\n");
        sb.append("    @JsonCreator\n    public static ")
                .append(name)
                .append(" from(String wire) {\n")
                .append("        if (wire == null) return null;\n")
                .append("        for (")
                .append(name)
                .append(" value : values()) {\n")
                .append("            if (value.wire.equals(wire)) return value;\n        }\n")
                .append("        return UNKNOWN;\n    }\n\n");
        sb.append("    @Override\n    public String toString() {\n        return wire;\n    }\n}\n");
        files.put(javaFile(PKG, name), sb.toString());
    }

    private static String enumConstant(String wire) {
        return wire.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
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

    // --- request bodies -------------------------------------------------------------------------

    private int requestCount;

    private void emitRequests(List<Map<String, Object>> routes) {
        for (Map<String, Object> r : routes) {
            String key = Json.str(r.get("method")) + " " + Json.str(r.get("path"));
            Object schema = r.get("request_schema");
            if (schema == null) schema = requestOverride(key);
            if (schema == null) continue;
            requestCount++;
            String name = requestClassName(Json.str(r.get("path")));
            StringBuilder sb = new StringBuilder(header);
            sb.append("package ").append(REQ_PKG).append(";\n\n");
            sb.append("import com.fasterxml.jackson.annotation.JsonInclude;\n");
            sb.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
            sb.append("import com.oblodai.contract.*;\n");
            sb.append("import java.util.List;\nimport java.util.Map;\n\n");
            sb.append("/**\n * Body of {@code ").append(key).append("}.\n *\n");
            sb.append(" * <p>Fluent setters; unset fields are omitted from the JSON, never sent as")
                    .append(" null.\n */\n");
            sb.append("@JsonInclude(JsonInclude.Include.NON_NULL)\n");
            emitBodyClass(sb, "public final class " + name, Json.obj(schema), key, "", 0);
            files.put(javaFile(REQ_PKG, name), sb.toString());
        }
    }

    /** A request DTO (or one nested item type) with fluent setters and getters. */
    private void emitBodyClass(
            StringBuilder sb,
            String declaration,
            Map<String, Object> schema,
            String route,
            String prefix,
            int depth) {
        String pad = "    ".repeat(depth);
        sb.append(pad).append(declaration).append(" {\n\n");
        Map<String, Object> props =
                schema.get("properties") == null
                        ? Map.of()
                        : new TreeMap<>(Json.obj(schema.get("properties")));
        Set<String> required = requiredOf(schema, route, prefix);
        List<String[]> fields = new ArrayList<>(); // {wireName, javaName, javaType, enumType}
        List<Runnable> nested = new ArrayList<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            String wire = e.getKey();
            Map<String, Object> p = Json.obj(e.getValue());
            String javaName = camel(wire);
            String type = javaType(p, route, prefix, wire, depth, sb, nested);
            fields.add(
                    new String[] {
                        wire, javaName, type, FIELD_ENUMS.get(wire),
                    });
            String doc = doc(route, prefix + wire, p, required.contains(wire));
            sb.append(pad).append("    ").append(doc.replace("\n", "\n" + pad + "    ")).append('\n');
            sb.append(pad)
                    .append("    @JsonProperty(\"")
                    .append(wire)
                    .append("\")\n")
                    .append(pad)
                    .append("    private ")
                    .append(type)
                    .append(' ')
                    .append(javaName)
                    .append(";\n\n");
        }
        for (String[] f : fields) {
            String setterDoc = "/** Sets {@code " + f[0] + "}. */";
            sb.append(pad).append("    ").append(setterDoc).append('\n');
            sb.append(pad)
                    .append("    public ")
                    .append(className(declaration))
                    .append(' ')
                    .append(f[1])
                    .append('(')
                    .append(f[2])
                    .append(" value) {\n")
                    .append(pad)
                    .append("        this.")
                    .append(f[1])
                    .append(" = value;\n")
                    .append(pad)
                    .append("        return this;\n")
                    .append(pad)
                    .append("    }\n\n");
            if (f[3] != null && f[2].equals("String")) {
                sb.append(pad)
                        .append("    /** Sets {@code ")
                        .append(f[0])
                        .append("} from the generated vocabulary. */\n");
                sb.append(pad)
                        .append("    public ")
                        .append(className(declaration))
                        .append(' ')
                        .append(f[1])
                        .append('(')
                        .append(f[3])
                        .append(" value) {\n")
                        .append(pad)
                        .append("        this.")
                        .append(f[1])
                        .append(" = value == null ? null : value.wire();\n")
                        .append(pad)
                        .append("        return this;\n")
                        .append(pad)
                        .append("    }\n\n");
            }
            sb.append(pad).append("    /** Current {@code ").append(f[0]).append("}. */\n");
            sb.append(pad)
                    .append("    public ")
                    .append(f[2])
                    .append(' ')
                    .append(f[1])
                    .append("() {\n")
                    .append(pad)
                    .append("        return ")
                    .append(f[1])
                    .append(";\n")
                    .append(pad)
                    .append("    }\n\n");
        }
        for (Runnable r : nested) r.run();
        sb.append(pad).append("}\n");
    }

    private String javaType(
            Map<String, Object> p,
            String route,
            String prefix,
            String wire,
            int depth,
            StringBuilder sb,
            List<Runnable> nested) {
        String type = p.get("type") == null ? "" : Json.str(p.get("type"));
        switch (type) {
            case "string":
                return "String";
            case "integer":
                return "Integer";
            case "number":
                return "Double";
            case "boolean":
                return "Boolean";
            case "array":
                {
                    Map<String, Object> items =
                            p.get("items") == null ? Map.of() : Json.obj(p.get("items"));
                    String itemType;
                    if ("object".equals(items.get("type")) && items.get("properties") != null) {
                        String name = itemClassName(wire);
                        nested.add(
                                () ->
                                        emitBodyClass(
                                                sb,
                                                "public static final class " + name,
                                                items,
                                                route,
                                                prefix + wire + ".",
                                                depth + 1));
                        itemType = name;
                    } else {
                        itemType = javaType(items, route, prefix, wire, depth, sb, nested);
                    }
                    return "List<" + itemType + ">";
                }
            case "object":
                {
                    if (p.get("properties") != null) {
                        String name = itemClassName(wire);
                        Map<String, Object> obj = p;
                        nested.add(
                                () ->
                                        emitBodyClass(
                                                sb,
                                                "public static final class " + name,
                                                obj,
                                                route,
                                                prefix + wire + ".",
                                                depth + 1));
                        return name;
                    }
                    if (p.get("additionalProperties") != null) {
                        return "Map<String, "
                                + javaType(
                                        Json.obj(p.get("additionalProperties")),
                                        route,
                                        prefix,
                                        wire,
                                        depth,
                                        sb,
                                        nested)
                                + ">";
                    }
                    return "Map<String, Object>";
                }
            default:
                return "Object";
        }
    }

    private Set<String> requiredOf(Map<String, Object> schema, String route, String prefix) {
        Set<String> out = new LinkedHashSet<>();
        if (schema.get("required") != null) out.addAll(strings(schema.get("required")));
        for (String f : REQUIRED_OVERRIDES.getOrDefault(route, List.of())) {
            if (f.startsWith(prefix) && !f.substring(prefix.length()).contains(".")) {
                out.add(f.substring(prefix.length()));
            }
        }
        return out;
    }

    private String doc(String route, String path, Map<String, Object> p, boolean required) {
        String desc = null;
        Object byRoute = Json.obj(descriptions.get("request")).get(route);
        if (byRoute != null) desc = (String) Json.obj(byRoute).get(path);
        if (desc == null && p.get("description") != null) missingDescriptions.add(route + "#" + path);
        StringBuilder sb = new StringBuilder("/**");
        if (desc != null) sb.append(' ').append(escapeDoc(desc));
        if (required) sb.append(desc != null ? " " : " ").append("Required.");
        if (p.get("example") != null) {
            // The gateway's snapshot carries some examples in Russian; the SDK's documentation is
            // English only, so a non-ASCII sample value is dropped rather than translated.
            String example = String.valueOf(p.get("example"));
            if (example.chars().allMatch(c -> c >= 0x20 && c < 0x7f)) {
                sb.append(" Example: {@code ").append(example).append("}.");
            }
        }
        String enumType = ROUTE_FIELD_VALUES.get(route + "#" + path);
        if (enumType != null) sb.append(" One of ").append(escapeDoc(enumType)).append('.');
        sb.append(" */");
        String text = sb.toString();
        return text.equals("/** */") ? "/** {@code " + path + "}. */" : text;
    }

    private static String escapeDoc(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("@", "&#64;")
                .replace("*/", "*&#47;");
    }

    // --- names ----------------------------------------------------------------------------------

    private static String className(String declaration) {
        String[] words = declaration.split("\\s+");
        return words[words.length - 1];
    }

    /** {@code /v1/payout/link/batch → PayoutLinkBatchRequest}; {@code {id}} contributes its name. */
    static String requestClassName(String path) {
        StringBuilder sb = new StringBuilder();
        for (String segment : path.replaceAll("[{}]", "").split("/")) {
            if (segment.isEmpty() || segment.equals("v1")) continue;
            sb.append(pascal(segment));
        }
        return sb + "Request";
    }

    /** Nested item type for an array/object field: {@code payouts → Payout}, {@code items → Item}. */
    private static String itemClassName(String field) {
        String base = field.endsWith("s") ? field.substring(0, field.length() - 1) : field;
        return pascal(base);
    }

    private static String pascal(String text) {
        String camel = camel(text);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    private static String camel(String wire) {
        String[] parts = wire.split("[^A-Za-z0-9]+");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private Path javaFile(String pkg, String name) {
        return root.resolve(srcDir(pkg)).resolve(name + ".java");
    }

    private static String srcDir(String pkg) {
        return "src/main/java/" + pkg.replace('.', '/');
    }

    private static List<String> strings(Object array) {
        List<String> out = new ArrayList<>();
        for (Object o : Json.arr(array)) out.add(Json.str(o));
        return out;
    }
}
