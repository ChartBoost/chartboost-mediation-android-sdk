package com.chartboost.sdk.internal.Networking;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.tracking.EventTracker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

public class CBNetworkServiceTest extends BaseTest {

    private final EventTracker eventTrackerMock = mock(EventTracker.class);

    @Captor
    private ArgumentCaptor<PriorityBlockingQueue<Runnable>> networkRequestQueueCaptor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    /*
        CBNetworkService.submit(request) should add the request to the network queue.
     */
    @Test
    public void testSubmitAddsToNetworkQueue() throws InterruptedException {
        try (TestContainer tc = new TestContainer()) {
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            final NetworkFactory factory = mock(NetworkFactory.class);
            ExecutorService networkExecutor = mock(ExecutorService.class);
            CBNetworkService networkService = new CBNetworkService(
                    tc.backgroundExecutor, factory,
                    tc.reachability,
                    tc.timeSource,
                    tc.uiPoster,
                    networkExecutor,
                    eventTrackerMock
            );
            CBNetworkRequest request = mock(CBNetworkRequest.class);
            networkService.submit(request);
            verify(networkExecutor).execute(runnableCaptor.capture());
        }
    }

    static class ThreadHolder implements Runnable, Comparable {
        final Semaphore runIsExecuting = new Semaphore(0);
        final Semaphore allowRunToExit = new Semaphore(0);

        @Override
        public int compareTo(Object another) {
            return 0;
        }

        @Override
        public void run() {
            runIsExecuting.release(1);
            allowRunToExit.acquireUninterruptibly(1);
        }
    }

    @Test
    public void testSubmitAddsInPriorityOrder() throws InterruptedException {
        try (TestContainer tc = new TestContainer()) {
            CBNetworkRequest requestLow = new CBNetworkRequest(CBNetworkRequest.Method.POST,
                    "uri low", Priority.LOW, null);
            CBNetworkRequest requestMedium = new CBNetworkRequest(CBNetworkRequest.Method.POST,
                    "uri medium", Priority.NORMAL, null);
            CBNetworkRequest requestHigh = new CBNetworkRequest(CBNetworkRequest.Method.POST,
                    "uri high", Priority.HIGH, null);

            final NetworkFactory factory = mock(NetworkFactory.class);

            ExecutorService networkExecutor = CBAsync.createNetworkExecutor(1); // 1 thread so all tasks are queued in the same queue

            try {
                NetworkDispatcher networkDispatcherLow = spy(
                        new NetworkDispatcher(
                                tc.backgroundExecutor,
                                factory,
                                tc.reachability,
                                tc.timeSource,
                                tc.uiPoster,
                                requestLow,
                                eventTrackerMock
                        )
                );
                NetworkDispatcher networkDispatcherMed = spy(
                        new NetworkDispatcher(
                                tc.backgroundExecutor,
                                factory,
                                tc.reachability,
                                tc.timeSource,
                                tc.uiPoster,
                                requestMedium,
                                eventTrackerMock
                        )
                );
                NetworkDispatcher networkDispatcherHigh = spy(
                        new NetworkDispatcher(
                                tc.backgroundExecutor,
                                factory,
                                tc.reachability,
                                tc.timeSource,
                                tc.uiPoster,
                                requestHigh,
                                eventTrackerMock
                        )
                );

                ThreadHolder holder = new ThreadHolder();
                networkExecutor.execute(holder);
                holder.runIsExecuting.acquireUninterruptibly(1);

                // Make it so all of these requests are in the queue at once before the
                // ExecutorService threads picks up any of them
                networkExecutor.execute(networkDispatcherLow);
                networkExecutor.execute(networkDispatcherMed);
                networkExecutor.execute(networkDispatcherHigh);

                holder.allowRunToExit.release(1);

                verify(networkDispatcherHigh, timeout(10000)).run();
                verify(networkDispatcherMed, timeout(10000)).run();
                verify(networkDispatcherLow, timeout(10000)).run();

                InOrder order = inOrder(networkDispatcherHigh, networkDispatcherMed, networkDispatcherLow);

                order.verify(networkDispatcherHigh, times(1)).run();
                order.verify(networkDispatcherMed, times(1)).run();
                order.verify(networkDispatcherLow, times(1)).run();
            } finally {
                networkExecutor.shutdownNow();
            }
        }
    }
}
