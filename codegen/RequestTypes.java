package codegen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Emits the request DTOs: one fluent class per route the gateway documents a body for, with the
 * English field descriptions from {@code contract/descriptions.en.json} as Javadoc.
 *
 * <p>Split out of {@link Codegen} because it is the one part with real shape logic — nested item
 * types, required-field overrides, vocabulary setters — while the rest is transcription.
 */
final class RequestTypes {

    private static final String PKG = "com.oblodai.contract";
    private static final String REQ_PKG = PKG + ".requests";

    private static final Map<String, String> FIELD_ENUMS = new LinkedHashMap<>();
    private static final Map<String, String> ROUTE_FIELD_VALUES = new LinkedHashMap<>();
    private static final Map<String, List<String>> REQUIRED_OVERRIDES = new LinkedHashMap<>();

    static {
        // Request fields drawn from a generated vocabulary: the setter takes the enum, and a String
        // overload keeps a value newer than this snapshot callable.
        FIELD_ENUMS.put("network", "Network");
        FIELD_ENUMS.put("pinned_network", "Network");
        FIELD_ENUMS.put("on_error", "BatchOnError");
        FIELD_ENUMS.put("fee_bearer", "FeeBearer");
        FIELD_ENUMS.put("amount_mode", "AmountMode");

        // Vocabularies the gateway does not export as enums; documented on the field instead.
        ROUTE_FIELD_VALUES.put("POST /v1/payment/history#status", "PaymentStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/payout/history#status", "PayoutStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/payout/history#kind", "\"payout\" | \"refund\"");
        ROUTE_FIELD_VALUES.put("POST /v1/payment/resolve#action", "\"accept\" | \"refund\"");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/payment#status", "PaymentStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/payout#status", "PayoutStatus");
        ROUTE_FIELD_VALUES.put("POST /v1/test-webhook/wallet#status", "\"paid\"");
        ROUTE_FIELD_VALUES.put("POST /v1/payment/testing-webhook#status", "PaymentStatus");

        // Fields the handler requires although the shared DTO marks them optional (batch items reuse
        // the single-create DTO, where the gateway backfills the key from the Idempotency-Key header).
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

    private final Codegen gen;
    private int count;

    RequestTypes(Codegen gen) {
        this.gen = gen;
    }

    /** How many request types were emitted. */
    int count() {
        return count;
    }

    // --- request bodies -------------------------------------------------------------------------

    void emit(List<Map<String, Object>> routes) {
        for (Map<String, Object> r : routes) {
            String key = Json.str(r.get("method")) + " " + Json.str(r.get("path"));
            Object schema = r.get("request_schema");
            if (schema == null) schema = requestOverride(key);
            if (schema == null) continue;
            count++;
            String name = requestClassName(Json.str(r.get("path")));
            StringBuilder sb = new StringBuilder(gen.header());
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
            gen.putFile(gen.javaFile(REQ_PKG, name), sb.toString());
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
            String javaName = Codegen.camel(wire);
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
        if (schema.get("required") != null) out.addAll(Codegen.strings(schema.get("required")));
        for (String f : REQUIRED_OVERRIDES.getOrDefault(route, List.of())) {
            if (f.startsWith(prefix) && !f.substring(prefix.length()).contains(".")) {
                out.add(f.substring(prefix.length()));
            }
        }
        return out;
    }

    private String doc(String route, String path, Map<String, Object> p, boolean required) {
        String desc = null;
        Object byRoute = Json.obj(gen.descriptions().get("request")).get(route);
        if (byRoute != null) desc = (String) Json.obj(byRoute).get(path);
        if (desc == null && p.get("description") != null) gen.noteMissingDescription(route + "#" + path);
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

    private static String className(String declaration) {
        String[] words = declaration.split("\\s+");
        return words[words.length - 1];
    }

    /** {@code /v1/payout/link/batch → PayoutLinkBatchRequest}; {@code {id}} contributes its name. */
    static String requestClassName(String path) {
        StringBuilder sb = new StringBuilder();
        for (String segment : path.replaceAll("[{}]", "").split("/")) {
            if (segment.isEmpty() || segment.equals("v1")) continue;
            sb.append(Codegen.pascal(segment));
        }
        return sb + "Request";
    }

    /** Nested item type for an array/object field: {@code payouts → Payout}, {@code items → Item}. */
    private static String itemClassName(String field) {
        String base = field.endsWith("s") ? field.substring(0, field.length() - 1) : field;
        return Codegen.pascal(base);
    }
}
