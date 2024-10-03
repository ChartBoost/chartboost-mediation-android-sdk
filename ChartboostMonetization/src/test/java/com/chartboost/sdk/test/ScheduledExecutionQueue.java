package com.chartboost.sdk.test;

import static com.chartboost.sdk.test.TestUtils.describeDuration;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.TimeSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledExecutionQueue {
    private CurrentTimeReader timeReader;
    private final TimeUnit timeUnit;
    private final List<Runnable> runnablesToRunAfterAnyRunnable;

    private SortedSet<ScheduledRunnable> scheduledRunnables;
    private int nextScheduledRunnableId = 1;

    public ScheduledExecutionQueue(CurrentTimeReader timeReader, TimeUnit timeUnit, List<Runnable> runnablesToRunAfterAnyRunnable) {
        this.timeReader = timeReader;
        this.timeUnit = timeUnit;
        this.runnablesToRunAfterAnyRunnable = runnablesToRunAfterAnyRunnable;
        this.scheduledRunnables = new TreeSet<>();
    }

    public ScheduledFuture<?> scheduleNow(Runnable runnable) {
        return scheduleAt(runnable, timeReader.getCurrentTime());
    }

    public ScheduledFuture<?> scheduleAfter(Runnable runnable, long delay, TimeUnit delayUnit) {
        return scheduleAt(runnable, timeReader.getCurrentTime() + timeUnit.convert(delay, delayUnit));
    }

    public ScheduledFuture<?> scheduleAt(Runnable runnable, long runAt) {
        final ScheduledRunnable sr = new ScheduledRunnable(nextScheduledRunnableId++, runnable, runAt);
        scheduledRunnables.add(sr);
        return new ScheduledRunnableFuture(sr);
    }

    protected void removeAll(Runnable r) {
        for (Iterator<ScheduledRunnable> itr = scheduledRunnables.iterator(); itr.hasNext(); ) {
            ScheduledRunnable sr = itr.next();
            if (sr.r == r)
                itr.remove();
        }
    }

    public Runnable runNext() {
        final Runnable r = removeFirstReadyRunnable();
        r.run();

        for (Runnable r2 : runnablesToRunAfterAnyRunnable) {
            r2.run();
        }
        return r;
    }

    public <T> T removeFirstReadyRunnable(Class<T> expectedClass) {
        Runnable r = removeFirstReadyRunnable();
        return expectedClass.cast(r);
    }

    private Runnable removeFirstReadyRunnable() {
        assertFalse("expect to have at least one scheduled runnable", scheduledRunnables.isEmpty());
        ScheduledRunnable sr = scheduledRunnables.first();
        assertTrue("expect a ready runnable", sr.isReady());
        scheduledRunnables.remove(sr);
        return sr.r;
    }


    public Runnable peekReady() {
        if (scheduledRunnables.isEmpty())
            return null;

        ScheduledRunnable sr = scheduledRunnables.first();
        return sr.isReady() ? sr.r : null;
    }

    <T> T peekReady(Class<T> expectedClass) {
        Runnable ready = peekReady();
        return ready != null ? expectedClass.cast(ready) : null;
    }

    public <T> T peekRunnablePostedForTime(Class<T> expectedClass, long runAt) {
        Runnable found = null;

        for (ScheduledRunnable sr : scheduledRunnables) {
            if (sr.runAt == runAt) {
                assertNull("more than one runnable posted at time", found);
                found = sr.r;
            }
        }

        return expectedClass.cast(found);
    }

    public int readyCount() {
        int result = 0;
        for (ScheduledRunnable sr : scheduledRunnables) {
            if (sr.isReady())
                result++;
            else
                break;
        }
        return result;
    }

    public int allScheduledRunnablesCount() {
        return scheduledRunnables.size();
    }

    public <T> T removeFirst(@NonNull Class<T> cls) {
        for (Iterator<ScheduledRunnable> itr = scheduledRunnables.iterator(); itr.hasNext(); ) {
            ScheduledRunnable sr = itr.next();
            if (cls.isAssignableFrom(sr.r.getClass())) {
                itr.remove();
                return cls.cast(sr.r);
            }
        }
        return null;
    }

    public long timeRemainingUntilNextScheduledRunnable(TimeUnit unit) {
        assertFalse("expect to have at least one scheduled runnable", scheduledRunnables.isEmpty());

        ScheduledRunnable sr = scheduledRunnables.first();
        return sr.getDelay(unit);
    }

    public String toString() {
        int numReady = 0;
        List<String> timeRemaining = new ArrayList<>();

        for (ScheduledRunnable sr : scheduledRunnables) {
            if (sr.isReady())
                numReady++;
            else {
                timeRemaining.add(describeDuration(sr.getDelay(timeUnit), timeUnit));
            }
        }

        StringBuilder sb = new StringBuilder();
        if (numReady == 0 && timeRemaining.size() == 1) {
            sb.append("next in ");
            sb.append(timeRemaining.get(0));
        } else {
            if (numReady > 0) {
                sb.append(numReady);
                sb.append(" ready");
            }
            if (!timeRemaining.isEmpty()) {
                if (sb.length() != 0)
                    sb.append(" then ");
                sb.append("scheduled in ");
                sb.append(TestUtils.join(timeRemaining, ", "));
            }
            if (sb.length() == 0) {
                sb.append("nothing posted");
            }
        }

        return sb.toString();
    }

    private class ScheduledRunnable implements Comparable<ScheduledRunnable> {
        private final int id;
        final Runnable r;
        final long runAt;

        ScheduledRunnable(int id, Runnable r, long runAt) {
            this.id = id;
            this.r = r;
            this.runAt = runAt;
        }

        @Override
        public int compareTo(@NonNull ScheduledRunnable another) {
            long diff = runAt - another.runAt;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else
                return id - another.id; // keep FIFO order if execution time is the same.
        }

        private long getDelay(TimeUnit unit) {
            long baseTimeRemaining = Math.max(0, runAt - currentTime());
            return unit.convert(baseTimeRemaining, timeUnit);
        }

        private boolean isReady() {
            return getDelay(TimeUnit.NANOSECONDS) == 0;
        }
    }

    private long currentTime() {
        return timeReader.getCurrentTime();
    }

    private class ScheduledRunnableFuture implements ScheduledFuture<Object> {
        private final ScheduledRunnable scheduledRunnable;

        ScheduledRunnableFuture(ScheduledRunnable scheduledRunnable) {
            this.scheduledRunnable = scheduledRunnable;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return scheduledRunnable.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed another) {
            throw new Error("not implemented");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return scheduledRunnables.remove(scheduledRunnable);
        }

        @Override
        public boolean isCancelled() {
            throw new Error("not implemented");
        }

        @Override
        public boolean isDone() {
            throw new Error("not implemented");
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            throw new Error("not implemented");
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new Error("not implemented");
        }
    }

    protected interface CurrentTimeReader {
        long getCurrentTime();
    }

    protected static CurrentTimeReader nanoTimeReader(final TimeSource timeSource) {
        return new CurrentTimeReader() {

            @Override
            public long getCurrentTime() {
                return timeSource.nanoTime();
            }
        };
    }

    protected static CurrentTimeReader uptimeMillisReader(final TimeSource timeSource) {
        return new CurrentTimeReader() {

            @Override
            public long getCurrentTime() {
                return timeSource.uptimeMillis();
            }
        };
    }
}
