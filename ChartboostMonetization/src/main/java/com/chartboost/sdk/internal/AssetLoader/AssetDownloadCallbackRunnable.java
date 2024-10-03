package com.chartboost.sdk.internal.AssetLoader;

public class AssetDownloadCallbackRunnable implements Runnable {
    private final AssetDownloadCallback callback;
    public final boolean success;

    AssetDownloadCallbackRunnable(AssetDownloadCallback callback,
                                  boolean success,
                                  int accumulatedProcessingMs) {
        this.callback = callback;
        this.success = success;
    }

    @Override
    public void run() {
        callback.assetDownloadResult(success);
    }
}
