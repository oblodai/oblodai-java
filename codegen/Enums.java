package codegen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The vocabulary half of the generator: every enum {@code contract.json} exports, written as a Java
 * enum that decodes an unknown value to {@code UNKNOWN} rather than failing. Split out of
 * {@link Codegen} so each file stays readable on its own.
 */
final class Enums {

    private static final String PKG = "com.oblodai.contract";

    /** Contract name to Java name, in the order the files are written. */
    private static final Map<String, String> ENUM_NAMES = new LinkedHashMap<>();

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
    }

    private final Codegen gen;

    Enums(Codegen gen) {
        this.gen = gen;
    }

    // --- enums ----------------------------------------------------------------------------------

    /**
     * Writes one Java enum per vocabulary the gateway exports, plus the two the SDK pins itself.
     *
     * @param enums the {@code enums} block of the contract
     * @param eventTypes the {@code event_types} list of the contract
     */
    void emit(Map<String, Object> enums, List<String> eventTypes) {
        emitVocabularyInterface();
        for (Map.Entry<String, String> e : ENUM_NAMES.entrySet()) {
            Object values = enums.get(e.getKey());
            if (values == null) throw new IllegalStateException("enum " + e.getKey() + " missing");
            emitEnum(e.getValue(), Codegen.strings(values), enumDoc(e.getValue()));
        }
        emitEnum(
                "AmountMode",
                List.of("fixed", "open", "range"),
                "How a payment link prices a checkout. Not exported by the gateway as an enum;"
                        + " pinned here from its handlers.");
        emitEnum(
                "EventType",
                eventTypes,
                "Webhook event types: {@code invoice.<status>}, {@code payout.<status>}, {@code"
                        + " wallet.paid}.");
    }

    private String enumDoc(String name) {
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

    /** The interface every generated vocabulary implements, so they can be handled uniformly. */
    private void emitVocabularyInterface() {
        String source =
                gen.header()
                        + "package "
                        + PKG
                        + ";\n\n"
                        + "/**\n"
                        + " * What every vocabulary this contract exports has in common: the exact"
                        + " string the API used, and\n"
                        + " * whether that string is one this snapshot knows.\n"
                        + " *\n"
                        + " * <p>The vocabularies are open. A gateway that starts sending a value"
                        + " this SDK has never heard\n"
                        + " * of must not break a deployed client, and must not have what it said"
                        + " thrown away either: the\n"
                        + " * raw string stays readable through {@link #wire()}, and"
                        + " {@link #isKnown()} says it is new.\n"
                        + " */\n"
                        + "public interface Vocabulary {\n\n"
                        + "    /** The exact string the API uses. */\n"
                        + "    String wire();\n\n"
                        + "    /** Whether this is one of the values this contract snapshot"
                        + " declares. */\n"
                        + "    boolean isKnown();\n"
                        + "}\n";
        gen.putFile(gen.javaFile(PKG, "Vocabulary"), source);
    }

    /**
     * One vocabulary, as an <b>open</b> value type: the known values are interned constants that can
     * be compared with {@code ==}, and a value the gateway starts sending that this snapshot has
     * never heard of is kept exactly as it arrived rather than collapsed into a sentinel. Losing the
     * observed string would leave a caller unable to log, branch on, or report what the gateway
     * actually said.
     */
    private void emitEnum(String name, List<String> values, String doc) {
        StringBuilder sb = new StringBuilder(gen.header());
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonCreator;\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n");
        sb.append("import java.util.LinkedHashMap;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n * ").append(doc).append("\n *\n");
        sb.append(" * <p>An open vocabulary. The values below are the ones this contract snapshot")
                .append(" knows, and they are\n * interned: {@code status == ")
                .append(name)
                .append(".")
                .append(enumConstant(values.get(0)))
                .append("} works. A value the\n * gateway starts sending that is not among them")
                .append(" decodes to an instance carrying that exact\n * string —")
                .append(" {@link #wire()} tells you what it was, {@link #isKnown()} that it is new —")
                .append(" so a\n * gateway that grows its vocabulary neither breaks a deployed")
                .append(" client nor hides what it said.\n */\n");
        sb.append("public final class ").append(name).append(" implements Vocabulary {\n\n");
        for (String v : values) {
            sb.append("    /** {@code ").append(v).append("} */\n");
            sb.append("    public static final ")
                    .append(name)
                    .append(" ")
                    .append(enumConstant(v))
                    .append(" = new ")
                    .append(name)
                    .append("(\"")
                    .append(v)
                    .append("\");\n\n");
        }
        sb.append("    private static final Map<String, ")
                .append(name)
                .append("> KNOWN = new LinkedHashMap<>();\n\n");
        sb.append("    /** Every value this snapshot knows, in the gateway's own order. */\n");
        sb.append("    public static final List<").append(name).append("> VALUES = List.of(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(i % 4 == 0 ? ",\n            " : ", ");
            sb.append(enumConstant(values.get(i)));
        }
        sb.append(");\n\n");
        sb.append("    static {\n        for (")
                .append(name)
                .append(" value : VALUES) KNOWN.put(value.wire, value);\n    }\n\n");
        sb.append("    private final String wire;\n\n");
        sb.append("    private ")
                .append(name)
                .append("(String wire) {\n        this.wire = wire;\n    }\n\n");
        sb.append("    /** The exact string the API uses. */\n");
        sb.append("    @JsonValue\n    @Override\n    public String wire() {\n")
                .append("        return wire;\n    }\n\n");
        sb.append("    /** Whether this is one of the values this contract snapshot declares. */\n");
        sb.append("    @Override\n    public boolean isKnown() {\n")
                .append("        return KNOWN.get(wire) == this;\n    }\n\n");
        sb.append("    /**\n     * Decodes a wire value. A value outside this snapshot's vocabulary")
                .append(" is kept as it arrived,\n     * readable through {@link #wire()}.\n     *\n")
                .append("     * @param wire the string the API sent\n")
                .append("     * @return the interned constant, or a new instance carrying the raw")
                .append(" value; null for null\n     */\n");
        sb.append("    @JsonCreator\n    public static ")
                .append(name)
                .append(" of(String wire) {\n")
                .append("        if (wire == null) return null;\n")
                .append("        ")
                .append(name)
                .append(" known = KNOWN.get(wire);\n")
                .append("        return known != null ? known : new ")
                .append(name)
                .append("(wire);\n    }\n\n");
        sb.append("    /**\n     * Alias of {@link #of(String)}.\n     *\n")
                .append("     * @param wire the string the API sent\n")
                .append("     * @return the value\n     */\n");
        sb.append("    public static ")
                .append(name)
                .append(" from(String wire) {\n        return of(wire);\n    }\n\n");
        sb.append("    @Override\n    public boolean equals(Object other) {\n")
                .append("        return other instanceof ")
                .append(name)
                .append(" value && value.wire.equals(wire);\n    }\n\n");
        sb.append("    @Override\n    public int hashCode() {\n        return wire.hashCode();\n    }\n\n");
        sb.append("    /** The observed wire value — including one this snapshot does not know. */\n");
        sb.append("    @Override\n    public String toString() {\n        return wire;\n    }\n}\n");
        gen.putFile(gen.javaFile(PKG, name), sb.toString());
    }

    private static String enumConstant(String wire) {
        return wire.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
    }

}
