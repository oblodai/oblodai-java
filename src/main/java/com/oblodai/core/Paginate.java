package com.oblodai.core;

import java.util.Map;

/**
 * The pagination block of a list result.
 *
 * @param total how many items match the query in total
 * @param perPage page size the gateway applied
 * @param offset offset of this page
 * @param hasPages the gateway's own "there is more" flag - iteration stops on it
 */
public record Paginate(long total, long perPage, long offset, boolean hasPages) {

    /**
     * @param data the {@code paginate} object of a list result
     * @return it, with absent fields read as zero / false
     */
    public static Paginate fromMap(Map<?, ?> data) {
        return new Paginate(
                number(data.get("total")),
                number(data.get("per_page")),
                number(data.get("offset")),
                Boolean.TRUE.equals(data.get("has_pages")));
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
