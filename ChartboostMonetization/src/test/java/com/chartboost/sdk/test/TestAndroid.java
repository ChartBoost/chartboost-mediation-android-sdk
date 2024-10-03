package com.chartboost.sdk.test;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.internal.External.Android;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class TestAndroid extends Android {
    public String environmentExternalStorageState;
    public int sdkVersion;
    public String osReleaseVersion;
    public AdvertisingIdClient.Info advertisingIdClientInfo;
    private final String SCOPE = "1";
    private final int SET_ID = 123;

    @Override
    public String getReleaseVersion() {
        return osReleaseVersion;
    }

    @Override
    public boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    @Override
    public Task<AppSetIdInfo> getAppSetIdTask(Context context) {
        return getMockedTask();
    }

    private Task<AppSetIdInfo> getMockedTask() {
        return new Task<AppSetIdInfo>() {

            @NonNull
            @Override
            public Task<AppSetIdInfo> addOnCompleteListener(@NonNull OnCompleteListener<AppSetIdInfo> onCompleteListener) {
                onCompleteListener.onComplete(this);
                return this;
            }

            @Override
            public boolean isComplete() {
                return true;
            }

            @Override
            public boolean isSuccessful() {
                return true;
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @NonNull
            @Override
            public AppSetIdInfo getResult() {
                return new AppSetIdInfo(SCOPE, SET_ID);
            }

            @NonNull
            @Override
            public <X extends Throwable> AppSetIdInfo getResult(@NonNull Class<X> aClass) throws X {
                return new AppSetIdInfo(SCOPE, SET_ID);
            }

            @Nullable
            @Override
            public Exception getException() {
                return null;
            }

            @NonNull
            @Override
            public Task<AppSetIdInfo> addOnSuccessListener(@NonNull OnSuccessListener<? super AppSetIdInfo> onSuccessListener) {
                onSuccessListener.onSuccess(getResult());
                return this;
            }

            @Override
            public Task<AppSetIdInfo> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super AppSetIdInfo> onSuccessListener) {
                return null;
            }

            @Override
            public Task<AppSetIdInfo> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super AppSetIdInfo> onSuccessListener) {
                return null;
            }

            @Override
            public Task<AppSetIdInfo> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
                return null;
            }

            @Override
            public Task<AppSetIdInfo> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
                return null;
            }

            @Override
            public Task<AppSetIdInfo> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
                return null;
            }
        };
    }
}
