package com.oblodai.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The pagination block of a list result.
 *
 * @param total how many items match the query in total
 * @param perPage page size the gateway applied
 * @param offset offset of this page
 * @param hasPages the gateway's own "there is more" flag — iteration stops on it
 */
public record Paginate(
        @JsonProperty("total") Integer total,
        @JsonProperty("per_page") Integer perPage,
        @JsonProperty("offset") Integer offset,
        @JsonProperty("has_pages") Boolean hasPages) {}
