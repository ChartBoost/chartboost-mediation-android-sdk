package com.chartboost.sdk.internal.External;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.chartboost.sdk.internal.logging.Logger;
import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.gms.tasks.Task;

/*
    The methods in the Android os libraries return null/0 in unit tests.
    This class exists to provide a way to mock this behavior for tests.
    //TODO this class should be removed at some point
 */
public class Android {
    private static Android instance = new Android();

    public static Android instance() {
        return instance;
    }

    public static void initialize(Android instance) {
        Android.instance = instance;
    }

    public String getReleaseVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Needs to be placed here for the unit test purposes
     * 
     * @param context The current context
     * @return The AppSetIdInfo task
     */
    public Task<AppSetIdInfo> getAppSetIdTask(Context context) {
        try {
            return AppSet.getClient(context).getAppSetIdInfo();
        } catch (Exception e) {
            Logger.e("Cannot retrieve appSetId client", e);
            return null;
        }
    }

    public boolean isEmpty(CharSequence str) {
        return TextUtils.isEmpty(str);
    }
}
