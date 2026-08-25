package com.oblodai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The non-blocking counterpart of {@link Pager}: one page as a future, or every item walked page by
 * page without ever blocking a thread. Nothing is requested until a method is called.
 *
 * @param <T> item type
 */
public final class AsyncPager<T> {

    /** Fetches one page without blocking. */
    @FunctionalInterface
    public interface PageFetcher<T> {
        /**
         * @param limit page size
         * @param offset offset into the result set
         * @return a future of that page
         */
        CompletableFuture<Page<T>> fetch(int limit, int offset);
    }

    private final PageFetcher<T> fetcher;
    private final int limit;
    private final int offset;

    /**
     * @param fetcher how to fetch one page
     * @param limit page size, or null for {@link Pager#DEFAULT_LIMIT}
     * @param offset first offset, or null for 0
     */
    public AsyncPager(PageFetcher<T> fetcher, Integer limit, Integer offset) {
        this.fetcher = fetcher;
        this.limit = limit == null ? Pager.DEFAULT_LIMIT : limit;
        this.offset = offset == null ? 0 : offset;
    }

    /** The first page — {@code items} plus {@code paginate}. */
    public CompletableFuture<Page<T>> firstPage() {
        return fetcher.fetch(limit, offset);
    }

    /** Every item, collected. Use {@link #all(int)} when the result set may be large. */
    public CompletableFuture<List<T>> all() {
        return all(Integer.MAX_VALUE);
    }

    /**
     * Items collected across pages, stopping at a cap.
     *
     * @param maxItems most items to collect
     * @return a future of the items
     */
    public CompletableFuture<List<T>> all(int maxItems) {
        List<T> out = new ArrayList<>();
        return walk(offset, maxItems, out::add).thenApply(ignored -> out);
    }

    /**
     * Walks every item, handing each to a consumer as its page arrives.
     *
     * @param consumer called once per item, in order
     * @return a future that completes when the last page has been consumed
     */
    public CompletableFuture<Void> forEach(Consumer<T> consumer) {
        return walk(offset, Integer.MAX_VALUE, consumer);
    }

    private CompletableFuture<Void> walk(int at, int remaining, Consumer<T> consumer) {
        if (remaining <= 0) return CompletableFuture.completedFuture(null);
        return fetcher
                .fetch(limit, at)
                .thenCompose(
                        page -> {
                            List<T> items = page.items();
                            int taken = 0;
                            for (T item : items) {
                                if (taken >= remaining) break;
                                consumer.accept(item);
                                taken++;
                            }
                            boolean more =
                                    !items.isEmpty()
                                            && page.paginate() != null
                                            && Boolean.TRUE.equals(page.paginate().hasPages())
                                            && taken < remaining;
                            if (!more) return CompletableFuture.completedFuture(null);
                            return walk(at + items.size(), remaining - taken, consumer);
                        });
    }
}
