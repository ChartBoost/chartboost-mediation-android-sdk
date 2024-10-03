package com.chartboost.sdk.Interstitials;

import static com.chartboost.sdk.test.TestUtils.assertAssetsInCache;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.chartboost.sdk.PlayServices.BaseTest;

import org.junit.Test;

public class CBInterstitialTest extends BaseTest {
    private final String location = "a location";
    //TODO fix for the new interstital api
    @Test
    public void testCacheNoResults() {

    }
//    @Test
//    public void testCacheNoResults() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(noAdvertiserCampaigns(Endpoint.webviewV2InterstitialGet))
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(true);
//
//            Chartboost.cacheInterstitial(location);
//
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate, never()).didFailToLoadInterstitial(anyString(), any(CBError.CBImpressionError.class));
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didFailToLoadInterstitial(location, CBError.CBImpressionError.NO_AD_FOUND);
//        }
//    }
//
//    @Test
//    public void testCacheWebViewWithResults() {
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
//            assertAssetsInCache(webviewV2InterstitialGetWithResultsAssetDescriptors, tc.internalBaseDir);
//            verify(tc.delegate, never()).didCacheInterstitial(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheInterstitial(location);
//        }
//    }
//
//    @Test
//    public void testShowWebViewWithResults() {
//        AssetDescriptor[] prefetchAssets = ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors;
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                .withResponses(prefetchAssets)
//                .withSpyOnClass(Prefetcher.class)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            verify(tc.prefetcher, times(1)).prefetch();
//
//            when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(true);
//
//            Chartboost.cacheInterstitial(location);
//
//            tc.runExceptNextDelegateCall();
//
//            assertAssetsInCache(prefetchAssets, tc.internalBaseDir);
//
//            verify(tc.prefetcher, times(1)).prefetch();
//
//            verify(tc.delegate, never()).didCacheInterstitial(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheInterstitial(location);
//
//            Chartboost.showInterstitial(location);
//            tc.run();
//        }
//    }
//
//    /*
//        The SDK<=6 CBImpressionManager removes the cached impression after CBImpression.shownFully()
//        is called (which the native bridge calls when the JavaScript calls "show").
//
//        After the app calls show() and while the ad is visible:
//          - hasCached() return false (unless a new ad is cached)
//          - the app can call cache() again to cache a new ad
//          - it's also at this point that the SDK "auto caches" the next ad.
//
//        The new AdUnitManager will need to match this behavior.
//     */
//    @Test
//    public void webViewHasCacheAfterShownFully() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors)
//                .withSpyOnClass(Prefetcher.class)
//                .withSpyOnClass(CBUIManager.class)
//                .startSdkWithDelegate()) {
//            tc.advanceThroughOnStart();
//
//            when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(true);
//
//            Chartboost.cacheInterstitial(location);
//
//            tc.run();
//
//            doReturn(true).when(tc.delegate).shouldDisplayInterstitial(location);
//
//            Chartboost.showInterstitial(location);
//
//            tc.run();
//
//            ArgumentCaptor<CBImpression> impressionCaptor = ArgumentCaptor.forClass(CBImpression.class);
//            verify(tc.uiManager).queueDisplayView(impressionCaptor.capture());
//            CBImpression impression = impressionCaptor.getValue();
//
//            assertTrue(Chartboost.hasInterstitial(location));
//            impression.shownFully();
//            tc.runNextBackgroundRunnable();
//            assertFalse(Chartboost.hasInterstitial(location));
//        }
//    }
}
