package com.oblodai.resources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The query parameters a generated document accepts: which language to render it in, which format
 * to render it as, and — for the documents that cover a period — which period to cover.
 *
 * <p>{@code lang} is a two-letter code (41 are supported); when omitted the merchant's own language
 * is used. {@code format} is {@code pdf} or {@code csv}, and only where the document offers CSV —
 * the rest are always PDF. {@code from} and {@code to} are {@code YYYY-MM-DD} dates.
 *
 * <p>Setters are fluent and every field is optional: {@code new DocumentQuery().lang("de")}.
 */
public final class DocumentQuery {

    private String lang;
    private String format;
    private String from;
    private String to;

    /** An empty query — the gateway's defaults for every field. */
    public DocumentQuery() {}

    /**
     * @param value two-letter language code, one of the 41 supported
     * @return this query
     */
    public DocumentQuery lang(String value) {
        this.lang = value;
        return this;
    }

    /**
     * @return the language code, or null when the merchant's own language is to be used
     */
    public String lang() {
        return lang;
    }

    /**
     * @param value {@code pdf} or {@code csv}, where the document offers CSV
     * @return this query
     */
    public DocumentQuery format(String value) {
        this.format = value;
        return this;
    }

    /**
     * @return the format, or null for the document's default
     */
    public String format() {
        return format;
    }

    /**
     * @param value first day of the period, {@code YYYY-MM-DD}
     * @return this query
     */
    public DocumentQuery from(String value) {
        this.from = value;
        return this;
    }

    /**
     * @return the first day of the period, or null
     */
    public String from() {
        return from;
    }

    /**
     * @param value last day of the period, {@code YYYY-MM-DD}
     * @return this query
     */
    public DocumentQuery to(String value) {
        this.to = value;
        return this;
    }

    /**
     * @return the last day of the period, or null
     */
    public String to() {
        return to;
    }

    /**
     * The query as it goes on the wire. Fields left unset are omitted rather than sent empty, so
     * the gateway applies its own defaults.
     *
     * @return the set fields, in declaration order
     */
    public Map<String, Object> toQuery() {
        Map<String, Object> query = new LinkedHashMap<>();
        if (lang != null) query.put("lang", lang);
        if (format != null) query.put("format", format);
        if (from != null) query.put("from", from);
        if (to != null) query.put("to", to);
        return query;
    }

    /** A query with no fields set. */
    static DocumentQuery orEmpty(DocumentQuery query) {
        return query == null ? new DocumentQuery() : query;
    }
}
