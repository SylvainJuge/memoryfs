package com.github.sylvainjuge.memoryfs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps {@link java.util.concurrent.ExecutorService} for tests with {@link java.lang.AutoCloseable} implementation,
 * thus allowing to use "try with resources" within tests requiring disposable thread pools.
 */
public class TestExecutorService implements AutoCloseable {

    private static final int WAIT_TIMEOUT_SECONDS = 5;

    private final ExecutorService pool;

    private TestExecutorService(ExecutorService pool) {
        this.pool = pool;
    }

    public static TestExecutorService wrap(ExecutorService pool) {
        return new TestExecutorService(pool);
    }

    @Override
    public void close() {
        pool.shutdown();

        try {
            // wait tasks to terminate, then cancel them if they don't
            // then wait again and throw exception if they don't
            if (!pool.awaitTermination(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
            if (!pool.awaitTermination(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("thread pool not properly shut down after timeout");
            }
        } catch (InterruptedException e) {
            // this thread have been interrupted while awaiting termination
            // we have to request proper shutdown, and reset current thread interrupt flag
            pool.shutdown();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return wrapped execution pool
     */
    public ExecutorService getPool() {
        return pool;
    }

}

