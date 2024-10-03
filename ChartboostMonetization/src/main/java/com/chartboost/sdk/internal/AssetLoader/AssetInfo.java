package com.chartboost.sdk.internal.AssetLoader;

import com.chartboost.sdk.internal.Priority;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class AssetInfo implements Comparable<AssetInfo> {
    final Priority priority;
    final String filename;
    final String uri;
    final String type;
    final String adTypeName;
    final AtomicInteger downloadStatus;
    private final AtomicReference<AssetDownloadCallback> callback;
    final AtomicInteger accumulatedProcessingMs;

    // All AssetInfo.downloadStatus references associated with a single call to
    // Downloader.downloadNativeVideos or Downloader.downloadWebViewAssets
    // will refer to the same AtomicInteger.
    // The value of the AtomicInteger indicates how many assets remain to be downloaded.
    // Any negative value indicates that the requests have been canceled.
    static final int CANCELED = -10000;

    AssetInfo(Priority priority,
              String filename,
              String uri,
              String type,
              AtomicInteger downloadStatus,
              AtomicReference<AssetDownloadCallback> callback,
              AtomicInteger accumulatedProcessingMs,
              String adTypeName) {
        this.priority = priority;
        this.filename = filename;
        this.uri = uri;
        this.type = type;
        this.downloadStatus = downloadStatus;
        this.callback = callback;
        this.accumulatedProcessingMs = accumulatedProcessingMs;
        this.adTypeName = adTypeName;
        downloadStatus.incrementAndGet();
    }

    @Override
    public int compareTo(AssetInfo another) {
        return priority.getValue() - another.priority.getValue();
    }

    void onFinished(Executor backgroundExecutor, final boolean success) {
        if (downloadStatus.decrementAndGet() == 0 || !success) {
            final AssetDownloadCallback callback = this.callback.getAndSet(null);
            if (callback != null) {
                final AssetDownloadCallbackRunnable callbackRunnable = new AssetDownloadCallbackRunnable(callback,
                        success,
                        accumulatedProcessingMs.get());
                backgroundExecutor.execute(callbackRunnable);
            }
        }
    }
}
