package com.github.sylvainjuge.memoryfs;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Wraps {@link java.util.concurrent.ExecutorService} for tests with {@link java.lang.AutoCloseable} implementation,
 * thus allowing to use "try with resources" within tests requiring disposable thread pools.
 */
public class TestExecutorService implements AutoCloseable, ExecutorService  {

    private static final int WAIT_TIMEOUT_SECONDS = 5;

    private final ExecutorService pool;

    private TestExecutorService(ExecutorService pool) {
        this.pool = pool;
    }

    public static <T extends ExecutorService> TestExecutorService wrap(T pool) {
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

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return pool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return pool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return pool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return pool.awaitTermination(l, timeUnit);
    }

    public <T> Future<T> submit(Callable<T> callable) {
        return pool.submit(callable);
    }

    public <T> Future<T> submit(Runnable runnable, T t) {
        return pool.submit(runnable, t);
    }

    public Future<?> submit(Runnable runnable) {
        return pool.submit(runnable);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables) throws InterruptedException {
        return pool.invokeAll(callables);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException {
        return pool.invokeAll(callables, l, timeUnit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables) throws InterruptedException, ExecutionException {
        return pool.invokeAny(callables);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return pool.invokeAny(callables, l, timeUnit);
    }


    @Override
    public void execute(Runnable runnable) {
        pool.execute(runnable);
    }
}

