package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExternalCallExecutor}: results always come back in input order, work actually
 * runs concurrently when a pool is configured, and unchecked exceptions surface unwrapped.
 */
class ExternalCallExecutorTest {

    @Test
    void sequentialExecutorRunsInlineAndKeepsOrder() {
        ExternalCallExecutor executor = ExternalCallExecutor.sequential();

        List<Integer> out = executor.map(List.of(1, 2, 3, 4, 5), n -> n * n);

        assertThat(out).containsExactly(1, 4, 9, 16, 25);
    }

    @Test
    void pooledExecutorKeepsInputOrderEvenWhenTasksFinishOutOfOrder() {
        ExternalCallExecutor executor = new ExternalCallExecutor(8);
        List<Integer> input = IntStream.range(0, 40).boxed().toList();

        List<Integer> out = executor.map(input, n -> {
            // later items finish first
            sleep(n % 8 == 0 ? 30 : 1);
            return n * 10;
        });

        assertThat(out).isEqualTo(input.stream().map(n -> n * 10).toList());
    }

    @Test
    void pooledExecutorActuallyRunsTasksOnSeveralThreads() {
        ExternalCallExecutor executor = new ExternalCallExecutor(6);
        var threads = ConcurrentHashMap.<String>newKeySet();

        executor.map(IntStream.range(0, 60).boxed().toList(), n -> {
            threads.add(Thread.currentThread().getName());
            sleep(2);
            return n;
        });

        assertThat(threads).hasSizeGreaterThan(1);
    }

    @Test
    void anUncheckedExceptionFromACallPropagatesUnwrapped() {
        ExternalCallExecutor executor = new ExternalCallExecutor(4);

        assertThatThrownBy(() -> executor.map(List.of(1, 2, 3), n -> {
            if (n == 2) {
                throw new IllegalStateException("boom");
            }
            return n;
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
