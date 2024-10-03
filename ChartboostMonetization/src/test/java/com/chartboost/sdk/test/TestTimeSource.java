package com.chartboost.sdk.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import com.chartboost.sdk.internal.Libraries.TimeSource;
import com.chartboost.sdk.internal.Model.TimeSourceBodyFields;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TestTimeSource extends TimeSource {
    private long simulatedCurrentTimeMillis;
    private long simulatedNanoTime;
    private long simulatedUptimeMillis;

    public TestTimeSource() {
        final Random random = new Random();
        simulatedCurrentTimeMillis = System.currentTimeMillis();

        // Add in some randomness to catch code that's still using System.nanoTime()
        // if tests are expecting that the code is using TimeSource.nanoTime()
        simulatedNanoTime = System.nanoTime()
                + TimeUnit.DAYS.toNanos((random.nextBoolean() ? -1 : +1) * (2 + random.nextInt(6)));

        simulatedUptimeMillis = TimeUnit.MINUTES.toMillis(5 + random.nextInt(600));
    }

    public void advanceUptime(long n, TimeUnit timeUnit) {
        assertThat(n, is(greaterThanOrEqualTo(0L)));
        final long ms = timeUnit.toMillis(n);
        final long ns = timeUnit.toNanos(n);

        simulatedCurrentTimeMillis += ms;
        simulatedNanoTime += ns;
        simulatedUptimeMillis += ms;
    }

    public void advanceDeepSleep(long n, TimeUnit timeUnit) {
        assertThat(n, is(greaterThanOrEqualTo(0L)));
        final long ms = timeUnit.toMillis(n);
        final long ns = timeUnit.toNanos(n);

        simulatedCurrentTimeMillis += ms;
        simulatedNanoTime += ns;
    }

    @Override
    public long currentTimeMillis() {
        return simulatedCurrentTimeMillis;
    }

    @Override
    public long nanoTime() {
        return simulatedNanoTime;
    }

    @Override
    public long uptimeMillis() {
        return simulatedUptimeMillis;
    }

    public long uptimeMillisPlus(int n, TimeUnit timeUnit) {
        return uptimeMillis() + timeUnit.toMillis(n);
    }

    public int epochTimePlus(int n, TimeUnit timeUnit) {
        return (int) (TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()) + timeUnit.toSeconds(n));
    }

    public long nanoTimePlus(long n, TimeUnit timeUnit) {
        return nanoTime() + timeUnit.toNanos(n);
    }

    public int epochTime() {
        return (int) TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis());
    }

    public TimeSourceBodyFields toBodyFields() {
        return
                new TimeSourceBodyFields(
                        currentTimeMillis(),
                        nanoTime(),
                        uptimeMillis());
    }
}
