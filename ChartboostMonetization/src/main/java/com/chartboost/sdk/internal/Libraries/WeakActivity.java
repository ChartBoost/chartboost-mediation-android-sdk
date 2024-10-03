package com.chartboost.sdk.internal.Libraries;

import static com.chartboost.sdk.internal.Trace.Trace.WEAKACTIVITY_CTOR;
import static com.chartboost.sdk.internal.Trace.Trace.trace;

import android.app.Activity;

import java.lang.ref.WeakReference;

public final class WeakActivity extends WeakReference<Activity> {
    public final int activityHashCode;

    public WeakActivity(Activity activity) {
        super(activity);
        trace(WEAKACTIVITY_CTOR, activity);
        this.activityHashCode = activity.hashCode();
    }

    public boolean equalsActivity(Activity target) {
        return target != null && (target.hashCode() == activityHashCode);
    }

    @Override
    public int hashCode() {
        return this.activityHashCode;
    }
}
