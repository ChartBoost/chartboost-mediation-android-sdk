package com.chartboost.sdk.internal.Trace;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.internal.logging.Logger;

public class Trace {
    public static final String WEAKACTIVITY_CTOR = "WeakActivity.WeakActivity";
    public static final String CBUIMANAGER_ON_START_IMPL = "CBUIManager.onStartImpl";
    public static final String CBUIMANAGER_ON_RESUME_IMPL = "CBUIManager.onResumeImpl";
    public static final String CBUIMANAGER_ON_PAUSE_IMPL = "CBUIManager.onPauseImpl";
    public static final String CBUIMANAGER_ON_STOP_IMPL = "CBUIManager.onStopImpl";
    public static final String CBUIMANAGER_ON_DESTROY_IMPL = "CBUIManager.onDestroyImpl";
    public static final String CBUIMANAGER_ON_BACK_PRESSED_IMPL = "CBUIManager.onBackPressedImpl";
    public static final String CBUIMANAGER_SET_IMPRESSION_ACTIVITY = "CBUIManager.setImpressionActivity";
    public static final String CBUIMANAGER_CLEAR_IMPRESSION_ACTIVITY = "CBUIManager.clearImpressionActivity";

    public static final String CBUIMANAGER_QUEUE_DISPLAY_VIEW = "CBUIManager.queueDisplayView";

    private static final boolean logEnabled = BuildConfig.DEBUG;

    public static void trace(String what, String s) {
        if (logEnabled) {
            Logger.i(what + ": " + s, null);
        }
    }

    public static void trace(String what, boolean b) {
        if (logEnabled) {
            Logger.i(what + ": " + b, null);
        }
    }

    public static void trace(String what, Object ob) {
        if (logEnabled) {
            if (ob != null) {
                Logger.i(what + ": " + ob.getClass().getName() + " " + ob.hashCode(), null);
            } else {
                Logger.i( what + ": null", null);
            }
        }
    }

    public static void trace(String what) {
        if (logEnabled) {
            Logger.i(what, null);
        }
    }
}
