package com.chartboost.sdk.internal.AssetLoader;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.CBUtility;
import com.chartboost.sdk.internal.Libraries.FileCache;
import com.chartboost.sdk.internal.Libraries.TimeSource;
import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;
import com.chartboost.sdk.internal.Networking.CBNetworkService;
import com.chartboost.sdk.internal.Networking.CBReachability;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.ErrorEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/*
    Combined native/webview downloader:
     - Downloads files listed in prefetch and adget responses if not already present.
     - Downloads one file at a time based on priority.
     - Network availability is not considered.

    STATES
    ===============================================================================
    IDLE         doing nothing
    DOWNLOADING  downloading files
    PAUSED       don't download assets until resume() is called
    PAUSING      pause requested and a request is in flight

    INCOMING EVENTS
    ===============================================================================
    downloadNativeVideos()
    downloadWebViewAssets()
    cancel()                    Cancel a set of assets previously requested
    pause()                     Stop downloading until resume() called
    resume()                    Resume downloading assets
    onAssetDownloadResult()     Asset download succeeded or failed
    requestTemplate             Forward to AssetManager, then start downloading.

    OUTGOING EVENTS
    ===============================================================================
    AssetManager:
      registerTemplateManifest  Tell what files make up a template.
      registerFile              A file has been downloaded.
      requestTemplate           Forward a template request.
      downloaderFinished        No more registerFile() calls are coming.

    STATE DIAGRAM
    ===============================================================================
    +-------------------+                     +--------------------+
    |                   |----------download-->|                    |---------+
    |                   |<--cancel------------|                    |         |
    |       IDLE        |                     |    DOWNLOADING     |  request finished
    |                   |     last            |                    |         |
    |                   |<--request-----------|                    |<--------+
    +-------------------+   finished          +--------------------+
       |     ^                                 ^   |     ^     |
       |  resume                               |   |   resume  |
       |     |    +------------------resume----+   |     |     |
       |     |    |                                |     |     |
     pause   |    |   +----pause-------------------+     |   pause (could not cancel
       |     |    |   |    (canceled                     |     |    current request)
       v     |    |   v     current request)             |     v
    +-------------------+                     +--------------------+
    |                   |                     |                    |
    |                   |                     |                    |
    |      PAUSED       |<-request finished---|      PAUSING       |
    |                   |                     |                    |
    |                   |                     |                    |
    +-------------------+                     +--------------------+

*/

public class Downloader {
    static final int STATE_IDLE = 1;
    static final int STATE_DOWNLOADING = 2;
    static final int STATE_PAUSING = 3;
    static final int STATE_PAUSED = 4;

    private final Executor backgroundExecutor;
    private final CBNetworkService networkRequestService;
    private final CBReachability reachability;
    private final AtomicReference<SdkConfiguration> sdkConfig;
    private final TimeSource timeSource;
    private final FileCache fileCache;

    int state = STATE_IDLE; // DOWNLOADING | PAUSING  | PAUSED
    //======================================
    private AssetRequest currentRequest = null;       //  non-null   | non-null | null
    private final PriorityQueue<AssetInfo> pending;    // empty in IDLE, otherwise anything goes
    private final EventTracker eventTracker;

    public Downloader(
            Executor backgroundExecutor,
            FileCache fileCache,
            CBNetworkService networkRequestService,
            CBReachability reachability,
            AtomicReference<SdkConfiguration> sdkConfig,
            TimeSource timeSource,
            EventTracker eventTracker
    ) {
        this.backgroundExecutor = backgroundExecutor;
        this.fileCache = fileCache;
        this.networkRequestService = networkRequestService;
        this.reachability = reachability;
        this.sdkConfig = sdkConfig;
        this.timeSource = timeSource;
        this.eventTracker = eventTracker;
        pending = new PriorityQueue<>();
    }

    /*
        Download assets (native, webview/v2, or v3).
          - Enqueue asset requests in pending.
          - Increment remainingDownloads with count of files to download.
          - Downloader will decrement remainingDownloads as files are downloaded.
          - On completion, will dispatch callback on the background executor.

        Possible state transitions:
          IDLE -> DOWNLOADING
     */
    public synchronized void downloadAssets(
            Priority priority,
            @NonNull Map<String, Asset> assets,
            AtomicInteger remainingDownloads,
            AssetDownloadCallback callback,
            String adTypeName
    ) {
        AtomicInteger accumulatedProcessingMs = new AtomicInteger();
        AtomicReference<AssetDownloadCallback> refToCallback = new AtomicReference<>(callback);
        for (Asset asset : assets.values()) {
            AssetInfo assetInfo = new AssetInfo(
                    priority,
                    asset.filename, asset.url, asset.directory,
                    remainingDownloads,
                    refToCallback,
                    accumulatedProcessingMs,
                    adTypeName
            );
            pending.add(assetInfo);
        }
        if (state == STATE_IDLE || state == STATE_DOWNLOADING) {
            submitHighestPriorityRequest();
        }
    }

    /*
        Cancel downloads associated with the AtomicInteger that was passed
        to downloadAssets.

        Possible state transitions:
          DOWNLOADING -> IDLE
     */
    public synchronized void cancel(AtomicInteger remainingDownloads) {
        remainingDownloads.set(AssetInfo.CANCELED);

        switch (state) {
            case STATE_IDLE:
                break;

            case STATE_DOWNLOADING:
                // Comparing the AtomicInteger references here to see if the current request
                // is one of the requests we are canceling.
                //noinspection NumberEquality
                boolean cancelingCurrentRequest = currentRequest.assetInfo.downloadStatus == remainingDownloads;

                if (cancelingCurrentRequest) {
                    if (currentRequest.cancel()) {
                        currentRequest = null;
                        submitHighestPriorityRequest();
                    }
                    // otherwise it's being processed, or even has been processed
                    // and the callback has been posted but not yet executed.
                    // Wait for the callback and process the request anyway.
                    // If we ignored the callback, then the download file would be
                    // left on disk and we'd never tell the AssetManager about it.
                }
                break;

            case STATE_PAUSING:
                // already determined that we can't cancel the current request
                break;

            case STATE_PAUSED:
                break;
        }
    }

    /*
        Stop downloading until resume() is called.
          - Will cancel the current submitted request, and re-enqueue it for later,
            if a network dispatcher isn't already downloading it.

        Possible state transitions:
          IDLE        -> PAUSED
          DOWNLOADING -> PAUSED  if currentRequest can be canceled.
                      -> PAUSING if currentRequest is already in flight.
     */
    public synchronized void pause() {
        switch (state) {
            case STATE_IDLE:
                Logger.d("Change state to PAUSED", null);
                state = STATE_PAUSED;
                break;

            case STATE_DOWNLOADING:
                if (currentRequest.cancel()) {
                    pending.add(currentRequest.assetInfo);
                    currentRequest = null;
                    Logger.d("Change state to PAUSED", null);
                    state = STATE_PAUSED;
                } else {
                    Logger.d("Change state to PAUSING", null);
                    state = STATE_PAUSING;
                }
                break;

            case STATE_PAUSING:
                break;

            case STATE_PAUSED:
                break;
        }
    }

    /*
        Resume downloading, or return to IDLE if nothing to download.

        Possible state transitions:
          PAUSING     -> DOWNLOADING
          PAUSED      -> IDLE        if there are no pending downloads
                      -> DOWNLOADING if there are pending downloads
     */
    public synchronized void resume() {
        switch (state) {
            case STATE_IDLE:
                break;

            case STATE_DOWNLOADING:
                break;

            case STATE_PAUSING:
                Logger.d("Change state to DOWNLOADING", null);
                state = STATE_DOWNLOADING;
                break;

            case STATE_PAUSED:
                Logger.d("Change state to IDLE", null);
                state = STATE_IDLE;
                submitHighestPriorityRequest();
                break;
        }
    }

    /*
        An asset has been downloaded (error == null), or failed to download (error != null).
          - Decrement the downloadStatus to indicate that the download is complete.
          - Tell the AssetManager about the file if the download was successful.
          - Continue processing pending requests.

        Possible state transitions:
          DOWNLOADING -> IDLE
          PAUSING -> PAUSED
     */
    synchronized void onAssetDownloadResult(
            AssetRequest assetRequest,
            /*nullable*/ CBError error,
            /*nullable*/ CBNetworkServerResponse serverResponse
    ) {
        switch (state) {
            case STATE_IDLE:
                break;

            case STATE_DOWNLOADING:
            case STATE_PAUSING:
                if (assetRequest != currentRequest)
                    return;

                currentRequest = null;
                long processingMs = TimeUnit.NANOSECONDS.toMillis(assetRequest.processingNs);

                final AssetInfo assetInfo = assetRequest.assetInfo;
                assetInfo.accumulatedProcessingMs.addAndGet((int) processingMs);
                assetInfo.onFinished(backgroundExecutor, error == null);

                if (error == null) {
                    Logger.d("Downloaded " + assetInfo.uri, null);
                } else {
                    AssetInfo info = assetRequest.assetInfo;
                    String adTypeName = info.adTypeName;

                    String message = error.getErrorDesc();
                    Logger.d("Failed to download " + assetInfo.uri +
                            (serverResponse != null ? " Status code=" + serverResponse.getStatusCode() : "") +
                            " Error message=" + message, null);
                    String trackMessage = "Name: " + assetInfo.filename + " Url: " + assetInfo.uri + " Error: " + message;
                    eventTracker.track(
                            new ErrorEvent(
                                    TrackingEventName.Cache.ASSET_DOWNLOAD_ERROR,
                                    trackMessage,
                                    adTypeName,
                                    "",
                                    null
                            )
                    );
                }

                // Why are we checking state again in a state case?
                if (state == STATE_PAUSING) {
                    Logger.d("Change state to PAUSED", null);
                    state = STATE_PAUSED;
                } else {
                    submitHighestPriorityRequest();
                }
                break;

            // Compiler says this case is duplicated, but the warning disappears
            // when I put a comment above it ¯\_(ツ)_/¯
            case STATE_PAUSED:
                break;
        }
    }

    /*
        Submit a request that has the highest priority available.
          - If a lower-priority request is already submitted, try to cancel it.
          - If a request hasn't already been submitted, then submit one.

        All callers must be synchronized.

        Some state invariants might not hold on entry.  They will be true on exit.
         - In IDLE, pending might be nonEmpty.  Outcome: submit a request.
           - downloadAssets()
           - resume()
         - In DOWNLOADING, currentRequest might be null.  Outcome: submit a request.
           - cancel()
           - onAssetDownloadResult()

        Possible state transitions:
          IDLE        -> DOWNLOADING
          DOWNLOADING -> IDLE
     */
    private void submitHighestPriorityRequest() {
        // Cancel the current submitted request if a higher-priority request is available
        // and the current request isn't already in flight.
        if (currentRequest != null) {
            AssetInfo nextDownload = pending.peek();
            if (nextDownload != null && currentRequest.assetInfo.priority.getValue() > nextDownload.priority.getValue()) {
                if (currentRequest.cancel()) {
                    pending.add(currentRequest.assetInfo);
                    currentRequest = null;
                }
            }
        }

        // Submit the highest-priority asset request if there is no request already submitted.
        AssetInfo assetInfo;
        while (currentRequest == null && (null != (assetInfo = pending.poll()))) {
            // Don't download canceled requests.
            if (assetInfo.downloadStatus.get() <= 0) {
                continue;
            }

            File destDir = new File(fileCache.currentLocations().baseDir, assetInfo.type);
            // If external storage availability changed, FileCacheLocations.initialize() may not have
            // been able to create the directories.
            if (!destDir.exists() && !destDir.mkdirs() && !destDir.isDirectory()) {
                Logger.e("Unable to create directory " + destDir.getPath(), null);
                assetInfo.onFinished(backgroundExecutor, false);
                continue;
            }
            // Don't download a file that we already have.
            File dest = new File(destDir, assetInfo.filename);
            if (dest.exists()) {
                fileCache.touch(dest);
                assetInfo.onFinished(backgroundExecutor, true);
                continue;
            }

            currentRequest = new AssetRequest(this, reachability, assetInfo, dest, networkRequestService.getAppId());
            networkRequestService.submit(currentRequest);
        }

        if (currentRequest != null) {
            if (state != STATE_DOWNLOADING) {
                Logger.d("Change state to DOWNLOADING", null);
                state = STATE_DOWNLOADING;
            }
        } else if (state != STATE_IDLE) {
            Logger.d("Change state to IDLE", null);
            state = STATE_IDLE;
        }
    }

    /*
        reduceCacheSize deletes expired files or videos that exceed configured limits,
            but only if no downloads are currently in progress.
     */
    public synchronized void reduceCacheSize() {
        if (state != Downloader.STATE_IDLE) {
            return;
        }

        try {
            Logger.d("########### Trimming the disk cache", null);

            File baseDir = fileCache.currentLocations().baseDir;

            List<File> cachedFiles = new ArrayList<>();
            String[] topLevelDirectories = baseDir.list();
            if (topLevelDirectories != null && topLevelDirectories.length > 0) {
                for (String dirName : topLevelDirectories) {
                    if (dirName.equalsIgnoreCase(FileCache.CBDirectoryType.RequestManager) ||
                            dirName.equalsIgnoreCase(FileCache.CBDirectoryType.Track) ||
                            dirName.equalsIgnoreCase(FileCache.CBDirectoryType.Session) ||
                            dirName.equalsIgnoreCase(FileCache.CBDirectoryType.VideoCompletion) ||
                            dirName.equalsIgnoreCase(FileCache.CBDirectoryType.Precache) ||
                            dirName.contains(".")) {
                        continue;
                    }

                    cachedFiles.addAll(CBUtility.listFiles(new File(baseDir, dirName), true));
                }
            }

            File[] files = new File[cachedFiles.size()];
            cachedFiles.toArray(files);

            // Sort by last modified
            if (files.length > 1) {
                Arrays.sort(files, (f1, f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
            }
            if (files.length > 0) {
                // Fare estimate the size based on the list of files to download
                SdkConfiguration sdkConfig = this.sdkConfig.get();
                long maxBytes = sdkConfig.webviewCacheMaxBytes;
                long videoSize = fileCache.getFolderSize(fileCache.currentLocations().videosDir);
                long nowInMs = timeSource.currentTimeMillis();
                List<String> invalidateFolderList = sdkConfig.invalidateFolderList;
                Logger.d("Total local file count:" + files.length, null);
                Logger.d("Video Folder Size in bytes :" + videoSize, null);
                Logger.d("Max Bytes allowed:" + maxBytes, null);
                for (File file : files) {
                    long ageInDays = TimeUnit.MILLISECONDS.toDays(nowInMs - file.lastModified());
                    boolean isExpired = ageInDays >= sdkConfig.webviewCacheTTLDays;
                    boolean isPartiallyDownloadedFile = file.getName().endsWith(".tmp");
                    File parentFolder = file.getParentFile();

                    String absolutePath = null;
                    if (parentFolder != null) {
                        absolutePath = parentFolder.getAbsolutePath();
                    }

                    boolean isVideoFile = false;
                    if (absolutePath != null) {
                        isVideoFile = absolutePath.contains("/" + FileCache.CBDirectoryType.Videos);
                    }

                    boolean shouldDeleteVideo = ((videoSize > maxBytes) && isVideoFile);
                    boolean shouldDelete =
                            file.length() == 0 ||
                                    isPartiallyDownloadedFile ||
                                    isExpired ||
                                    invalidateFolderList.contains(parentFolder.getName()) ||
                                    shouldDeleteVideo;
                    if (shouldDelete) {
                        if (isVideoFile)
                            videoSize = videoSize - file.length();
                        Logger.d("Deleting file at path:" + file.getPath(), null);
                        if (!file.delete()) {
                            Logger.e("Unable to delete " + file.getPath(), null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("reduceCacheSize", e);
        }
    }
}
