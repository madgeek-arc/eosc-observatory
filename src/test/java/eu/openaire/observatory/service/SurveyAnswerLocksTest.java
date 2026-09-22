package eu.openaire.observatory.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyAnswerLocksTest {
    @Test
    void concurrentHandoffsNeverCreateTwoLocksForTheSameAnswer() throws Exception {
        var locks = new SurveyAnswerLocks();
        var start = new CountDownLatch(1);
        var inside = new AtomicInteger();
        var maximumInside = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(8);
        try {
            var workers = new ArrayList<Future<?>>();
            for (int thread = 0; thread < 8; thread++) {
                final String key = thread % 2 == 0 ? "sa-1" : RedisCacheService.PREFIX + "sa-1";
                workers.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int iteration = 0; iteration < 1_000; iteration++) {
                        try (var ignored = locks.acquire(key)) {
                            maximumInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                            // Nested service/CRUD calls must retain the outer lock.
                            try (var nested = locks.acquire("sa-1")) {
                                Thread.yield();
                            }
                            inside.decrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> worker : workers) {
                worker.get(10, TimeUnit.SECONDS);
            }
            assertEquals(1, maximumInside.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void unrelatedAnswersRemainWritable() throws Exception {
        var locks = new SurveyAnswerLocks();
        var executor = Executors.newSingleThreadExecutor();
        try (var ignored = locks.acquire("sa-1")) {
            executor.submit(() -> {
                try (var other = locks.acquire("sa-2")) {
                    return true;
                }
            }).get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
