package com.chartboost.sdk.test;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.TimeSource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ManualExecutorService extends ScheduledExecutionQueue implements ScheduledExecutorService {
    public ManualExecutorService(TimeSource timeSource, List<Runnable> runnablesToRunAfterAnyRunnable) {
        super(nanoTimeReader(timeSource), TimeUnit.NANOSECONDS, runnablesToRunAfterAnyRunnable);
    }

    @Override
    public void execute(@NonNull Runnable runnable) {
        scheduleNow(runnable);
    }

    @NonNull
    @Override
    public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        return scheduleAfter(command, delay, unit);
    }

    @Override
    public void shutdown() {
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        throw new Error("not implemented");
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
    public boolean awaitTermination(long l, @NonNull TimeUnit timeUnit) throws InterruptedException {
        return false;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable runnable, T t) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection) throws InterruptedException {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) throws InterruptedException {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
        throw new Error("not implemented");
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new Error("not implemented");
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new Error("not implemented");
    }
}
