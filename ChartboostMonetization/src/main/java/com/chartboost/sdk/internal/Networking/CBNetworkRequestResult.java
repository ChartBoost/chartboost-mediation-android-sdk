package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.internal.Model.CBError;

// TODO Replace with Kotlin's Result?
public class CBNetworkRequestResult<T> {
    public final T value;
    public final CBError error;

    public static <T> CBNetworkRequestResult<T> success(T value) {
        return new CBNetworkRequestResult<>(value, null);
    }

    public static <T> CBNetworkRequestResult<T> failure(CBError error) {
        return new CBNetworkRequestResult<>(null, error);
    }

    private CBNetworkRequestResult(T value, CBError error) {
        this.value = value;
        this.error = error;
    }
}
