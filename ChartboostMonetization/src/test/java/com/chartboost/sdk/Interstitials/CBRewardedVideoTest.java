package com.chartboost.sdk.Interstitials;

import static com.chartboost.sdk.test.TestUtils.assertAssetsInCache;
import static com.chartboost.sdk.test.TestUtils.randomString;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.chartboost.sdk.PlayServices.BaseTest;

import org.junit.Test;

public class CBRewardedVideoTest extends BaseTest {
    private final String location = randomString("a location");
    //TODO redo with new api
    @Test
    public void testCacheNoResults() {

    }
//    @Test
//    public void testCacheNoResults() throws InterruptedException, TimeoutException {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(noAdvertiserCampaigns(Endpoint.webviewV2RewardGet))
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            Chartboost.cacheRewardedVideo(location);
//
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didFailToLoadRewardedVideo(anyString(), any(CBError.CBImpressionError.class));
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didFailToLoadRewardedVideo(location, CBError.CBImpressionError.NO_AD_FOUND);
//        }
//    }
//
//    @Test
//    public void testCacheWebViewWithResults() throws InterruptedException, TimeoutException {
//        AssetDescriptor[] prefetchAssets = ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors;
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(prefetchAssets)
//                .withSpyOnClass(Prefetcher.class)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            verify(tc.prefetcher, times(1)).prefetch();
//
//            Chartboost.cacheRewardedVideo(location);
//
//            tc.runExceptNextDelegateCall();
//            assertAssetsInCache(prefetchAssets, tc.internalBaseDir);
//
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheRewardedVideo(location);
//
//            verify(tc.prefetcher, times(1)).prefetch(); // this was 3.  adserver no longer sends prefetch_required=true
//        }
//    }
//
//    @Test
//    public void testShowWebViewWithResults() throws InterruptedException, TimeoutException {
//        AssetDescriptor[] prefetchAssets = ReferenceResponse.webviewV2RewardGetWithResultsAssetDescriptors;
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2RewardGetWithResults)
//                .withResponses(prefetchAssets)
//                .withSpyOnClass(Prefetcher.class)
//                .withSpyOnClass(CBUIManager.class)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//            verify(tc.prefetcher, times(1)).prefetch();
//
//            when(tc.delegate.shouldDisplayRewardedVideo(location)).thenReturn(true);
//
//            Chartboost.cacheRewardedVideo(location);
//
//            tc.runExceptNextDelegateCall();
//
//            assertAssetsInCache(prefetchAssets, tc.internalBaseDir);
//            verify(tc.delegate, never()).didCacheRewardedVideo(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheRewardedVideo(location);
//
//            verify(tc.uiManager, never()).queueDisplayView(any(CBImpression.class));
//            verify(tc.delegate, never()).willDisplayVideo(anyString());
//
//            Chartboost.showRewardedVideo(location);
//            tc.run();
//
//            ArgumentCaptor<CBImpression> impressionCaptor = ArgumentCaptor.forClass(CBImpression.class);
//            verify(tc.uiManager).queueDisplayView(impressionCaptor.capture());
//            CBImpression impression = impressionCaptor.getValue();
//            assertNotNull(impression);
//            verify(tc.delegate).willDisplayVideo(location);
//            verify(tc.uiManager).startActivity(impression);
//        }
//    }
}
