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
 * What a list method returns: every item across pages, page by page, or just the first page.
 *
 * <p>Nothing is requested until the pager is consumed, and the first page is fetched once however
 * many ways it is read. Iteration follows the gateway's own {@code paginate.has_pages} flag and
 * stops early on an empty page.
 *
 * <pre>{@code
 * for (PaymentView p : oblodai.payments().listHistory(params)) { ... }       // every item
 * for (Page<PaymentView> page : oblodai.payments().listHistory(params).byPage()) { ... }
 * Page<PaymentView> first = oblodai.payments().listHistory(params).firstPage();
 * List<PaymentView> recent = oblodai.payments().listHistory(params).all(1000);
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
        Page<T> fetch(long limit, long offset);
    }

    private final PageFetcher<T> fetcher;
    private final long limit;
    private final long offset;
    private Page<T> cachedFirst;

    /**
     * @param fetcher how to fetch one page
     * @param limit page size, or null for {@link #DEFAULT_LIMIT}
     * @param offset first offset, or null for 0
     */
    public Pager(PageFetcher<T> fetcher, Long limit, Long offset) {
        this(fetcher, limit, offset, null);
    }

    /**
     * @param fetcher how to fetch one page
     * @param limit page size, or null for {@link #DEFAULT_LIMIT}
     * @param offset first offset, or null for 0
     * @param first the first page when it is already at hand, else null
     */
    public Pager(PageFetcher<T> fetcher, Long limit, Long offset, Page<T> first) {
        this.fetcher = fetcher;
        this.limit = limit == null ? DEFAULT_LIMIT : limit;
        this.offset = offset == null ? 0 : offset;
        this.cachedFirst = first;
    }

    /** @return the first page - {@code items} plus {@code paginate}; fetched once and cached */
    public synchronized Page<T> firstPage() {
        if (cachedFirst == null) {
            cachedFirst = fetcher.fetch(limit, offset);
        }
        return cachedFirst;
    }

    /** @return the page size this pager walks with */
    public long limit() {
        return limit;
    }

    /** @return the offset this pager starts at */
    public long offset() {
        return offset;
    }

    /**
     * Every page in turn, one request each; the first page is reused when already fetched.
     *
     * @return the pages, lazily
     */
    public Iterable<Page<T>> byPage() {
        return () ->
                new Iterator<>() {
                    private Page<T> next;
                    private long nextOffset = offset;
                    private boolean started;
                    private boolean exhausted;

                    @Override
                    public boolean hasNext() {
                        if (next != null) {
                            return true;
                        }
                        if (exhausted) {
                            return false;
                        }
                        next = started ? fetcher.fetch(limit, nextOffset) : firstPage();
                        started = true;
                        nextOffset += next.items().size();
                        if (next.items().isEmpty() || !next.hasPages()) {
                            exhausted = true;
                        }
                        return true;
                    }

                    @Override
                    public Page<T> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Page<T> out = next;
                        next = null;
                        return out;
                    }
                };
    }

    /** Every item, page by page; each step fetches at most one page. */
    @Override
    public Iterator<T> iterator() {
        Iterator<Page<T>> pages = byPage().iterator();
        return new Iterator<>() {
            private Iterator<T> current = List.<T>of().iterator();

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && pages.hasNext()) {
                    current = pages.next().items().iterator();
                }
                return current.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    /** @return every item as a lazy stream */
    public Stream<T> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    /** @return every item, collected; use {@link #all(int)} when the result set may be large */
    public List<T> all() {
        return all(Integer.MAX_VALUE);
    }

    /**
     * Items collected across pages, stopping at a cap without fetching a page past it.
     *
     * @param maxItems most items to collect
     * @return the items
     */
    public List<T> all(int maxItems) {
        List<T> out = new ArrayList<>();
        if (maxItems <= 0) {
            return out;
        }
        Iterator<T> it = iterator();
        while (out.size() < maxItems && it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }

    @Override
    public String toString() {
        return "Pager(limit=" + limit + ", offset=" + offset + ", " + (cachedFirst == null ? "not fetched" : "fetched") + ")";
    }
}
