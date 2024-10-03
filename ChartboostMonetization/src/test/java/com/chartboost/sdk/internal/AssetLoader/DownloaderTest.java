package com.chartboost.sdk.internal.AssetLoader;

import static com.chartboost.sdk.internal.AssetLoader.AssetInfoMatcher.assetInfoWithUri;
import static com.chartboost.sdk.internal.AssetLoader.Downloader.STATE_DOWNLOADING;
import static com.chartboost.sdk.internal.AssetLoader.Downloader.STATE_IDLE;
import static com.chartboost.sdk.internal.AssetLoader.Downloader.STATE_PAUSED;
import static com.chartboost.sdk.internal.AssetLoader.Downloader.STATE_PAUSING;
import static com.chartboost.sdk.internal.Model.CBError.Internal.INVALID_RESPONSE;
import static com.chartboost.sdk.internal.Networking.CBNetworkRequestMatcher.matchesAssetDescriptor;
import static com.chartboost.sdk.test.AssetDescriptorMatcher.hasUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsIn.isOneOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;
import com.chartboost.sdk.internal.Networking.CBNetworkService;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.test.TestContainerControl;
import com.chartboost.sdk.test.TestUtils;

import org.hamcrest.CoreMatchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloaderTest extends BaseTest {
    //<editor-fold desc="Full-cycle tests">
    /*
        The Downloader should:
          - download all the assets.
          - inform the asset manager of each file.
          - inform the asset manager when all downloads are complete.
          - finish in the IDLE state.
     */
    @Test
    public void downloadNative_full_cycle() throws JSONException {
        final AssetDescriptor assetDescriptors[] = ReferenceResponse.apiVideoPrefetchWithResultsAssetDescriptors;
        try (TestHarness harness = TestHarness.idleNative()) {
            harness.resetMocks();

            AtomicInteger downloadStatus = new AtomicInteger();
            NativePrefetchResponse resp = new NativePrefetchResponse(ReferenceResponse.apiVideoPrefetchWithResults);

            harness.downloader.downloadAssets(Priority.LOW, resp.getAssets(), downloadStatus, harness.assetDownloadCallback, "");

            assertThat(downloadStatus.get(), is(assetDescriptors.length));

            for (int i = 0; i < assetDescriptors.length; ++i) {
                AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
                harness.onAssetDownloadSuccess(assetRequest);
            }

            assertThat(downloadStatus.get(), is(0));
            for (AssetDescriptor assetDescriptor : assetDescriptors) {
                verify(harness.networkRequestService).submit(argThat(matchesAssetDescriptor(assetDescriptor)));
            }

            harness.verifyNoAssetDownloadResult();
            harness.tc.runNextBackgroundRunnable();
            harness.tc.verifyNoMoreRunnables();

            harness.verifyOnlyAssetDownloadResult(true);
            assertThat(harness.tc.backgroundRunnableCount(), is(0));

            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        The Downloader should:
         - inform the asset manager of the template manifests.
         - download all the assets.
         - notify the asset manager of each file.
         - notify the asset manager that downloads are complete.
         - finish in the IDLE state.
     */
    @Test
    public void downloadWebView_full_cycle() throws JSONException {
        AssetDescriptor[] assetDescriptors = ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors;

        try (TestHarness harness = TestHarness.idle()) {
            harness.resetMocks();

            AtomicInteger downloadStatus = new AtomicInteger();
            WebViewPrefetchResponse resp = new WebViewPrefetchResponse(ReferenceResponse.webviewV2PrefetchWithResults);


            Map<String, Asset> assets = Asset.v2PrefetchToAssets(resp.response, 10);

            harness.downloader.downloadAssets(Priority.LOW, assets, downloadStatus, harness.assetDownloadCallback, "");

            assertThat(downloadStatus.get(), is(assetDescriptors.length));

            for (int i = 0; i < assetDescriptors.length; ++i) {
                AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
                harness.onAssetDownloadSuccess(assetRequest);
            }

            assertThat(downloadStatus.get(), is(0));

            for (AssetDescriptor assetDescriptor : assetDescriptors) {
                verify(harness.networkRequestService).submit(argThat(matchesAssetDescriptor(assetDescriptor)));
            }

            harness.verifyNoAssetDownloadResult();

            harness.tc.runNextBackgroundRunnable();
            harness.tc.verifyNoMoreRunnables();

            harness.verifyOnlyAssetDownloadResult(true);

            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        The Downloader should call the assetDownloadComplete() callback even if the last file
        to be attempted to downloaded (or all of them) is already on the filesystem.
     */
    @Test
    public void downloadWebView_lastFileAlreadyDownloaded() throws JSONException {
        AssetDescriptor[] assetDescriptors = ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors;

        try (TestHarness harness = TestHarness.idle()) {
            harness.resetMocks();

            AtomicInteger downloadStatus = new AtomicInteger();
            WebViewPrefetchResponse resp = new WebViewPrefetchResponse(ReferenceResponse.webviewV2PrefetchWithResults);

            Map<String, Asset> assets = Asset.v2PrefetchToAssets(resp.response, 10);

            harness.downloader.downloadAssets(Priority.LOW, assets, downloadStatus, harness.assetDownloadCallback, "");

            assertThat(downloadStatus.get(), is(assetDescriptors.length));

            for (int i = 0; i < assetDescriptors.length; ++i) {
                boolean secondLast = i == assetDescriptors.length - 2;
                if (secondLast) {
                    for (AssetDescriptor assetDescriptor : assetDescriptors) {
                        assetDescriptor.writeResourceToCache(harness.tc.internalBaseDir);
                    }
                }
                AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
                harness.onAssetDownloadSuccess(assetRequest);
            }

            assertThat(downloadStatus.get(), is(0));

            harness.tc.run();

            harness.verifyOnlyAssetDownloadResult(true);

            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        The Downloader should call the assetDownloadComplete() callback even if all of the
        files requested are already on the filesystem.
     */
    @Test
    public void downloadWebView_allFilesAlreadyDownloaded() throws JSONException {
        AssetDescriptor[] assetDescriptors = ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors;

        try (TestHarness harness = TestHarness.idle()) {
            harness.resetMocks();

            for (AssetDescriptor assetDescriptor : assetDescriptors) {
                assetDescriptor.writeResourceToCache(harness.tc.internalBaseDir);
            }

            AtomicInteger downloadStatus = new AtomicInteger();
            WebViewPrefetchResponse resp = new WebViewPrefetchResponse(ReferenceResponse.webviewV2PrefetchWithResults);

            Map<String, Asset> assets = Asset.v2PrefetchToAssets(resp.response, 10);

            harness.downloader.downloadAssets(Priority.LOW, assets, downloadStatus, harness.assetDownloadCallback, "");

            assertThat(downloadStatus.get(), is(0));

            harness.verifyNoAssetDownloadResult();

            harness.tc.runNextBackgroundRunnable();

            harness.verifyOnlyAssetDownloadResult(true);

            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        The Downloader should report to the AssetDownloadCallback:
         - ms between the downloadAssets call and when the completion callback was posted
         - sum of the processing time of the asset requests
     */
    @Test
    public void completionCallback_reports_timings() throws JSONException {

        try (TestHarness harness = TestHarness.idle()) {
            AtomicInteger downloadStatus = new AtomicInteger();

            AssetDescriptor[] assetDescriptors = ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors;

            Map<String, RequestTimes> requestTimes = new HashMap<>();
            for (AssetDescriptor d : assetDescriptors) {
                requestTimes.put(d.uri, RequestTimes.random());
            }

            harness.downloader.downloadAssets(Priority.LOW,
                    AssetDescriptor.toAssets(assetDescriptors),
                    downloadStatus,
                    harness.assetDownloadCallback,
                    "");

            int expectedProcessingMs = 0;
            int expectedRequestToCompletionMs = 0;
            Random r = new Random();
            for (AssetDescriptor unused : assetDescriptors) {
                AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
                RequestTimes rt = requestTimes.get(assetRequest.getUri());

                int extraProcessingMs = r.nextInt(60);
                harness.tc.advanceUptimeMs(extraProcessingMs);
                harness.onAssetDownloadSuccess(assetRequest, rt);

                final long requestProcessingMs = TimeUnit.NANOSECONDS.toMillis(rt.processingNs);
                expectedProcessingMs += requestProcessingMs;

                expectedRequestToCompletionMs += extraProcessingMs;
                expectedRequestToCompletionMs += requestProcessingMs;
            }

            harness.tc.run();

            verify(harness.assetDownloadCallback, only()).assetDownloadResult(true);

            harness.assertStateIs(STATE_IDLE);
        }
    }

    //</editor-fold>

    /*
        downloadNativeVideos() should:
         - submit an asset download request.
         - currentRequest should be that request
         - the rest of the requests should be in pendingDownloads
         - leave in STATE_DOWNLOADING
     */
    @Test
    public void downloadNativeVideos_submitFirstAssetRequest() throws JSONException {
        try (TestHarness harness = TestHarness.idleNative()) {
            AtomicInteger downloadStatus = new AtomicInteger();
            NativePrefetchResponse resp = new NativePrefetchResponse(ReferenceResponse.apiVideoPrefetchWithResults);

            JSONArray videos = resp.getVideos();
            assertThat(videos.length(), is(8));

            harness.downloader.downloadAssets(Priority.LOW, resp.getAssets(), downloadStatus, mock(AssetDownloadCallback.class), "");

            AssetRequest assetRequest = harness.getOnlySubmittedAssetRequest();

            harness.assertStateIs(STATE_DOWNLOADING);
            harness.assertCurrentRequestIs(assetRequest);
            harness.assertPendingDownloadCount(videos.length() - 1);
            assertThat(downloadStatus.get(), is(videos.length()));

            // the submitted request should be one of the filenames
            List<String> uris = resp.getVideoURIs();
            assertThat(uris, hasItem(assetRequest.getUri()));

            // the rest of the filenames should be queued up
            List<AssetInfo> pending = harness.getPendingAssetInfo();
            for (String uri : uris) {
                if (uri.equals(assetRequest.getUri())) {
                    assertThat(pending, not(hasItem(assetInfoWithUri(uri))));
                } else {
                    assertThat(pending, hasItem(assetInfoWithUri(uri)));
                }
            }
        }
    }

    /*
        Download success should advance and submit a new request
     */
    @Test
    public void successSendsTheNextRequest() {
        try (TestHarness harness = TestHarness.idleNative()) {
            AtomicInteger downloadStatus = new AtomicInteger();
            NativePrefetchResponse resp = new NativePrefetchResponse(ReferenceResponse.apiVideoPrefetchWithResults);

            JSONArray videos = resp.getVideos();
            harness.downloader.downloadAssets(Priority.LOW, resp.getAssets(), downloadStatus, mock(AssetDownloadCallback.class), "");

            AssetRequest firstRequest = harness.getOnlySubmittedAssetRequest();

            harness.assertStateIs(STATE_DOWNLOADING);

            // if the download succeeds
            harness.onAssetDownloadSuccess(firstRequest);

            // the downloader should decrement the download status
            assertThat(downloadStatus.get(), is(7));

            AssetRequest secondRequest = harness.getSubmittedAssetRequest(2);

            // the request submitted should be different than the first
            assertThat(secondRequest.getUri(), not(is(firstRequest.getUri())));

            // the submitted request should be one of the filenames
            List<String> uris = resp.getVideoURIs();
            assertThat(uris, hasItem(secondRequest.getUri()));

            // the rest of the filenames should be queued up
            List<AssetInfo> pending = harness.getPendingAssetInfo();
            for (String uri : uris) {
                if (uri.equals(firstRequest.getUri()) || uri.equals(secondRequest.getUri())) {
                    assertThat(pending, not(hasItem(assetInfoWithUri(uri))));
                } else {
                    assertThat(pending, hasItem(assetInfoWithUri(uri)));
                }
            }

            // There shouldn't be any extras
            assertThat(harness.getPendingAssetInfo().size(), is(videos.length() - 2));
        }
    }

    /*
        The Downloader should report asset download times to track.
     */
    @Test
    public void reportAssetDownloadSuccessTimesToTrack() throws JSONException {
        AssetDescriptor[] assetDescriptors = ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors;

        Map<String, RequestTimes> requestTimes = new HashMap<>();
        for (AssetDescriptor d : assetDescriptors) {
            requestTimes.put(d.uri, RequestTimes.random());
        }

        Map<String, Asset> assets = AssetDescriptor.toAssets(assetDescriptors);

        try (TestHarness h = TestHarness.idle()) {
            h.downloader.downloadAssets(Priority.LOW, assets, new AtomicInteger(), h.assetDownloadCallback, "");

            for (AssetDescriptor unused : assetDescriptors) { // no way to know which asset will be submitted first
                AssetRequest assetRequest = h.getLatestSubmittedAssetRequest();

                RequestTimes rt = requestTimes.get(assetRequest.getUri());

                h.onAssetDownloadSuccess(assetRequest, rt);
            }

            for (AssetDescriptor d : assetDescriptors) {
                RequestTimes rt = requestTimes.get(d.uri);
            }
        }
    }

    //<editor-fold desc="Function-level tests">

    //<editor-fold desc="downloadNativeVideos tests"
    /*
        downloadNativeVideos() should increment the downloadStatic AtomicInteger
        with the number of files to be downloaded.
     */
    @Test
    public void downloadNativeVideos_incrementRemainingDownloads() {
        try (TestHarness harness = TestHarness.idleNative()) {
            NativePrefetchResponse resp = new NativePrefetchResponse(ReferenceResponse.apiVideoPrefetchWithResults);
            AtomicInteger downloadStatus = new AtomicInteger();
            JSONArray videos = resp.getVideos();
            harness.downloader.downloadAssets(Priority.LOW, resp.getAssets(), downloadStatus, mock(AssetDownloadCallback.class), "");

            assertThat(videos.length(), is(8));
            assertThat(downloadStatus.get(), is(8));
        }
    }

    /*
        downloadNativeVideos() should log any JSON parsing exceptions to Track
     */
    @Test
    public void downloadNativeVideos_trackJSONExceptions() throws JSONException {
        try (TestHarness harness = TestHarness.idleNative()) {

            AtomicInteger downloadStatus = new AtomicInteger();
            NativePrefetchResponse resp = new NativePrefetchResponse(ReferenceResponse.apiVideoPrefetchWithResults);

            JSONObject response = resp.getResponse();
            JSONArray videos = response.getJSONArray("videos");
            videos.put(2, "not a JSONObject");

            Map<String, Asset> assets;
            assets = Asset.deserializeNativeVideos(response);

            harness.downloader.downloadAssets(Priority.LOW, assets, downloadStatus, mock(AssetDownloadCallback.class), "");
            assertThat(videos.length(), is(8));
            // should download the parts that it could
            assertThat(downloadStatus.get(), is(7));
        }

    }
    //</editor-fold>

    //<editor-fold desc="downloadWebViewCacheAssets tests">

    //</editor-fold

    //<editor-fold desc="cancel() tests">
    /*
        cancel() should set the AtomicInteger to CANCELED, and do nothing else, in IDLE.
     */
    @Test
    public void cancel_IDLE_to_IDLE() {
        try (TestHarness harness = TestHarness.idle()) {
            AtomicInteger remainingDownloads = new AtomicInteger(3);

            String before = harness.internalSnapshot();
            harness.downloader.cancel(remainingDownloads);
            assertThat(harness.internalSnapshot(), is(before));

            assertThat(remainingDownloads.get(), is(AssetInfo.CANCELED));
            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        cancel() should cancel the current request if it matches the AtomicInteger,
        and transition to STATE_IDLE if there are no more pending requests.
     */
    @Test
    public void cancel_DOWNLOADING_to_IDLE() {
        AtomicInteger downloadsRemaining = new AtomicInteger(0);
        try (TestHarness harness = TestHarness.downloading(downloadsRemaining)) {

            harness.downloader.cancel(downloadsRemaining);

            assertThat(downloadsRemaining.get(), is(AssetInfo.CANCELED));
            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        cancel() should cancel the current request if it matches the AtomicInteger,
        and continue in DOWNLOADING if there are more pending requests.
     */
    @Test
    public void cancel_DOWNLOADING_to_DOWNLOADING() {
        final AtomicInteger firstBatch = new AtomicInteger(0);
        final AtomicInteger secondBatch = new AtomicInteger(0);

        try (TestHarness harness = TestHarness.downloading(firstBatch)) {
            harness.downloadWebViewCacheAssets(secondBatch, ReferenceResponse.webviewV2PrefetchWithResultsSplit2);
            assertThat(firstBatch.get(), is(6));
            assertThat(secondBatch.get(), is(5));

            harness.downloader.cancel(firstBatch);

            assertThat(firstBatch.get(), is(AssetInfo.CANCELED));
            harness.assertStateIs(STATE_DOWNLOADING);

            assertThat(firstBatch.get(), is(AssetInfo.CANCELED));
            assertThat(secondBatch.get(), is(5));
        }
    }

    /*
        cancel() should set the AtomicInteger to CANCELED, and do nothing else, in PAUSING.
     */
    @Test
    public void cancel_PAUSING_to_PAUSING() {
        try (TestHarness harness = TestHarness.pausing()) {

            AssetRequest request = harness.getOnlySubmittedAssetRequest();
            AtomicInteger downloadStatus = request.assetInfo.downloadStatus;
            assertThat(downloadStatus.get(), is(greaterThan(0)));

            String before = harness.internalSnapshot();
            // oh boy
            String expectedAfter = before.replace("downloadStatus=6", "downloadStatus=-10000");
            harness.downloader.cancel(downloadStatus);
            assertThat(harness.internalSnapshot(), is(equalTo(expectedAfter)));

            assertThat(downloadStatus.get(), is(AssetInfo.CANCELED));
            harness.assertStateIs(STATE_PAUSING);
        }
    }

    //</editor-fold>

    //<editor-fold desc="pause() tests">
    /*
        pause() should always enter the PAUSED state from IDLE
     */
    @Test
    public void pause_IDLE_to_PAUSED() {
        try (TestHarness harness = TestHarness.idle()) {
            harness.downloader.pause();
            harness.assertStateIs(STATE_PAUSED);
        }
    }

    /*
        pause() should enter the PAUSED state from DOWNLOADING,
        if the current request can be canceled.
     */
    @Test
    public void pause_DOWNLOADING_to_PAUSED() {
        try (TestHarness harness = TestHarness.downloading()) {
            harness.downloader.pause();
            harness.assertStateIs(STATE_PAUSED);
        }
    }

    /*
        pause() should always enter the PAUSING state from DOWNLOADING,
        if the current request cannot be canceled.
     */
    @Test
    public void pause_DOWNLOADING_to_PAUSING() {
        try (TestHarness harness = TestHarness.downloading()) {
            harness.markCurrentRequestAsProcessing();
            harness.downloader.pause();
            harness.assertStateIs(STATE_PAUSING);
        }
    }

    /*
        pause() should do nothing when in the PAUSING state.
     */
    @Test
    public void pause_PAUSING_to_PAUSING() {
        try (TestHarness harness = TestHarness.pausing()) {
            String before = harness.internalSnapshot();
            harness.downloader.pause();
            String after = harness.internalSnapshot();

            harness.assertStateIs(STATE_PAUSING);
            assertThat(before, is(equalTo(after)));
        }
    }

    /*
        pause() should do nothing when in the PAUSED state.
     */
    @Test
    public void pause_PAUSED_to_PAUSED() {
        try (TestHarness harness = TestHarness.paused()) {
            String before = harness.internalSnapshot();
            harness.downloader.pause();
            String after = harness.internalSnapshot();

            harness.assertStateIs(STATE_PAUSED);
            assertThat(before, is(equalTo(after)));
        }
    }

    /*
        pause() should cancel the current request if it isn't already being processed
         - request will be marked as cancelled
         - request will be moved back to pendingRequests
         - currentRequest will be set to null
     */
    @Test
    public void pause_cancels_request() {
        try (TestHarness harness = TestHarness.downloading()) {
            AssetRequest request = harness.getOnlySubmittedAssetRequest();

            assertThat(request.status.get(), is(CBNetworkRequest.Status.QUEUED));

            harness.downloader.pause();

            assertThat(request.status.get(), is(CBNetworkRequest.Status.CANCELED));
            assertNull(harness.getCurrentRequest());
            assertThat(harness.getPendingAssetInfo(), hasItem(assetInfoWithUri(request.getUri())));
        }
    }

    /*
        pause() should not cancel the current request if it IS already being processed
         - current request should remain
         - should not be moved to pendingRequests
         - CBNetworkRequest should remain in processing state
     */
    @Test
    public void pause_does_not_cancel_request_if_processing() {
        try (TestHarness harness = TestHarness.downloading()) {
            AssetRequest request = harness.getOnlySubmittedAssetRequest();
            harness.markCurrentRequestAsProcessing();

            harness.downloader.pause();

            assertThat(request.status.get(), is(CBNetworkRequest.Status.PROCESSING));
            assertThat(harness.getCurrentRequest(), is(sameInstance(request)));
            assertThat(harness.getPendingAssetInfo(), not(hasItem(assetInfoWithUri(request.getUri()))));
        }
    }
    //</editor-fold>

    //<editor-fold desc="resume() tests">
    @Test
    public void resume_when_IDLE_does_nothing() {
        try (TestHarness harness = TestHarness.idle()) {
            String before = harness.internalSnapshot();
            harness.downloader.resume();
            String after = harness.internalSnapshot();

            assertThat(before, is(after));
            harness.assertStateIs(STATE_IDLE);
        }
    }

    @Test
    public void resume_when_DOWNLOADING_does_nothing() {
        try (TestHarness harness = TestHarness.downloading()) {
            String before = harness.internalSnapshot();
            harness.downloader.resume();
            String after = harness.internalSnapshot();

            assertThat(before, is(after));
            harness.assertStateIs(STATE_DOWNLOADING);
        }
    }

    /*
        resume() should transition from PAUSED -> IDLE if there are no pending requests
     */
    @Test
    public void resume_PAUSED_to_IDLE() {
        try (TestHarness harness = TestHarness.paused()) {
            harness.downloader.resume();
            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        resume() should transition from PAUSED -> DOWNLOADING if there are pending requests
     */
    @Test
    public void resume_PAUSED_to_DOWNLOADING() {
        try (TestHarness harness = TestHarness.paused()) {
            harness.downloadNativeVideos();
            harness.downloader.resume();
            harness.assertStateIs(STATE_DOWNLOADING);
        }
    }

    /*
        resume() should transition from PAUSING -> DOWNLOADING
     */
    @Test
    public void resume_PAUSING_to_DOWNLOADING() {
        try (TestHarness harness = TestHarness.pausing()) {
            harness.downloader.resume();
            harness.assertStateIs(STATE_DOWNLOADING);
        }
    }

    //</editor-fold>

    //<editor-fold desc="onAssetDownloadError tests">
    /*
        onAssetDownloadError should do nothing in IDLE
     */
    @Test
    public void onAssetDownloadError_IDLE_to_IDLE() {
        try (TestHarness harness = TestHarness.idle()) {
            harness.resetMocks();
            harness.recordState();

            harness.onAssetDownloadFailure();
            harness.assertStateUnchanged();
        }
    }

    /*
        onAssetDownloadError should download the next pending request if there is one.
     */
    @Test
    public void onAssetDownloadError_DOWNLOADING_to_DOWNLOADING() {
        try (TestHarness harness = TestHarness.downloading()) {
            AssetRequest firstRequest = harness.getOnlySubmittedAssetRequest();
            int entryDownloadStatus = firstRequest.assetInfo.downloadStatus.get();

            harness.onAssetDownloadError(firstRequest);

            assertThat(firstRequest.assetInfo.downloadStatus.get(), is(equalTo(entryDownloadStatus - 1)));

            AssetRequest nextRequest = harness.getLatestSubmittedAssetRequest();
            assertThat(nextRequest, not(is(sameInstance(firstRequest))));
            verify(harness.networkRequestService).submit(nextRequest);
        }
    }

    /*
        onAssetDownloadError should report the failure time to track.
     */
    @Test
    public void onAssetDownloadError_reportToTrack() {
        try (TestHarness harness = TestHarness.downloading()) {
            final String firstErrorMessage = "the first error message";

            final String secondErrorMessage = "the second error message";

            AssetRequest firstRequest = harness.getOnlySubmittedAssetRequest();

            RequestTimes firstRequestTimes = RequestTimes.random();

            harness.onAssetDownloadErrorAfter(firstRequest, new CBError(INVALID_RESPONSE, firstErrorMessage), mock(CBNetworkServerResponse.class), firstRequestTimes);

            AssetRequest secondRequest = harness.getLatestSubmittedAssetRequest();
            assertThat(secondRequest, not(sameInstance(firstRequest)));

            RequestTimes secondRequestTimes = RequestTimes.random();
            harness.onAssetDownloadErrorAfter(secondRequest, new CBError(INVALID_RESPONSE, secondErrorMessage), mock(CBNetworkServerResponse.class), secondRequestTimes);
        }
    }

    /*
        onAssetDownloadError should download the next pending request if there is one.
     */
    @Test
    public void onAssetDownloadError_DOWNLOADING_to_IDLE() {
        try (TestHarness harness = TestHarness.downloading()) {
            harness.processUntilDownloadingLastRequest();

            AssetRequest lastRequest = harness.getLatestSubmittedAssetRequest();
            assertThat(lastRequest.assetInfo.downloadStatus.get(), is(equalTo(1)));

            harness.onAssetDownloadError(lastRequest);

            assertThat(lastRequest.assetInfo.downloadStatus.get(), is(equalTo(0)));
            harness.assertStateIs(STATE_IDLE);
        }
    }

    /*
        onAssetDownloadError should transition to PAUSED from PAUSING once
        the current request has been processed.
     */
    @Test
    public void onAssetDownloadError_PAUSING_to_PAUSED() {
        try (TestHarness harness = TestHarness.pausing()) {
            AssetRequest request = harness.getLatestSubmittedAssetRequest();

            int entryDownloadStatus = request.assetInfo.downloadStatus.get();
            harness.onAssetDownloadError(request);
            assertThat(request.assetInfo.downloadStatus.get(), is(equalTo(entryDownloadStatus - 1)));

            harness.assertStateIs(STATE_PAUSED);
        }
    }

    /*
        onAssetDownloadError should stay in PAUSING if the request
        was not the current request
     */
    @Test
    public void onAssetDownloadError_PAUSING_to_PAUSING() {
        try (TestHarness harness = TestHarness.pausing()) {
            AssetRequest request = harness.getLatestSubmittedAssetRequest();

            harness.recordState();

            int entryDownloadStatus = request.assetInfo.downloadStatus.get();

            harness.onAssetDownloadFailure();
            assertThat(request.assetInfo.downloadStatus.get(), is(equalTo(entryDownloadStatus)));

            harness.assertStateUnchanged();

            harness.assertStateIs(STATE_PAUSING);
        }
    }

    /*
        onAssetDownloadError should stay in PAUSED
     */
    @Test
    public void onAssetDownloadError_PAUSED_to_PAUSED() {
        try (TestHarness harness = TestHarness.pausedWithPending()) {
            AssetRequest latestRequest = harness.getLatestSubmittedAssetRequest();

            harness.resetMocks();
            harness.recordState();

            latestRequest.deliverError(new CBError(CBError.Internal.INTERNET_UNAVAILABLE, "simulated"), mock(CBNetworkServerResponse.class));

            harness.assertStateUnchanged();
            harness.verifyZeroInteractionsOnMocks();

            harness.assertStateIs(STATE_PAUSED);
        }
    }
    //</editor-fold>

    //<editor-fold desc="onAssetDownloadSuccess tests"

    @Test
    public void onAssetDownloadSuccess_decrementsDownloadStatus() {
        AtomicInteger downloadStatus = new AtomicInteger();
        try (TestHarness harness = TestHarness.downloading(downloadStatus)) {
            assertThat(downloadStatus.get(), is(6));

            AssetRequest assetRequest = harness.getOnlySubmittedAssetRequest();
            harness.onAssetDownloadSuccess(assetRequest);

            assertThat(downloadStatus.get(), is(5));
        }
    }

    /*
        Verify that the downloader posts the AssetDownloadCallback for
        delivery only after the last asset is downloaded.
     */
    @Test
    public void onAssetDownloadSuccess_notifiesAssetManager_lastFileOnly() {
        AtomicInteger downloadStatus = new AtomicInteger();
        try (TestHarness harness = TestHarness.downloadingNative(downloadStatus)) {
            assertThat(downloadStatus.get(), is(8));

            for (int n = 1; n <= 8; n++) {
                AssetRequest assetRequest = harness.getSubmittedAssetRequest(n);
                harness.onAssetDownloadSuccess(assetRequest);
                harness.verifyNoAssetDownloadResult();
                assertThat(harness.tc.backgroundRunnableCount(), is(n < 8 ? 0 : 1));
            }
            harness.tc.runNextBackgroundRunnable();

            harness.verifyOnlyAssetDownloadResult(true);
        }
    }


    @Test
    public void onAssetDownloadSuccess_submitsNextRequest() {
        AtomicInteger downloadStatus = new AtomicInteger();
        try (TestHarness harness = TestHarness.downloading(downloadStatus)) {
            assertThat(downloadStatus.get(), is(6));

            AssetRequest assetRequest = harness.getOnlySubmittedAssetRequest();
            harness.onAssetDownloadSuccess(assetRequest);

            AssetRequest secondRequest = harness.getSubmittedAssetRequest(2);
            assertThat(secondRequest, not(sameInstance(assetRequest)));
        }
    }

    /*
        Compare with onAssetDownloadSuccess_native_notifiesAssetManager_lastFileOnly.

        This test verifies that Downloader.onAssetDownloadSuccess ignores callbacks
        if not for the current request.  It verifies this behavior by using an identical
        AssetRequest that is just a different instance.
     */
    @Test
    public void onAssetDownloadSuccess_doesNothingIfNotCurrentRequest() {
        AtomicInteger downloadStatus = new AtomicInteger();
        try (TestHarness harness = TestHarness.downloadingNative(downloadStatus)) {
            assertThat(downloadStatus.get(), is(8));

            for (int n = 1; n <= 8; n++) {
                AssetRequest assetRequest = harness.getSubmittedAssetRequest(n);
                if (n == 8) {
                    assetRequest = new AssetRequest(harness.downloader, harness.tc.reachability, assetRequest.assetInfo, assetRequest.outputFile, harness.tc.appId);
                }
                harness.onAssetDownloadSuccess(assetRequest);
                harness.verifyNoAssetDownloadResult();
            }
            assertThat(harness.tc.backgroundRunnableCount(), is(0));

            harness.verifyNoAssetDownloadResult();
        }
    }

    @Test
    public void onAssetDownloadSuccess_IDLE_to_IDLE() {
        try (TestHarness harness = TestHarness.idle()) {
            harness.resetMocks();
            harness.recordState();
            harness.downloader.onAssetDownloadResult(mock(AssetRequest.class), null, null);
            harness.assertStateUnchanged();
            verifyNoInteractions(harness.networkRequestService);
        }
    }

    //</editor-fold>

    //<editor-fold desc="Edge cases"

    /*
        PAUSED is a little tricky:
         - it's possible that some requests were sent, or not.
         - if pending() is empty, we can't tell.  This was a problem
           when there was a downloaderFinished() method to call, but there isn't anymore.

        The scenario is:
         - we have submitted the last request
         - a network dispatcher has started processing it
         - call pause()
            - in this case pending() will be empty
            - this test verifies that the Downloader enters PAUSING and stays
              until the request is processed
         - wait for the request to complete (success or error)
         - later call resume()
            - this will transition to IDLE

     */
    @Test
    public void signal_downloadComplete_on_DOWNLOADING_to_PAUSING_to_PAUSED_to_IDLE() {
        for (Boolean success : TestUtils.eitherBoolean) {
            try (TestHarness harness = TestHarness.downloading()) {
                harness.processUntilDownloadingLastRequest();

                AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
                harness.markCurrentRequestAsProcessing();

                harness.downloader.pause();
                harness.assertStateIs(STATE_PAUSING);

                if (success) {
                    harness.onAssetDownloadSuccess(assetRequest);
                } else {
                    harness.onAssetDownloadError(assetRequest);
                }
                harness.assertStateIs(STATE_PAUSED);

                harness.tc.runNextBackgroundRunnable();
                harness.verifyOnlyAssetDownloadResult(success);

                harness.downloader.resume();

                harness.assertStateIs(STATE_IDLE);
            }
        }
    }

    /*
        cancel() should not call either callback even if cancelling the last request.
     */
    @Test
    public void cancel_signals_downloadComplete_on_cancel_last_request() {
        try (TestHarness harness = TestHarness.downloading()) {
            harness.processUntilDownloadingLastRequest();

            AssetRequest assetRequest = harness.getLatestSubmittedAssetRequest();
            harness.downloader.cancel(assetRequest.assetInfo.downloadStatus);
            harness.verifyNoAssetDownloadResult();
        }
    }
    //</editor-fold>

    //<editor-fold desc="submitHighestPriorityRequest() tests">
    /*
        submitHighestPriorityRequest() should stay in PAUSING even if new downloads are available.
     */
    @Test
    public void submitHighestPriorityRequest_stay_in_pausing_until_current_complete() {
        try (TestHarness harness = TestHarness.pausing()) {
            harness.downloadWebViewCacheAssets(
                    new AtomicInteger(),
                    ReferenceResponse.webviewV2PrefetchWithResultsSplit2,
                    Priority.HIGH);
            harness.assertStateIs(STATE_PAUSING);
        }
    }

    /*
        submitHighestPriorityRequest() should not download files that already exist in the file cache.
     */
    @Test
    public void submitHighestPriorityRequest_do_not_download_files_already_in_cache() {
        AssetDescriptor assetInCache = AssetDescriptor.videoAd56ff66;
        AssetDescriptor assetsNotInCache[] = new AssetDescriptor[]{
                AssetDescriptor.baseTemplate33cda9,
                AssetDescriptor.videoAdMagicWars2,
                AssetDescriptor.videoAdMagicWars3,
                AssetDescriptor.videoAd571459,
                AssetDescriptor.videoAd55fb49
        };

        try (TestHarness harness = TestHarness.idle()) {
            assetInCache.writeResourceToCache(harness.tc.internalBaseDir);
            AtomicInteger downloadStatus = new AtomicInteger();
            harness.downloadWebViewCacheAssets(downloadStatus);

            harness.processUntilDownloadComplete();

            // should indicate that the downloads are all done
            assertThat(downloadStatus.get(), is(0));

            // should not have downloaded the file already in cache
            verify(harness.networkRequestService, never()).submit(argThat(matchesAssetDescriptor(assetInCache)));

            // should have downloaded all of the others
            for (AssetDescriptor notInCache : assetsNotInCache) {
                verify(harness.networkRequestService).submit(argThat(matchesAssetDescriptor(notInCache)));
            }
        }
    }

    /*
        submitHighestPriorityRequest() should skip files if it can't create the directory
     */
    @Test
    public void submitHighestPriorityRequest_handle_cannot_create_directory() {
        AssetDescriptor assetInCache = AssetDescriptor.videoAd56ff66;
        AssetDescriptor assetCannotCreateDir = AssetDescriptor.baseTemplate33cda9;
        AssetDescriptor assetsNotInCache[] = new AssetDescriptor[]{
                AssetDescriptor.videoAdMagicWars2,
                AssetDescriptor.videoAdMagicWars3,
                AssetDescriptor.videoAd571459,
                AssetDescriptor.videoAd55fb49
        };

        try (TestHarness harness = TestHarness.idle()) {
            File htmlDir = new File(harness.tc.internalBaseDir, "html");

            // Make it so the Downloader can't create the directory for one file
            assertTrue(htmlDir.delete());
            assertThat(harness.tc.internalBaseDir.setWritable(false), CoreMatchers.is(true));

            try {
                assetInCache.writeResourceToCache(harness.tc.internalBaseDir);
                AtomicInteger downloadStatus = new AtomicInteger();
                harness.downloadWebViewCacheAssets(downloadStatus);

                harness.processUntilDownloadComplete();

                // should indicate that the downloads are all done
                assertThat(downloadStatus.get(), is(0));
            } finally {
                assertThat(harness.tc.internalBaseDir.setWritable(true), CoreMatchers.is(true));
            }

            // should not have downloaded the file already in cache
            verify(harness.networkRequestService, never()).submit(argThat(matchesAssetDescriptor(assetInCache)));

            // should have downloaded all of the others
            for (AssetDescriptor notInCache : assetsNotInCache) {
                verify(harness.networkRequestService).submit(argThat(matchesAssetDescriptor(notInCache)));
            }
            // should not have tried to download the file in the html directory, which it couldn't create
            verify(harness.networkRequestService, never()).submit(argThat(matchesAssetDescriptor(assetCannotCreateDir)));
        }
    }

    /*
        submitHighestPriorityRequest() should touch requested files that are already in the cache.
     */
    @Test
    public void submitHighestPriorityRequest_touch_files_already_in_cache() {
        AssetDescriptor assetInCache = AssetDescriptor.videoAd56ff66;

        try (TestHarness harness = TestHarness.idle()) {
            assetInCache.writeResourceToCache(harness.tc.internalBaseDir);

            File inCacheFile = assetInCache.getCacheFile(harness.tc.internalBaseDir);
            long twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2);
            assertTrue(inCacheFile.setLastModified(twoDaysAgo));

            harness.downloadWebViewCacheAssets();

            harness.processUntilDownloadComplete();

            long reasonablyRecent = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15);
            assertThat(inCacheFile.lastModified(), is(greaterThan(reasonablyRecent)));
        }
    }

    /*
        submitHighestPriorityRequest() should cancel the current request if it can, if there is
        a higher-priority asset request available.
     */
    @Test
    public void submitHighestPriorityRequest_cancels_lower_priority_request() {
        try (TestHarness harness = TestHarness.downloading()) {
            AssetRequest lowPriorityRequest = harness.getOnlySubmittedAssetRequest();

            AtomicInteger downloadStatus = new AtomicInteger();
            harness.downloadWebViewCacheAssets(downloadStatus, ReferenceResponse.webviewV2PrefetchWithResultsSplit2, Priority.HIGH);

            assertThat(lowPriorityRequest.status.get(), is(CBNetworkRequest.Status.CANCELED));
            AssetRequest currentRequest = harness.getCurrentRequest();
            assertThat(currentRequest.assetInfo.priority, is(Priority.HIGH));
            assertThat(currentRequest.assetInfo.downloadStatus, is(sameInstance(downloadStatus)));

            assertThat(ReferenceResponse.webviewV2PrefetchWithResultsSplit2AssetDescriptors,
                    hasItemInArray(hasUri(currentRequest.assetInfo.uri)));
        }
    }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="reduceCacheSize tests">

    /*
        Verify that reduceCacheSize deletes the oldest files until
        the total file size is within cacheMaxBytes
     */
    @Test
    public void testReduceCacheSize_DeletesOldestVideosOverLimit() throws Exception {
        try (TestHarness h = TestHarness.idle()) {
            TestContainer tc = h.tc;

            tc.control.configure().withWebViewCacheTTLDays(7);
            tc.installConfig();

            File externalCache = tc.internalBaseDir;

            File smallest = AssetDescriptor.videoAdMagicWars2.writeResourceToCache(externalCache);
            File secondSmallest = AssetDescriptor.videoAdMagicWars3.writeResourceToCache(externalCache);
            File secondLargest = AssetDescriptor.videoAd56ff66.writeResourceToCache(externalCache);
            File largest = AssetDescriptor.videoAdMagicWars1.writeResourceToCache(externalCache);

            // Have smallest -> largest order not match oldest -> newest order
            File oldest = secondLargest;
            File secondOldest = smallest;
            File secondNewest = secondSmallest;
            File newest = largest;

            Long now = tc.timeSource.currentTimeMillis();
            Long day = TimeUnit.DAYS.toMillis(1);

            assertTrue(oldest.setLastModified(now - day * 5));
            assertTrue(secondOldest.setLastModified(now - day * 4));
            assertTrue(secondNewest.setLastModified(now - day * 3));
            assertTrue(newest.setLastModified(now - day * 2));

            assertThat(smallest.length(), is(1409745L));
            assertThat(secondSmallest.length(), is(1506640L));
            assertThat(secondLargest.length(), is(1553941L));
            assertThat(largest.length(), is(2735912L));

            tc.control.configure().withWebViewCacheMaxBytes(
                    oldest.length() + secondOldest.length() + secondNewest.length() + newest.length());
            tc.installConfig();

            h.downloader.reduceCacheSize();
            assertTrue(oldest.exists());
            assertTrue(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            tc.control.configure().withWebViewCacheMaxBytes(
                    secondOldest.length() + secondNewest.length() + newest.length());
            tc.installConfig();
            tc.downloader.state = STATE_IDLE;
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertTrue(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            tc.control.configure().withWebViewCacheMaxBytes(secondNewest.length() + newest.length());
            tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            tc.control.configure().withWebViewCacheMaxBytes(newest.length());
            tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertFalse(secondNewest.exists());
            assertTrue(newest.exists());

            tc.control.configure().withWebViewCacheMaxBytes(newest.length() - 1);
            tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertFalse(secondNewest.exists());
            assertFalse(newest.exists());
        }
    }

    /*
        reduceCacheSize() should delete expired files, but only if the Downloader is idle.
     */
    @Test
    public void reduceCacheSize_in_IDLE() {
        int[] states = new int[]{
                STATE_IDLE,
                STATE_DOWNLOADING,
                STATE_PAUSED,
                STATE_PAUSING
        };
        for (int state : states) {
            try (TestHarness h = TestHarness.forState(state)) {

                h.tc.control.configure()
                        .withWebViewCacheMaxBytes(10 * 1024 * 1024)
                        .withWebViewCacheTTLDays(7);
                h.tc.installConfig();

                File expiredFile = AssetDescriptor.videoAd56ff66.writeResourceToCache(h.tc.internalBaseDir);
                Long now = h.tc.timeSource.currentTimeMillis();
                Long day = TimeUnit.DAYS.toMillis(1);

                assertTrue(expiredFile.setLastModified(now - day * 8));

                h.downloader.reduceCacheSize();

                boolean shouldDelete = state == STATE_IDLE;
                String message = "should" + (shouldDelete ? "" : " not") + " delete expired files in Downloader state " + state;

                assertThat(message, !expiredFile.exists(), is(shouldDelete));
            }
        }
    }

    /*
        Verify that reduceCacheSize deletes expired assets
     */
    @Test
    public void testReduceCacheSize_DeletesExpiredAssets() throws Exception {
        try (TestHarness h = TestHarness.idle()) {

            h.tc.control.configure()
                    .withWebViewCacheMaxBytes(10 * 1024 * 1024)
                    .withWebViewCacheTTLDays(7);
            h.tc.installConfig();

            File externalCache = h.tc.internalBaseDir;

            File oldest = AssetDescriptor.videoAd56ff66.writeResourceToCache(externalCache);
            File secondOldest = AssetDescriptor.baseTemplate2e34e6.writeResourceToCache(externalCache);
            File secondNewest = AssetDescriptor.closeButton60x60.writeResourceToCache(externalCache);
            File newest = AssetDescriptor.videoAdMagicWars1.writeResourceToCache(externalCache);

            Long now = h.tc.timeSource.currentTimeMillis();
            Long day = TimeUnit.DAYS.toMillis(1);

            assertTrue(oldest.setLastModified(now - day * 5 - 60));
            assertTrue(secondOldest.setLastModified(now - day * 4 - 60));
            assertTrue(secondNewest.setLastModified(now - day * 3 - 60));
            assertTrue(newest.setLastModified(now - day * 2 - 60));

            h.downloader.reduceCacheSize();
            assertTrue(oldest.exists());
            assertTrue(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            h.tc.control.configure().withWebViewCacheTTLDays(5);
            h.tc.installConfig();

            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertTrue(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            h.tc.control.configure().withWebViewCacheTTLDays(4);
            h.tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertTrue(secondNewest.exists());
            assertTrue(newest.exists());

            h.tc.control.configure().withWebViewCacheTTLDays(3);
            h.tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertFalse(secondNewest.exists());
            assertTrue(newest.exists());

            h.tc.control.configure().withWebViewCacheTTLDays(2);
            h.tc.installConfig();
            h.downloader.reduceCacheSize();
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertFalse(secondNewest.exists());
            assertFalse(newest.exists());
        }
    }

    /*
        reduceCacheSize should not delete any files in these subdirectories:
           track
           session
           request manager
           video completion
     */
    @Test
    public void reduceCacheSize_leavesNonAssetsAlone() {
        try (TestHarness h = TestHarness.idle()) {
            String[] directories = {
                    "session",
                    "requests",
                    "track",
                    "videoCompletionEvents",
                    "has.dot"
            };
            Long now = System.currentTimeMillis();
            Long day = TimeUnit.DAYS.toMillis(1);

            for (String dirname : directories) {
                File file = new File(h.tc.internalBaseDir, dirname + "/filename");
                TestUtils.writeStringToFile(file, "contents of " + dirname);
                assertTrue(file.setLastModified(now - day * 1200));
            }

            h.downloader.reduceCacheSize();
            for (String dirname : directories) {
                File file = new File(h.tc.internalBaseDir, dirname + "/filename");
                assertTrue(file.exists());
                TestUtils.assertFileContentsMatchString(file, "contents of " + dirname);
                TestUtils.writeStringToFile(file, "contents");
            }
        }
    }

    /*
        Verify that reduceCacheSize deletes the oldest files until
        the total file size is within cacheMaxBytes
     */
    @Test
    public void testReduceCacheSize_skipsUndeletableFiles() throws Exception {
        try (TestHarness h = TestHarness.idle()) {

            h.tc.control.configure().withWebViewCacheTTLDays(7);
            h.tc.installConfig();

            File externalCache = h.tc.internalBaseDir;

            File htmlFile = AssetDescriptor.baseTemplate2e34e6.writeResourceToCache(externalCache);
            File smallest = AssetDescriptor.videoAdMagicWars2.writeResourceToCache(externalCache);
            File secondSmallest = AssetDescriptor.videoAdMagicWars3.writeResourceToCache(externalCache);
            File secondLargest = AssetDescriptor.videoAd56ff66.writeResourceToCache(externalCache);
            File largest = AssetDescriptor.videoAdMagicWars1.writeResourceToCache(externalCache);

            // Have smallest -> largest order not match oldest -> newest order
            File oldest = secondLargest;
            File secondOldest = smallest;
            File secondNewest = secondSmallest;
            File newest = largest;

            Long now = System.currentTimeMillis();
            Long day = TimeUnit.DAYS.toMillis(1);

            assertTrue(htmlFile.setLastModified(now - day * 8));

            assertTrue(oldest.setLastModified(now - day * 5));
            assertTrue(secondOldest.setLastModified(now - day * 4));
            assertTrue(secondNewest.setLastModified(now - day * 3));
            assertTrue(newest.setLastModified(now - day * 2));

            assertThat(smallest.length(), is(1409745L));
            assertThat(secondSmallest.length(), is(1506640L));
            assertThat(secondLargest.length(), is(1553941L));
            assertThat(largest.length(), is(2735912L));

            h.tc.control.configure().withWebViewCacheMaxBytes(newest.length());
            h.tc.installConfig();

            File htmlDir = new File(externalCache, "html");
            try {
                assertTrue(htmlDir.setWritable(false));
                h.downloader.reduceCacheSize();
            } finally {
                assertTrue(htmlDir.setWritable(true));
            }

            assertTrue(htmlFile.exists()); // didn't delete the 8 day old file
            assertFalse(oldest.exists());
            assertFalse(secondOldest.exists());
            assertFalse(secondNewest.exists());
            assertTrue(newest.exists());
        }
    }

    static class TestHarness implements AutoCloseable {
        final TestContainer tc;
        final CBNetworkService networkRequestService;
        final Downloader downloader;
        final AssetDownloadCallback assetDownloadCallback;

        InternalState recordedState;

        TestHarness(TestContainer tc) {
            this.tc = tc;
            networkRequestService = tc.networkService;
            downloader = tc.downloader;
            assetDownloadCallback = mock(AssetDownloadCallback.class);
        }

        static TestHarness idle() {
            return idle(TestContainerControl.defaultWebView());
        }

        static TestHarness idle(TestContainerControl control) {
            return new TestHarness(new TestContainerBuilder(control).withDownloader().build());
        }

        static TestHarness idleNative() {
            return idle(TestContainerControl.defaultNative());
        }

        static TestHarness downloadingNative(AtomicInteger downloadStatus) {
            TestHarness harness = TestHarness.idle(TestContainerControl.defaultNative());

            harness.downloadNativeVideos(downloadStatus);

            return harness;
        }

        static TestHarness downloading() {
            return downloading(new AtomicInteger());
        }

        static TestHarness downloading(AtomicInteger downloadStatus) {
            TestHarness harness = TestHarness.idle();

            harness.downloadWebViewCacheAssets(downloadStatus);

            return harness;
        }

        private void resetMocks() {
            reset(networkRequestService);
        }

        private void downloadNativeVideos() {
            downloadNativeVideos(new AtomicInteger());
        }

        private void downloadNativeVideos(AtomicInteger downloadStatus) {
            downloadNativeVideos(ReferenceResponse.apiVideoPrefetchWithResults, downloadStatus);

        }

        private void downloadNativeVideos(ResponseDescriptor response, AtomicInteger downloadStatus) {
            NativePrefetchResponse resp = new NativePrefetchResponse(response);

            JSONArray videos = resp.getVideos();
            downloader.downloadAssets(Priority.LOW, resp.getAssets(), downloadStatus, assetDownloadCallback, "");
        }

        private void downloadWebViewCacheAssets() {
            downloadWebViewCacheAssets(new AtomicInteger());
        }

        private void downloadWebViewCacheAssets(AtomicInteger downloadStatus) {
            downloadWebViewCacheAssets(downloadStatus, ReferenceResponse.webviewV2PrefetchWithResultsSplit1);

        }

        private void downloadWebViewCacheAssets(AtomicInteger downloadStatus, ResponseDescriptor response) {
            downloadWebViewCacheAssets(downloadStatus, response, Priority.LOW);
        }

        private void downloadWebViewCacheAssets(AtomicInteger downloadStatus, ResponseDescriptor response, Priority priority) {
            WebViewPrefetchResponse resp = new WebViewPrefetchResponse(response);

            Map<String, Asset> assets = Asset.v2PrefetchToAssets(resp.response, 10);

            downloader.downloadAssets(priority, assets, downloadStatus, assetDownloadCallback, "");
        }


        static TestHarness paused() {
            TestHarness harness = idle();

            harness.downloader.pause();

            harness.assertStateIs(STATE_PAUSED);

            return harness;
        }

        static TestHarness pausedWithPending() {
            TestHarness harness = downloading();

            harness.downloader.pause();

            harness.assertStateIs(STATE_PAUSED);

            return harness;
        }

        static TestHarness pausing() {
            TestHarness harness = downloading();

            harness.markCurrentRequestAsProcessing();
            harness.downloader.pause();

            harness.assertStateIs(STATE_PAUSING);

            return harness;
        }

        private void markCurrentRequestAsProcessing() {
            assertThat(
                    getCurrentRequest().status.compareAndSet(
                            CBNetworkRequest.Status.QUEUED,
                            CBNetworkRequest.Status.PROCESSING),
                    is(true));
        }

        @Override
        public void close() {
            tc.close();
        }

        public void assertStateIs(int expectedState) {
            assertThat(getState(), is(equalTo(expectedState)));

            assertInvariants();
        }


        public void assertCurrentRequestIs(AssetRequest expected) {
            assertThat(getCurrentRequest(), is(sameInstance(expected)));

            assertInvariants();
        }

        public void assertPendingDownloadCount(int expected) {
            int actual = getPending().size();
            assertThat(actual, is(equalTo(expected)));

            assertInvariants();
        }


        public void assertInvariants() {
            int state = getState();
            AssetRequest currentRequest = getCurrentRequest();
            PriorityQueue<AssetInfo> pending = getPending();

            assertThat(state, isOneOf(
                    STATE_IDLE,
                    STATE_DOWNLOADING,
                    STATE_PAUSING,
                    STATE_PAUSED));

            // If we are downloading an AssetRequest, the associated AssetInfo
            // should not also be in pending.
            if (currentRequest != null)
                assertThat(pending, not(hasItem(sameInstance(currentRequest.assetInfo))));

            switch (state) {
                case STATE_IDLE:
                    assertNull(currentRequest);
                    assertTrue(pending.isEmpty());
                    break;

                case STATE_DOWNLOADING:
                    assertNotNull(currentRequest);
                    break;

                case STATE_PAUSING:
                    assertNotNull(currentRequest);
                    break;

                case STATE_PAUSED:
                    assertNull(currentRequest);
                    break;
            }
        }

        private Integer getState() {
            return (Integer) TestUtils.getFieldWithReflection(downloader, Downloader.class, "state");
        }

        private AssetRequest getOnlySubmittedAssetRequest() {
            ArgumentCaptor<AssetRequest> requestCaptor = ArgumentCaptor.forClass(AssetRequest.class);
            verify(networkRequestService).submit(requestCaptor.capture());
            return requestCaptor.getValue();
        }

        public AssetRequest getSubmittedAssetRequest(int nth) {
            ArgumentCaptor<AssetRequest> requestCaptor = ArgumentCaptor.forClass(AssetRequest.class);
            verify(networkRequestService, atLeast(nth)).submit(requestCaptor.capture());
            List<AssetRequest> requests = requestCaptor.getAllValues();
            return requests.get(nth - 1);
        }

        public AssetRequest getLatestSubmittedAssetRequest() {
            ArgumentCaptor<AssetRequest> requestCaptor = ArgumentCaptor.forClass(AssetRequest.class);
            verify(networkRequestService, atLeastOnce()).submit(requestCaptor.capture());
            List<AssetRequest> requests = requestCaptor.getAllValues();
            return requests.get(requests.size() - 1);
        }


        private AssetRequest getCurrentRequest() {
            return (AssetRequest) TestUtils.getFieldWithReflection(downloader, Downloader.class, "currentRequest");
        }

        private PriorityQueue<AssetInfo> getPending() {
            //noinspection unchecked
            return (PriorityQueue<AssetInfo>) TestUtils.getFieldWithReflection(downloader, Downloader.class, "pending");
        }

        private List<AssetInfo> getPendingAssetInfo() {
            List<AssetInfo> result = new ArrayList<>();
            for (AssetInfo assetInfo : getPending()) {
                result.add(assetInfo);
            }
            return result;
        }

        private String internalSnapshot() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.US, "State: %d\n", getState()));
            AssetRequest currentRequest = getCurrentRequest();
            sb.append("Current Request: ");
            if (currentRequest == null) {
                sb.append("None\n");
            } else {
                sb.append("  uri=");
                sb.append(currentRequest.getUri());
                sb.append("\n");
            }
            sb.append("Pending:\n");
            for (AssetInfo assetInfo : getPendingAssetInfo()) {
                sb.append("  Asset Info:\n");
                sb.append("    priority=");
                sb.append(assetInfo.priority);
                sb.append("\n");
                sb.append("    filename=");
                sb.append(assetInfo.filename);
                sb.append("\n");
                sb.append("    uri=");
                sb.append(assetInfo.uri);
                sb.append("\n");
                sb.append("    type=");
                sb.append(assetInfo.type);
                sb.append("\n");
                sb.append("    downloadStatus=");
                sb.append(assetInfo.downloadStatus.get());
                sb.append("\n");
            }
            sb.append("\n");
            return sb.toString();
        }

        public void recordState() {
            recordedState = new InternalState();
        }

        public void assertStateUnchanged() {
            assertNotNull(recordedState);
            recordedState.assertSame(new InternalState());
        }

        public void processUntilDownloadingLastRequest() {
            assertStateIs(STATE_DOWNLOADING);
            while (!getPending().isEmpty()) {
                onAssetDownloadSuccess(getCurrentRequest());
            }
        }

        public void processUntilDownloadComplete() {
            assertStateIs(STATE_DOWNLOADING);
            while (getState() != STATE_IDLE) {
                onAssetDownloadSuccess(getCurrentRequest());
            }
        }

        public void verifyZeroInteractionsOnMocks() {
            verifyNoInteractions(networkRequestService);
        }

        public void onAssetDownloadError(AssetRequest request) {
            request.deliverError(new CBError(CBError.Internal.INTERNET_UNAVAILABLE, "simulated"), mock(CBNetworkServerResponse.class));
        }

        public void onAssetDownloadErrorAfter(AssetRequest request, CBError cbError, CBNetworkServerResponse errorResponse, RequestTimes requestTimes) {
            request.processingNs = requestTimes.processingNs;
            request.getResponseCodeNs = requestTimes.getResponseCodeNs;
            request.readDataNs = requestTimes.readDataNs;
            tc.advanceUptime(requestTimes.processingNs, TimeUnit.NANOSECONDS);
            tc.advanceUptime(new Random().nextInt(300), TimeUnit.MILLISECONDS);

            request.deliverError(cbError, errorResponse);
        }

        public void onAssetDownloadSuccess(AssetRequest request) {
            request.deliverResponse(null, null); // void
        }

        public void onAssetDownloadSuccess(AssetRequest request, RequestTimes requestTimes) {
            request.processingNs = requestTimes.processingNs;
            request.getResponseCodeNs = requestTimes.getResponseCodeNs;
            request.readDataNs = requestTimes.readDataNs;
            tc.advanceUptime(requestTimes.processingNs, TimeUnit.NANOSECONDS);
            request.deliverResponse(null, null); // void, no error
        }

        public void onAssetDownloadSuccessAfter(AssetRequest request, long n, TimeUnit unit) {
            request.processingNs = unit.toNanos(n);
            request.deliverResponse(null, null); // void, no error
        }

        public static TestHarness forState(int state) {
            switch (state) {
                case STATE_IDLE:
                    return idle();

                case STATE_DOWNLOADING:
                    return downloading();

                case STATE_PAUSED:
                    return paused();

                case STATE_PAUSING:
                    return pausing();

                default:
                    throw new Error("unknown state");
            }
        }

        private void onAssetDownloadFailure() {
            downloader.onAssetDownloadResult(mock(AssetRequest.class), new CBError(CBError.Internal.INTERNET_UNAVAILABLE, "simulated"), mock(CBNetworkServerResponse.class));
        }

        private void verifyNoAssetDownloadResult() {
            verify(assetDownloadCallback, never()).assetDownloadResult(anyBoolean());
        }

        private void verifyOnlyAssetDownloadResult(Boolean success) {
            verify(assetDownloadCallback, only()).assetDownloadResult(eq(success));
        }

        private class InternalState {
            final String internalSnapshot;

            InternalState() {
                internalSnapshot = internalSnapshot();
            }

            public void assertSame(InternalState internalState) {
                assertThat(internalSnapshot, is(equalTo(internalState.internalSnapshot)));
            }
        }
    }
    //</editor-fold>

    private static class NativePrefetchResponse {
        ResponseDescriptor reference;

        NativePrefetchResponse(ResponseDescriptor reference) {
            this.reference = reference;
        }


        public JSONObject getResponse() {
            return reference.asJSONObject();
        }

        public Map<String, Asset> getAssets() {
            return Asset.deserializeNativeVideos(getResponse());
        }

        private JSONArray getVideos() {
            try {
                JSONObject response = reference.asJSONObject();
                return response.getJSONArray("videos");
            } catch (JSONException ex) {
                throw new Error(ex);
            }
        }

        private List<String> getVideoURIs() {
            try {
                JSONArray videos = getVideos();
                List<String> filenames = new ArrayList<>();
                for (int i = 0; i < videos.length(); ++i) {
                    filenames.add(videos.getJSONObject(i).getString("video"));
                }
                return filenames;
            } catch (JSONException ex) {
                throw new Error(ex);
            }
        }
    }

    private static class WebViewPrefetchResponse {
        final ResponseDescriptor reference;
        final JSONObject response;
        final JSONObject cacheAssets;
        final JSONArray templates;

        WebViewPrefetchResponse(ResponseDescriptor reference) {
            this.reference = reference;
            response = reference.asJSONObject();
            cacheAssets = response.optJSONObject("cache_assets");
            templates = cacheAssets.optJSONArray("templates");
        }
    }

    private static class RequestTimes {
        final long processingNs;
        final long getResponseCodeNs;
        final long readDataNs;

        RequestTimes(long processingNs, long getResponseCodeNs, long readDataNs) {
            this.processingNs = processingNs;
            this.getResponseCodeNs = getResponseCodeNs;
            this.readDataNs = readDataNs;
        }

        static RequestTimes random() {
            Random r = new Random();
            long readDataNs = TimeUnit.MILLISECONDS.toNanos(120L + r.nextInt(500));
            long getResponseCodeNs = TimeUnit.MILLISECONDS.toNanos(40L + r.nextInt(400));
            long processingNs = readDataNs + getResponseCodeNs + TimeUnit.MILLISECONDS.toNanos(20L + r.nextInt(300));

            return new RequestTimes(processingNs, getResponseCodeNs, readDataNs);
        }
    }
}
