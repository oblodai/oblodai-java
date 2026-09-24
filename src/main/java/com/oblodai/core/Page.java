package com.oblodai.core;

import java.util.Iterator;
import java.util.List;

/**
 * One page of a list route: {@code {items, paginate}}. Iterating a page walks THIS page only; the
 * {@link Pager} it came from walks every page.
 *
 * @param items the items of this page
 * @param paginate totals and the "there is more" flag
 * @param <T> item type
 */
public record Page<T>(List<T> items, Paginate paginate) implements Iterable<T> {

    /** @return whether the gateway says more pages follow */
    public boolean hasPages() {
        return paginate.hasPages();
    }

    /** @return how many items match the query in total */
    public long total() {
        return paginate.total();
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
