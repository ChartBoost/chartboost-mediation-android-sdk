package com.chartboost.sdk.internal.Libraries;

import android.os.SystemClock;

public class TimeSource {
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long nanoTime() {
        return System.nanoTime();
    }

    public long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }
}
