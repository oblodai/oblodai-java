package com.oblodai.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * What a list method returns: one page on demand, or every item across pages.
 *
 * <p>Nothing is requested until the pager is consumed, and the first page is fetched once however
 * many ways it is read. Iteration follows the gateway's own {@code paginate.has_pages} flag, and
 * stops early on a short page.
 *
 * <pre>{@code
 * Page<Payment> page = oblodai.payments().history(params).firstPage();
 * for (Payment p : oblodai.payments().history(params)) { ... }
 * List<Payout> recent = oblodai.payouts().history(params).all(1000);
 * }</pre>
 *
 * @param <T> item type
 */
public final class Pager<T> implements Iterable<T> {

    /** Page size used when the caller did not ask for one. */
    public static final int DEFAULT_LIMIT = 50;

    /** Fetches one page. */
    @FunctionalInterface
    public interface PageFetcher<T> {
        /**
         * @param limit page size
         * @param offset offset into the result set
         * @return that page
         */
        Page<T> fetch(int limit, int offset);
    }

    private final PageFetcher<T> fetcher;
    private final int limit;
    private final int offset;
    private Page<T> cachedFirst;

    /**
     * @param fetcher how to fetch one page
     * @param limit page size, or null for {@link #DEFAULT_LIMIT}
     * @param offset first offset, or null for 0
     */
    public Pager(PageFetcher<T> fetcher, Integer limit, Integer offset) {
        this.fetcher = fetcher;
        this.limit = limit == null ? DEFAULT_LIMIT : limit;
        this.offset = offset == null ? 0 : offset;
    }

    /** The first page — {@code items} plus {@code paginate}. Fetched once and cached. */
    public synchronized Page<T> firstPage() {
        if (cachedFirst == null) cachedFirst = fetcher.fetch(limit, offset);
        return cachedFirst;
    }

    /** Every item, page by page; each step fetches at most one page. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Iterator<T> current = List.<T>of().iterator();
            private int nextOffset = offset;
            private boolean exhausted;
            private boolean started;

            private void advance() {
                while (!current.hasNext() && !exhausted) {
                    Page<T> page = !started ? firstPage() : fetcher.fetch(limit, nextOffset);
                    started = true;
                    List<T> items = page.items();
                    nextOffset += items.size();
                    current = items.iterator();
                    boolean more =
                            !items.isEmpty()
                                    && page.paginate() != null
                                    && Boolean.TRUE.equals(page.paginate().hasPages());
                    if (!more) exhausted = true;
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return current.hasNext();
            }

            @Override
            public T next() {
                advance();
                if (!current.hasNext()) throw new NoSuchElementException();
                return current.next();
            }
        };
    }

    /** Every item as a lazy stream. */
    public Stream<T> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    /** Every item, collected. Use {@link #all(int)} when the result set may be large. */
    public List<T> all() {
        return all(Integer.MAX_VALUE);
    }

    /**
     * Items collected across pages, stopping at a cap.
     *
     * @param maxItems most items to collect
     * @return the items
     */
    public List<T> all(int maxItems) {
        List<T> out = new ArrayList<>();
        for (T item : this) {
            if (out.size() >= maxItems) break;
            out.add(item);
        }
        return out;
    }
}
