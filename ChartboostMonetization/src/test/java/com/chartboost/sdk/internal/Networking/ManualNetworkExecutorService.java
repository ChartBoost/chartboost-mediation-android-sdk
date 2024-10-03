package com.chartboost.sdk.internal.Networking;

import static org.junit.Assert.assertFalse;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executor service to manually step through processing of network and background processing
 * requests (on the networkExecutor) rather than having them run in background threads.
 */
public class ManualNetworkExecutorService implements ExecutorService {
    private final PriorityBlockingQueue<Runnable> queue;
    private final List<Runnable> runnablesToRunAfterAnyRunnable;

    public ManualNetworkExecutorService(List<Runnable> runnablesToRunAfterAnyRunnable) {
        queue = new PriorityBlockingQueue<>();
        this.runnablesToRunAfterAnyRunnable = runnablesToRunAfterAnyRunnable;
    }

    @Override
    public void execute(Runnable runnable) {
        queue.add(runnable);
    }

    public void runNext() {
        assertFalse(queue.isEmpty());
        Runnable r = queue.remove();
        r.run();

        for (Runnable r2 : runnablesToRunAfterAnyRunnable) {
            r2.run();
        }
    }

    public Runnable peek() {
        return queue.peek();
    }

    public <T> T peek(Class<T> expectedClass) {
        Runnable ready = peek();
        return ready != null ? expectedClass.cast(ready) : null;
    }


    @Override
    public void shutdown() {

    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return false;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> Future<T> submit(Runnable runnable, T t) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public Future<?> submit(Runnable runnable) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
        throw new Error("not implemented");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new Error("not implemented");
    }

    public int queueSize() {
        return queue.size();
    }
}
