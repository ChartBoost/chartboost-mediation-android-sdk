package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.internal.Libraries.CBConstants;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class CBAsync {

    /**
     * Get the Chartboost Executor Service in which to run any asynchronous task.
     * <p>
     * Ensure that the Runnable.run passed to ExecutorService.execute() does not
     * allow exceptions to escape.  If an exception does escape, the worker thread
     * will be shut down and a new one will be created.  This involves a 1M allocation
     * for the stack for the new thread, which sometimes fails.
     * <p>
     * This method either returns the Executor with ASYNC_MAX_THREADS running,
     * or calls ExecutorService.shutdown() before allowing an exception to propagate.
     */

    public static ScheduledExecutorService createBackgroundExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, CBConstants.ASYNC_THREAD_PREFIX + mCount.getAndIncrement());
            }
        };
        ScheduledThreadPoolExecutor backgroundExecutor = new ScheduledThreadPoolExecutor(CBConstants.ASYNC_MAX_THREADS, threadFactory);
        backgroundExecutor.prestartAllCoreThreads();
        return backgroundExecutor;
    }


    /**
     * Create a network executor that will run prioritzed network tasks
     * <p>
     * The executor has pool with a fixed number of threads, which are prestarted on creation
     * to avoid future Out of Memory errors
     */
    public static ExecutorService createNetworkExecutor(int numberOfThreads) {
        BlockingQueue<Runnable> networkQueue = new PriorityBlockingQueue<>();
        ThreadPoolExecutor networkExecutor = new ThreadPoolExecutor(numberOfThreads,
                numberOfThreads,
                10,
                TimeUnit.SECONDS,
                networkQueue);
        networkExecutor.prestartAllCoreThreads(); // prestart all threads to avoid causing out of memory errors when creating threads on demand
        return networkExecutor;
    }
}
