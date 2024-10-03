package com.chartboost.sdk.internal.AssetLoader;

import static com.chartboost.sdk.test.TestUtils.assertAssetsInCache;
import static com.chartboost.sdk.test.TestUtils.writeAssetResourcesToCache;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * DEPRECATED unit test with the robolectric created an instance of the prefetcher and downloader
 * to download real file into the disk and then check if that file exists
 * TODO Test should be done via new Prefetcher test class
 */
//public class AssetLoaderTest extends BaseTest {
//
//    /*
//        A basic prefetch cycle: Verify that the assets are downloaded into the cache,
//        and that manifests are stored for the templates.
//     */
//    @Test
//    public void webviewPrefetch() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(webviewV2PrefetchWithResults)
//                .withResponses(webviewV2PrefetchWithResultsAssetDescriptors)
//                .withDownloader()
//                .build()) {
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            // Verify that the Downloader wrote the assets to the cache
//            assertAssetsInCache(webviewV2PrefetchWithResultsAssetDescriptors, tc.internalBaseDir);
//
//            assertEquals(prefetcherState(tc.prefetcher), Prefetcher.STATE_DOWNLOAD_ASSETS);
//            assertEquals(downloaderState(tc.downloader), Downloader.STATE_IDLE);
//
//            tc.prefetcher.prefetch();
//            assertEquals(prefetcherState(tc.prefetcher), Prefetcher.STATE_COOLDOWN);
//            assertEquals(downloaderState(tc.downloader), Downloader.STATE_IDLE);
//        }
//    }
//
//    /*
//        The Downloader shouldn't download anything if a successful response returns no videos.
//     */
//    @Test
//    public void testVideoPrefetcherNoVideos() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.apiVideoPrefetchNoVideos)
//                .withSpyOnNetworkService()
//                .withDownloader()
//                .build()) {
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            verify(tc.networkService).submit(argThat(hasEndpointUri(Endpoint.webviewV2Prefetch)));
//            verify(tc.networkService, times(1)).submit(any(CBNetworkRequest.class));
//
//            assertEquals(prefetcherState(tc.prefetcher), Prefetcher.STATE_COOLDOWN);
//            tc.prefetcher.prefetch();
//            tc.run();
//            assertEquals(prefetcherState(tc.prefetcher), Prefetcher.STATE_COOLDOWN);
//            assertEquals(downloaderState(tc.downloader), Downloader.STATE_IDLE);
//            verify(tc.networkService, times(1)).submit(any(CBNetworkRequest.class));
//        }
//    }
//
//    /*
//        Verify what happens if some videos to prefetch are already in the cache.
//        They should not be downloaded, and should still be in the cache after prefetch.
//     */
//    @Test
//    public void testWebViewSomeVideosAlreadyDownloaded() throws Exception {
//        List<AssetDescriptor> alreadyDownloadedAssets = Arrays.asList(
//                AssetDescriptor.videoAd56ff66,
//                AssetDescriptor.videoAdMagicWars1,
//                AssetDescriptor.baseTemplate33cda9,
//                AssetDescriptor.videoAdMagicWars2,
//                AssetDescriptor.videoAdMagicWars3);
//        AssetDescriptor expectedPrefetchTemplate = AssetDescriptor.baseTemplate2e34e6;
//        List<AssetDescriptor> expectedPrefetchVideos = Arrays.asList(
//                AssetDescriptor.videoAd55fb49,
//                AssetDescriptor.videoAd567a70,
//                AssetDescriptor.videoAd571459,
//                AssetDescriptor.videoAd571b10,
//                AssetDescriptor.videoAd57148e
//        );
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(webviewV2PrefetchWithResults)
//                .withResponse(expectedPrefetchTemplate)
//                .withResponses(expectedPrefetchVideos)
//                .withDownloader()
//                .build()) {
//
//            writeAssetResourcesToCache(alreadyDownloadedAssets, tc.internalBaseDir);
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            assertEquals(downloaderState(tc.downloader), Downloader.STATE_IDLE);
//
//            // Verify that the Downloader wrote the assets to the cache
//            // and left the assets that were already there
//            assertAssetsInCache(alreadyDownloadedAssets, tc.internalBaseDir);
//            expectedPrefetchTemplate.assertAssetInCache(tc.internalBaseDir);
//            assertAssetsInCache(expectedPrefetchVideos, tc.internalBaseDir);
//        }
//    }
//
//    @Test
//    public void testWebViewSomeVideosNotFound() throws Exception {
//        List<AssetDescriptor> videosNotFound = Arrays.asList(
//                AssetDescriptor.videoAd56ff66,
//                AssetDescriptor.videoAdMagicWars1,
//                AssetDescriptor.videoAdMagicWars2,
//                AssetDescriptor.videoAdMagicWars3,
//                AssetDescriptor.videoAd57148e);
//        List<AssetDescriptor> expectedPrefetchAssets = Arrays.asList(
//                AssetDescriptor.baseTemplate2e34e6,
//                AssetDescriptor.baseTemplate33cda9,
//                AssetDescriptor.videoAd55fb49,
//                AssetDescriptor.videoAd567a70,
//                AssetDescriptor.videoAd571459,
//                AssetDescriptor.videoAd571b10);
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(webviewV2PrefetchWithResults)
//                .withResponses(expectedPrefetchAssets)
//                .withNotFound(videosNotFound)
//                .withDownloader()
//                .build()) {
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            // Verify that the Downloader wrote the assets to the cache
//            // and left the assets that were already there
//            assertAssetsInCache(expectedPrefetchAssets, tc.internalBaseDir);
//        }
//    }
//
//    /*
//        Set the WebView configuration to have 1 max units, and verify that one of the
//        templates isn't downloaded.
//     */
//    @Test
//    public void testPrefetchWithFewerMaxUnits() {
//        List<AssetDescriptor> expectedPrefetchAssets = Arrays.asList(
//                AssetDescriptor.baseTemplate33cda9,
//                AssetDescriptor.videoAd56ff66,
//                AssetDescriptor.videoAd55fb49,
//                AssetDescriptor.videoAdMagicWars1,
//                AssetDescriptor.videoAd567a70,
//                AssetDescriptor.videoAdMagicWars2,
//                AssetDescriptor.videoAdMagicWars3,
//                AssetDescriptor.videoAd571459,
//                AssetDescriptor.videoAd571b10,
//                AssetDescriptor.videoAd57148e
//        );
//
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(webviewV2PrefetchWithResults)
//                .withResponse(AssetDescriptor.baseTemplate2e34e6)         // <-- with max units=1, this isn't downloaded, but add a response for it so the mock can download it if it tries.
//                .withResponses(expectedPrefetchAssets)
//                .withDownloader()
//                .build()) {
//
//            tc.control.configure().withWebViewCacheMaxUnits(1);
//            tc.installConfig();
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            // Verify that the Downloader wrote the assets to the cache
//            assertAssetsInCache(expectedPrefetchAssets, tc.internalBaseDir);
//
//            // should have only downloaded one template.
//            assertEquals(tc.getCacheFile(".chartboost/html").list(),
//                    new String[]{"33cda9530711983713a77eff494dd126f2fe86b5"});
//        }
//    }
//
//    /*
//        Make sure the HTTP headers are as expected.
//
//        They're different for the /api/prefetch request and for the asset/CDN requests.
//     */
//    //TODO why requests are not registered in the tests
////    @Test
////    public void testHeaders() {
////        SandboxBridgeSettings.isSandboxMode = false;
////        for (@CBReachability.CBNetworkType int connectionType : TestUtils.networkTypes) {
////            List<AssetDescriptor> expectedPrefetchTemplates = Arrays.asList(
////                    AssetDescriptor.baseTemplate2e34e6,
////                    AssetDescriptor.baseTemplate33cda9);
////            List<AssetDescriptor> expectedPrefetchVideos = Arrays.asList(
////                    AssetDescriptor.videoAd56ff66,
////                    AssetDescriptor.videoAd55fb49,
////                    AssetDescriptor.videoAdMagicWars1,
////                    AssetDescriptor.videoAd567a70,
////                    AssetDescriptor.videoAdMagicWars2,
////                    AssetDescriptor.videoAdMagicWars3,
////                    AssetDescriptor.videoAd571459,
////                    AssetDescriptor.videoAd571b10,
////                    AssetDescriptor.videoAd57148e
////            );
////            List<AssetDescriptor> expectedPrefetchAssets = new ArrayList<>();
////            expectedPrefetchAssets.addAll(expectedPrefetchTemplates);
////            expectedPrefetchAssets.addAll(expectedPrefetchVideos);
////
////            try (TestContainer tc = TestContainerBuilder.defaultWebView()
////                    .withResponse(webviewV2PrefetchWithResults)
////                    .withResponses(expectedPrefetchVideos)
////                    .withResponses(expectedPrefetchTemplates)
////                    .withReachabilityConnectionType(connectionType)
////                    .withInterceptReachability()
////                    .startSdk()) {
////
////                String expectedClient = String.format("Chartboost-Android-SDK  %s", BuildConfig.SDK_VERSION);
////
////                for (AssetDescriptor assetDescriptor : expectedPrefetchAssets) {
////                    HttpsURLConnection assetConn = tc.mockNetworkFactory.getMockConnectionReturnedForRequest(Method.GET, assetDescriptor.uri);
////                    verify(assetConn).addRequestProperty("X-Chartboost-App", tc.appId);
////                    verify(assetConn).addRequestProperty("X-Chartboost-Client", expectedClient);
////                    verify(assetConn).addRequestProperty("X-Chartboost-Reachability", Integer.toString(connectionType));
////                    verify(assetConn, times(3)).addRequestProperty(anyString(), anyString()); // and nothing else
////                }
////
////                HttpsURLConnection prefetchConn = tc.mockNetworkFactory.getMockConnectionReturnedForRequest(
////                        Method.POST, webviewV2PrefetchWithResults.endpoint.uri);
////                verify(prefetchConn).addRequestProperty("X-Chartboost-App", tc.appId);
////                verify(prefetchConn).addRequestProperty("X-Chartboost-Client", expectedClient);
////                verify(prefetchConn).addRequestProperty("X-Chartboost-API", BuildConfig.API_VERSION);
////                verify(prefetchConn).addRequestProperty(eq("X-Chartboost-Signature"), matches("[0-9A-Fa-f]{40}"));
////                verify(prefetchConn).addRequestProperty("Accept", "application/json");
////                verify(prefetchConn).addRequestProperty("Content-Type", "application/json");
////                //TODO this part keeps on failing in travis only, in android studio or local build with gradlew there is no issue
//////                if (ChartboostDSP.isDSP) {
//////                    verify(prefetchConn).addRequestProperty("X-Chartboost-DspDemoApp", "{}");
//////                    verify(prefetchConn, times(7)).addRequestProperty(anyString(), anyString()); // and nothing else
//////                } else {
//////                    verify(prefetchConn, times(6)).addRequestProperty(anyString(), anyString()); // and nothing else
//////                }
////            }
////        }
////    }
//
//    private int downloaderState(Downloader downloader) {
//        return (Integer) TestUtils.getFieldWithReflection(downloader, Downloader.class, "state");
//    }
//
//    private int prefetcherState(Prefetcher prefetcher) {
//        return (Integer) TestUtils.getFieldWithReflection(prefetcher, Prefetcher.class, "state");
//    }
//
//    @Test
//    public void testPrefetchWithVideoSubDir() {
//        List<AssetDescriptor> expectedPrefetchAssets = Arrays.asList(
//                AssetDescriptor.videoAd55fb49WithSubDir);
//
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(webviewV2PrefetchWithResultsSubDir)
//                .withResponses(expectedPrefetchAssets)
//                .withDownloader()
//                .build()) {
//
//            tc.prefetcher.prefetch();
//
//            tc.run();
//
//            // Verify that the Downloader wrote the assets to the cache
//            assertAssetsInCache(expectedPrefetchAssets, tc.internalBaseDir);
//
//            //verify the file exists
//            assertEquals(tc.getCacheFile(".chartboost/videos/chartboost/subdir1").list(),
//                    new String[]{"55fb49585b1453669ffdd4da_568-1442531710.mp4"});
//
//            //file removal test during cache clean-up
//            File expiredFile = AssetDescriptor.videoAd55fb49WithSubDir.getCacheFile(tc.internalBaseDir);
//            Long now = tc.timeSource.currentTimeMillis();
//            Long day = TimeUnit.DAYS.toMillis(1);
//
//            //setting last modified before 8 days. default time to live is 7 days after which the
//            //asset is removed from cache.
//            assertTrue(expiredFile.setLastModified(now - day * 8));
//            tc.downloader.reduceCacheSize();
//
//            //enaure that the existing asset is removed
//            assertEquals(tc.getCacheFile(".chartboost/videos/chartboost/subdir1").list(),
//                    new String[]{});
//
//        }
//    }
//}
