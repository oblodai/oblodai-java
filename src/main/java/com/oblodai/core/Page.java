package com.oblodai.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One page of a list route: {@code {items, paginate}}.
 *
 * @param items the items of this page, newest first on history routes
 * @param paginate totals and the "there is more" flag
 * @param <T> item type
 */
public record Page<T>(
        @JsonProperty("items") List<T> items, @JsonProperty("paginate") Paginate paginate) {

    /** Items, never {@code null}. */
    @Override
    public List<T> items() {
        return items == null ? List.of() : items;
    }
}
