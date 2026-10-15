package com.everest.locker.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the same task on many threads that all start at the same instant, to give race
 * conditions the best chance to show up. Every thread waits at a starting gate (a latch) until
 * all of them are ready, then they are released together.
 */
public final class ConcurrentRunner {

    /** What one thread ended with: either a result or the exception it threw. */
    public record Outcome<T>(T result, Throwable failure) {

        public boolean succeeded() {
            return failure == null;
        }
    }

    private ConcurrentRunner() {
    }

    public static <T> List<Outcome<T>> runTogether(int threads, Callable<T> task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch startingGate = new CountDownLatch(1);
        try {
            List<Future<Outcome<T>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    startingGate.await();
                    try {
                        return new Outcome<>(task.call(), null);
                    } catch (Exception e) {
                        return new Outcome<T>(null, e);
                    }
                }));
            }
            ready.await();
            startingGate.countDown();

            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<Outcome<T>> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } catch (Exception e) {
            throw new IllegalStateException("Concurrent run did not finish", e);
        } finally {
            pool.shutdownNow();
        }
    }
}
