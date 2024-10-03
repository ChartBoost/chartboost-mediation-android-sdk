package com.chartboost.sdk.internal.AssetLoader;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.FileCache;
import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.RequestBodyBuilder;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.CBNetworkService;
import com.chartboost.sdk.internal.Networking.EndpointRepository;
import com.chartboost.sdk.internal.Networking.EndpointRepositoryBase;
import com.chartboost.sdk.internal.Networking.EndpointRepositoryBaseKt;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.ErrorEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

import org.json.JSONObject;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/*
    Combined native/webview prefetcher:
        - Submits either webview or native prefetch requests to the adserver.
        - Forwards the asset list from the response to the Downloader.
        - Enforces a delay between prefetch requests, unless switching between webview and native.

    STATES
    ===============================================================================
    IDLE     doing nothing
    AWAIT    waiting for /api/prefetch or /webview/v2/prefetch response
    DOWNLOAD waiting for Downloader to finish
    COOLDOWN delay after a prefetch attempt

    INCOMING CALLS              All public methods are no-throw.
    ===============================================================================
    prefetch   Try to start prefetching if enabled.  Cancel if disabled.
    cancel     Cancel current prefetch
    onSuccess  received /api/prefetch response
    onFailure  /api/prefetch request failed

    OUTGOING CALLS
    ===============================================================================
    Downloader
      downloadNativeVideos      Download files given /api/prefetch response
      downloadWebViewAssets     Download files given /webview/v2/prefetch response
      cancel                    Cancel earlier download requests
    AssetManager
      getWebViewCacheAssets()
      getNativeVideoList()

    Deadlocks are prevented by:
     - Dependencies are unidirectional between Prefetcher, Downloader, and AssetManager.
     - AssetManager posts external calls rather than calling directly to prevent recursion.
 */
public class Prefetcher implements CBRequest.CBAPINetworkResponseCallback {
    static final int STATE_IDLE = 1;
    static final int STATE_AWAIT_PREFETCH_RESPONSE = 2;
    static final int STATE_DOWNLOAD_ASSETS = 3;
    static final int STATE_COOLDOWN = 4;

    static final int MODE_NONE = 0;
    static final int MODE_WEBVIEW = 2;

    public Downloader downloader;
    private final FileCache fileCache;
    private final CBNetworkService networkService;
    private final RequestBodyBuilder requestBodyBuilder;
    private final AtomicReference<SdkConfiguration> sdkConfig;
    private final EventTracker eventTracker;
    private final EndpointRepository endpointRepository;
    // -------------------------------
    private int state = STATE_IDLE;
    private int mode = MODE_NONE;
    private long cooldownExpiresAt = 0; // nanoTime
    private CBRequest currentPrefetchRequest = null; // non-null |   null   |   null <- What does this even mean?
    private AtomicInteger remainingDownloads = null; //   null   | non-null |   null <- What does this even mean?

    /* And state can be calculated:
        IDLE: mode == MODE_NONE
        AWAIT: currentPrefetchRequest != null
        DOWNLOAD: remainingDownloads != null
        COOLDOWN: cooldownExpiresAt != 0 && currentPrefetchRequest == null && remainingDownloads == null
     */

    public Prefetcher(
            Downloader downloader,
            FileCache fileCache,
            CBNetworkService networkService,
            RequestBodyBuilder requestBodyBuilder,
            AtomicReference<SdkConfiguration> sdkConfig,
            EventTracker eventTracker,
            EndpointRepository endpointRepository
    ) {
        this.downloader = downloader;
        this.fileCache = fileCache;
        this.networkService = networkService;
        this.requestBodyBuilder = requestBodyBuilder;
        this.sdkConfig = sdkConfig;
        this.eventTracker = eventTracker;
        this.endpointRepository = endpointRepository;
    }

    /*
        prefetch():
        1. Stop the current prefetch if that mode (native/webview) is disabled now.
        2. Cancel and exit if prefetch is disabled
        3. Advance state from DOWNLOAD -> COOLDOWN if downloads are finished
        4. Advance state from COOLDOWN -> IDLE if the cooldown timer has elapsed
        5. If now in state IDLE, issue a new prefetch and advance state to AWAIT.
     */
    public synchronized void prefetch() {
        try {
            Logger.i("Sdk Version = " + BuildConfig.SDK_VERSION + ", Commit: " + BuildConfig.RELEASE_COMMIT_HASH, null);
            final SdkConfiguration sdkConfig = this.sdkConfig.get();

            // Reset any active but now-disabled prefetch mode
            cancelDisabledPrefetchMode(sdkConfig);

            if (sdkConfig.getPublisherDisable() || sdkConfig.getPrefetchDisable()) {
                cancel();
                return;
            }

            // Note that if all downloads have completed and the cooldown timer has expired,
            // both of these transitions will happen now:
            // DOWNLOAD -> COOLDOWN followed immediately by COOLDOWN -> IDLE

            if (state == STATE_DOWNLOAD_ASSETS) {
                if (remainingDownloads.get() > 0)
                    return;

                Logger.d("Change state to COOLDOWN", null);
                state = STATE_COOLDOWN;
                remainingDownloads = null;
            }

            if (state == STATE_COOLDOWN) {
                long cooldownRemainingNs = cooldownExpiresAt - System.nanoTime();
                if (cooldownRemainingNs > 0) {
                    Logger.d("Prefetch session is still active. Won't be making any new prefetch until the prefetch session expires", null);
                    return;
                }
                Logger.d("Change state to IDLE", null);
                state = STATE_IDLE;
                mode = MODE_NONE;
                cooldownExpiresAt = 0;
            }

            if (state != STATE_IDLE)
                return;

            if (sdkConfig.isWebviewEnabled()) {
                final URL url = endpointRepository.getEndPointUrl(EndpointRepositoryBase.EndPoint.PREFETCH);
                CBWebViewRequest request = new CBWebViewRequest(
                        CBNetworkRequest.Method.POST,
                        EndpointRepositoryBaseKt.getCbRequestHost(url),
                        url.getPath(),
                        requestBodyBuilder.build(),
                        Priority.NORMAL,
                        null,
                        this,
                        eventTracker
                );

                JSONObject webAssetList = fileCache.getWebViewCacheAssets();
                request.appendWebViewBodyArgument(CBConstants.REQUEST_PARAM_ASSET_LIST, webAssetList);
                request.checkStatusInResponseBody = true;

                Logger.d("Change state to AWAIT_PREFETCH_RESPONSE", null);
                state = STATE_AWAIT_PREFETCH_RESPONSE;
                mode = MODE_WEBVIEW;
                cooldownExpiresAt = System.nanoTime() + TimeUnit.MINUTES.toNanos(sdkConfig.webviewPrefetchSession);
                currentPrefetchRequest = request;
            } else {
                Logger.e("Did not prefetch because neither native nor webview are enabled.", null);
                return;
            }

            networkService.submit(currentPrefetchRequest);
        } catch (Exception ex) {
            // The assumption is that if sendRequest() throws, then the
            // onSuccess/onFailure callbacks may or may not be called.
            // Change to STATE_COOLDOWN and ignore the callbacks if they do happen.
            if (state == STATE_AWAIT_PREFETCH_RESPONSE) {
                Logger.d("Change state to COOLDOWN", null);
                state = STATE_COOLDOWN;
                currentPrefetchRequest = null;
            }
            Logger.e("prefetch", ex);
        }
    }

    /*
        Cancel webview prefetch if webview is now disabled
        Cancel native prefetch if native is now disabled
        Cancel either type of prefetch if the AdUnitManager is handling the preloading.
            (When the AdUnitManager uses the v2 endpoints, it still needs this
             prefetcher to run since it will be sending the local asset list
             in the /webview/v2/{}/get request)

        Note that there is no cooldown timer, so that the presumably-enabled
        other mode can begin to prefetch immediately.
     */
    private void cancelDisabledPrefetchMode(SdkConfiguration sdkConfig) {
        final boolean webviewEnabled = sdkConfig.webviewEnabled;

        final boolean disabledModeActive = mode == MODE_WEBVIEW && !webviewEnabled;

        if (disabledModeActive) {
            Logger.d("Change state to IDLE", null);
            state = STATE_IDLE;
            mode = MODE_NONE;
            cooldownExpiresAt = 0;
            currentPrefetchRequest = null;
            AtomicInteger downloadsToCancel = remainingDownloads;
            remainingDownloads = null;
            if (downloadsToCancel != null)
                downloader.cancel(downloadsToCancel);
        }
    }

    /*
        State transitions:
            IDLE        -> IDLE
            any other   -> COOLDOWN
     */
    private synchronized void cancel() {
        if (state == STATE_AWAIT_PREFETCH_RESPONSE) {
            Logger.d("Change state to COOLDOWN", null);
            state = STATE_COOLDOWN;
            currentPrefetchRequest = null;
        } else if (state == STATE_DOWNLOAD_ASSETS) {
            Logger.d("Change state to COOLDOWN", null);
            state = STATE_COOLDOWN;
            AtomicInteger downloadsToCancel = remainingDownloads;
            remainingDownloads = null;
            if (downloadsToCancel != null)
                downloader.cancel(downloadsToCancel);
        }
    }

    /*
        onSuccess: called when the prefetch response is received.

        Pass the asset list along to the Downloader and
        change state to STATE_DOWNLOAD_ASSETS
     */
    public synchronized void onSuccess(CBRequest request, JSONObject response) {
        try {
            if (state != STATE_AWAIT_PREFETCH_RESPONSE) {
                return;
            }

            if (request != currentPrefetchRequest) {
                return;
            }

            Logger.d("Change state to DOWNLOAD_ASSETS", null);
            state = STATE_DOWNLOAD_ASSETS;
            currentPrefetchRequest = null;
            remainingDownloads = new AtomicInteger();

            if (response != null) {
                Logger.d("Got Asset list for Prefetch from server: " + response, null);
                Map<String, Asset> assets = Asset.v2PrefetchToAssets(response, sdkConfig.get().webviewCacheMaxUnits);
                downloader.downloadAssets(Priority.LOW, assets, remainingDownloads, null, "");
            }
        } catch (Exception e) {
            // The assumption here is that if the Downloader incremented downloadStatus,
            // it has taken responsibility for decrementing it at some point.
            Logger.e("prefetch onSuccess",  e);
        }
    }

    /*
        The prefetch request has failed.  Just go into cooldown.
     */
    public synchronized void onFailure(CBRequest request, CBError error) {
        String errorMsg = "Prefetch failure";
        if (error != null) {
            errorMsg = error.getErrorDesc();
        }
        eventTracker.track(
                new ErrorEvent(
                        TrackingEventName.Misc.PREFETCH_REQUEST_ERROR,
                        errorMsg,
                        "",
                        "",
                        null
                )
        );

        if (state != STATE_AWAIT_PREFETCH_RESPONSE) {
            return;
        }

        if (request != currentPrefetchRequest) {
            return;
        }

        currentPrefetchRequest = null;
        Logger.d("Change state to COOLDOWN", null);
        state = STATE_COOLDOWN;
    }

}
