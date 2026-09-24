package com.oblodai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The non-blocking counterpart of {@link Pager}: one page as a future, every page or every item
 * walked without ever blocking a thread. Nothing is requested until a method is called.
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
        CompletableFuture<Page<T>> fetch(long limit, long offset);
    }

    private final PageFetcher<T> fetcher;
    private final long limit;
    private final long offset;
    private CompletableFuture<Page<T>> first;

    /**
     * @param fetcher how to fetch one page
     * @param limit page size, or null for {@link Pager#DEFAULT_LIMIT}
     * @param offset first offset, or null for 0
     */
    public AsyncPager(PageFetcher<T> fetcher, Long limit, Long offset) {
        this.fetcher = fetcher;
        this.limit = limit == null ? Pager.DEFAULT_LIMIT : limit;
        this.offset = offset == null ? 0 : offset;
    }

    /** @return the first page - {@code items} plus {@code paginate}; fetched once */
    public synchronized CompletableFuture<Page<T>> firstPage() {
        if (first == null || first.isCompletedExceptionally() || first.isCancelled()) {
            first = fetcher.fetch(limit, offset);
        }
        return first;
    }

    /**
     * One page at an explicit offset: what a pull-based consumer (a Kotlin {@code Flow}, a reactive
     * publisher) walks with.
     *
     * @param limit page size
     * @param offset offset into the result set
     * @return a future of that page
     */
    public CompletableFuture<Page<T>> page(long limit, long offset) {
        return fetcher.fetch(limit, offset);
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
     * Walks every page, handing each to {@code consumer} as it arrives; the next page is requested
     * only after the consumer returns.
     *
     * @param consumer called once per page, in order
     * @return a future that completes after the last page
     */
    public CompletableFuture<Void> byPage(Consumer<Page<T>> consumer) {
        return walkPages(firstPage(), offset, page -> {
            consumer.accept(page);
            return true;
        });
    }

    /** @return every item, collected; use {@link #all(int)} when the result set may be large */
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
        if (maxItems <= 0) {
            return CompletableFuture.completedFuture(out);
        }
        return walkPages(
                        firstPage(),
                        offset,
                        page -> {
                            for (T item : page.items()) {
                                if (out.size() >= maxItems) {
                                    return false;
                                }
                                out.add(item);
                            }
                            return out.size() < maxItems;
                        })
                .thenApply(ignored -> out);
    }

    /**
     * Walks every item, handing each to a consumer as its page arrives.
     *
     * @param consumer called once per item, in order
     * @return a future that completes when the last page has been consumed
     */
    public CompletableFuture<Void> forEach(Consumer<T> consumer) {
        return byPage(page -> page.items().forEach(consumer));
    }

    private CompletableFuture<Void> walkPages(
            CompletableFuture<Page<T>> pending, long at, Function<Page<T>, Boolean> onPage) {
        return pending.thenCompose(
                page -> {
                    boolean goOn = onPage.apply(page);
                    List<T> items = page.items();
                    if (!goOn || items.isEmpty() || !page.hasPages()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    long next = at + items.size();
                    return walkPages(fetcher.fetch(limit, next), next, onPage);
                });
    }
}
