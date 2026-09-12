package com.magomez.androidapps.movierec.support;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Runs a batch of independent external calls (TMDB identification, enrichment) on a
 * <em>bounded</em> thread pool and returns the results <b>in input order</b>.
 *
 * <p>Identifying a Letterboxd library or a candidate list is hundreds to thousands of
 * independent HTTP round-trips; done one after another they dominate the request time.
 * The pool size is fixed and configurable ({@code movierec.external.concurrency}, default
 * {@value #DEFAULT_CONCURRENCY}) so the fan-out stays under control — never an unbounded
 * flood of calls. A size of {@code 1} (or a list shorter than two) runs inline on the
 * caller thread, which is what tests use for determinism.
 *
 * <p>Stateless with respect to any data: the pool holds no results between calls.
 */
@Component
public class ExternalCallExecutor {

    static final int DEFAULT_CONCURRENCY = 12;

    private final ExecutorService pool;

    public ExternalCallExecutor(
            @Value("${movierec.external.concurrency:" + DEFAULT_CONCURRENCY + "}") int concurrency) {
        this.pool = concurrency > 1
                ? Executors.newFixedThreadPool(concurrency, daemonThreadFactory())
                : null;
    }

    /** A same-thread executor: every call runs inline, in order. For tests. */
    public static ExternalCallExecutor sequential() {
        return new ExternalCallExecutor(1);
    }

    /**
     * Applies {@code call} to every item, in parallel when a pool is configured, and
     * returns the results positionally aligned with {@code items}. Any unchecked
     * exception thrown by {@code call} propagates unwrapped, exactly as it would from a
     * plain sequential loop.
     */
    public <T, R> List<R> map(List<T> items, Function<? super T, R> call) {
        if (pool == null || items.size() < 2) {
            return items.stream().map(call).toList();
        }
        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> call.apply(item), pool))
                .toList();
        try {
            return futures.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }

    @PreDestroy
    void shutdown() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger seq = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "movierec-external-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
