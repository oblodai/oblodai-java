package codegen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for the code generator. The generator must run with nothing but a JDK on the
 * path (it produces the sources the SDK compiles from), so it cannot use the SDK's own Jackson.
 */
final class Json {
    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.pos != text.length()) throw p.fail("trailing content");
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> arr(Object o) {
        return (List<Object>) o;
    }

    static String str(Object o) {
        return (String) o;
    }

    private Object value() {
        char c = peek();
        switch (c) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return string();
            case 't':
                expect("true");
                return Boolean.TRUE;
            case 'f':
                expect("false");
                return Boolean.FALSE;
            case 'n':
                expect("null");
                return null;
            default:
                return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> out = new LinkedHashMap<>();
        pos++; // {
        ws();
        if (peek() == '}') {
            pos++;
            return out;
        }
        for (; ; ) {
            ws();
            String key = string();
            ws();
            if (src.charAt(pos++) != ':') throw fail("expected ':'");
            ws();
            out.put(key, value());
            ws();
            char c = src.charAt(pos++);
            if (c == '}') return out;
            if (c != ',') throw fail("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> out = new ArrayList<>();
        pos++; // [
        ws();
        if (peek() == ']') {
            pos++;
            return out;
        }
        for (; ; ) {
            ws();
            out.add(value());
            ws();
            char c = src.charAt(pos++);
            if (c == ']') return out;
            if (c != ',') throw fail("expected ',' or ']'");
        }
    }

    private String string() {
        if (src.charAt(pos++) != '"') throw fail("expected string");
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = src.charAt(pos++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw fail("bad escape \\" + e);
            }
        }
    }

    private Object number() {
        int start = pos;
        while (pos < src.length() && "-+.eE0123456789".indexOf(src.charAt(pos)) >= 0) pos++;
        String text = src.substring(start, pos);
        if (text.indexOf('.') < 0 && text.indexOf('e') < 0 && text.indexOf('E') < 0) {
            return Long.parseLong(text);
        }
        return Double.parseDouble(text);
    }

    private void expect(String literal) {
        if (!src.startsWith(literal, pos)) throw fail("expected " + literal);
        pos += literal.length();
    }

    private char peek() {
        return src.charAt(pos);
    }

    private void ws() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private IllegalStateException fail(String message) {
        return new IllegalStateException(message + " at offset " + pos);
    }
}
