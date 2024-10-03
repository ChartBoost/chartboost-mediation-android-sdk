package com.chartboost.sdk;

import com.chartboost.sdk.PlayServices.BaseTest;
import org.junit.Test;

public class DelegateCallbackTest extends BaseTest {
    private final String location = "a location";

    //TODO this is invalid as we won't use global delegate anymore
    @Test
    public void test() {
    }
//    @Test
//    public void startWithAppId_callsDidInitialize() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.apiConfigSuccessWebView)
//                .withEmptyApiInstallResponse()
//                .startSdkWithDelegate()) {
//
//            verify(tc.delegate).didInitialize();
//        }
//    }
//
//    @Test
//    public void startWithAppId_callsDidInitialize_evenForBadResponse() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.badJson(Endpoint.apiConfig))
//                .withEmptyApiInstallResponse()
//                .startSdkWithDelegate()) {
//
//            // The invalid JSON will cause a JSONException
//
//            verify(tc.delegate).didInitialize();
//        }
//    }
//
//    @Test
//    public void cacheInterstitial_callsDidCacheInterstitial() {
//        MockNetworkResponses responses = new MockNetworkResponses();
//        responses.add(ReferenceResponse.webviewV2InterstitialGetWithResults);
//        responses.add(ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors);
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(true);
//
//            Chartboost.cacheInterstitial(location);
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didCacheInterstitial(anyString());
//            tc.runNextUiRunnable(); // delegate callback delivery
//
//            verify(tc.delegate).didCacheInterstitial(location);
//            tc.assertNoUiRunnablesReady();
//        }
//    }
//
//    @Test
//    public void cacheInterstitial_respectsShouldCacheInterstitial() {
//        for (boolean shouldRequest : TestUtils.eitherBoolean) {
//            try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                    .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                    .withResponses(ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors)
//                    .withSpyOnClass(CBNetworkService.class)
//                    .startSdkWithDelegate()) {
//                tc.advanceThroughOnStart();
//
//                when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(shouldRequest);
//
//                Chartboost.cacheInterstitial(location);
//                tc.runNextBackgroundRunnable();
//                verify(tc.delegate).shouldRequestInterstitial(location);
//
//                if (shouldRequest) {
//                    verify(tc.networkService).submit(argThat(hasPath("/webview/v2/interstitial/get")));
//
//                    tc.runExceptNextDelegateCall();
//
//                    verify(tc.delegate, never()).didCacheInterstitial(anyString());
//                    tc.runNextUiRunnable();
//                    verify(tc.delegate).didCacheInterstitial(location);
//                } else {
//                    verify(tc.networkService, never()).submit(argThat(hasPath("/webview/v2/interstitial/get")));
//                    tc.assertNoUiRunnablesReady();
//                    verify(tc.delegate, never()).didCacheInterstitial(anyString());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void cacheRewardedVideo_callsDidCacheRewardedVideo() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            // there is no ChartboostDelegate.shouldRequestRewardedVideo
//            Chartboost.cacheRewardedVideo(location);
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//
//            //Notify UI thread that the rewardVideo is cached
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheRewardedVideo(location);
//        }
//    }
//
//    /*
//        If you call cacheRewardedVideo() twice with the same location,
//        the didCacheRewardedVideo() delegate callback should only once.
//     */
//    @Test
//    public void cacheRewardedVideo_twice_callsDidCacheRewardedVideoOnlyOnce() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            Chartboost.cacheRewardedVideo(location);
//            Chartboost.cacheRewardedVideo(location);
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//            tc.runNextUiRunnable(); // the posted didCache delegate call
//
//            verify(tc.delegate, times(1)).didCacheRewardedVideo(location);
//            tc.assertNoUiRunnablesReady();
//        }
//    }
//
//    /*
//        If you call cacheRewardedVideo() then showRewardedVideo() before
//        the cache finishes, you still get a didCacheRewardedVideo() delegate callback.
//     */
//    @Test
//    public void cacheThenShowRewardedVideo_callsDidCacheRewardedVideo() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors)
//                .withSpyOnClass(CBUIManager.class)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            Chartboost.cacheRewardedVideo(location);
//            Chartboost.showRewardedVideo(location);
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//
//            tc.runExceptNextDelegateCall();
//
//            when(tc.delegate.shouldDisplayRewardedVideo(location)).thenReturn(true);
//
//            verify(tc.delegate, never()).shouldDisplayRewardedVideo(location);
//            tc.runNextUiRunnable();
//
//            // This is the main thing this test is verifying: that even though
//            // show() was called immediately after cache(), and nothing happened in between,
//            // we still call the didCache() callback.
//            verify(tc.delegate).didCacheRewardedVideo(location);
//
//            verify(tc.delegate, never()).shouldDisplayRewardedVideo(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).shouldDisplayRewardedVideo(location);
//
//            tc.assertNoUiRunnablesReady();
//            verify(tc.delegate, never()).didDisplayRewardedVideo(anyString());
//
//            // the webview template makes a native bridge "show" call, which ultimately
//            // calls the delegate, so simulate that:
//            assertNotNull(tc.uiManager.impressionToDisplay);
//            verify(tc.uiManager).startActivity(tc.uiManager.impressionToDisplay);
//            ArgumentCaptor<Intent> activityCaptor = ArgumentCaptor.forClass(Intent.class);
//            verify(tc.applicationContext).startActivity(activityCaptor.capture());
//            CBWebViewProtocol webViewProtocol = (CBWebViewProtocol) tc.uiManager.impressionToDisplay.getViewProtocol();
//            webViewProtocol.impression.state = ImpressionTypes.CBImpressionState.DISPLAYED;
//            webViewProtocol.onShowImpression();
//
//            verify(tc.delegate, never()).didDisplayRewardedVideo(anyString());
//            tc.run();
//            verify(tc.delegate).didDisplayRewardedVideo(location);
//            tc.assertNoUiRunnablesReady();
//        }
//    }
//
//    /*
//        If you call showRewardedVideo() without calling cacheRewardedVideo, we
//        will not call the didCacheRewardedVideo delegate callback.
//     */
//    @Test
//    public void showRewardedVideo_doesNotCallDidCacheRewardedVideo() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            when(tc.delegate.shouldDisplayRewardedVideo(location)).thenReturn(true);
//
//            Chartboost.showRewardedVideo(location);
//
//            //Processing all the template initialization (GET) requests such as
//            //Video template (html folder), images such as icons, video icons, close button etc
//            tc.run();
//
//            verify(tc.delegate, never()).didDisplayRewardedVideo(anyString());
//            tc.assertNoUiRunnablesReady();
//
//            // the webview template makes a native bridge "show" call, which ultimately
//            // calls the delegate, so simulate that
//            CBWebViewProtocol webViewProtocol = (CBWebViewProtocol) tc.uiManager.impressionToDisplay.getViewProtocol();
//            webViewProtocol.impression.state = ImpressionTypes.CBImpressionState.DISPLAYED;
//            webViewProtocol.onShowImpression();
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didDisplayRewardedVideo(anyString());
//
//            tc.runNextUiRunnable(); // the didDisplay() delegate call
//
//            verify(tc.delegate).didDisplayRewardedVideo(location);
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//            tc.assertNoUiRunnablesReady();
//        }
//    }
}
