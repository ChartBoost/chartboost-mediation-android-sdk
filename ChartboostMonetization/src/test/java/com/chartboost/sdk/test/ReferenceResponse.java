package com.chartboost.sdk.test;

import static com.chartboost.sdk.test.AssetDescriptor.closeButtonVideo60x60;
import static com.chartboost.sdk.test.AssetDescriptor.interstitialFrameVideo2048x1536;
import static com.chartboost.sdk.test.AssetDescriptor.placeholderImage;
import static com.chartboost.sdk.test.AssetDescriptor.placeholderVideo;
import static com.chartboost.sdk.test.AssetDescriptor.videoDefaultAssetDownload;
import static com.chartboost.sdk.test.AssetDescriptor.videoDefaultAssetPlayFree;
import static com.chartboost.sdk.test.AssetDescriptor.videoDefaultAssetWatch;
import static com.chartboost.sdk.test.AssetDescriptor.videoFrame960x640;
import static com.chartboost.sdk.test.AssetDescriptor.videoIconRewardedCoin;
import static com.chartboost.sdk.test.TestUtils.readResourceToByteArray;

public class ReferenceResponse {
    public static ResponseDescriptor noAdvertiserCampaigns(Endpoint endpoint) {
        return respondWithResource(
                endpoint,
                "com/chartboost/error_responses/no_advertiser_campaigns.json");
    }

    public static ResponseDescriptor emptyJsonBody(Endpoint endpoint) {
        return respondWithResource(endpoint, "com/chartboost/misc/empty.json");
    }

    public static ResponseDescriptor badJson(Endpoint endpoint) {
        return respondWithResource(endpoint, "com/chartboost/misc/bad_json.json");
    }

    public static final ResponseDescriptor apiConfigSuccessWebView =
            respondWithResource(
                    Endpoint.apiConfig,
                    "com/chartboost/api/config/success_webview.json");

    public static final ResponseDescriptor apiVideoPrefetchWithResults =
            respondWithResource(
                    Endpoint.apiVideoPrefetch,
                    "com/chartboost/api/video-prefetch/success_with_videos.json");

    public static final AssetDescriptor[] apiVideoPrefetchWithResultsAssetDescriptors = new AssetDescriptor[]{
            AssetDescriptor.videoAd5704d9,
            AssetDescriptor.videoAd55f35e,
            AssetDescriptor.videoAd56a719,
            AssetDescriptor.videoAd5721b5,
            AssetDescriptor.videoAd5697d4,
            AssetDescriptor.videoAd570593,
            AssetDescriptor.videoAd5668ca,
            AssetDescriptor.videoAd56abc8
    };

    public static final ResponseDescriptor apiVideoPrefetchNoVideos =
            respondWithResource(
                    Endpoint.apiVideoPrefetch,
                    "com/chartboost/api/video-prefetch/success_no_videos.json");

    // Created by hand.  Has no "videos" key.
    public static final ResponseDescriptor apiVideoPrefetchNoVideosKey =
            respondWithResource(
                    Endpoint.apiVideoPrefetch,
                    "com/chartboost/api/video-prefetch/success_no_videos_key.json");

    public static final ResponseDescriptor interstitialGetWithResults =
            respondWithResource(
                    Endpoint.interstitialGet,
                    "com/chartboost/interstitial/get/response_with_results.json");

    public static final ResponseDescriptor interstitialGetWithResultsVideoUrl =
            respondWithResource(
                    Endpoint.interstitialGet,
                    "com/chartboost/interstitial/get/Android-get-interstitial_video_url.json");

    public static final AssetDescriptor[] interstitialGetWithResultsAssetDescriptors = {
            AssetDescriptor.closeButton60x60,
            AssetDescriptor.interstitialFrame640x1088,
            AssetDescriptor.imageLevel23
    };

    public static final ResponseDescriptor interstitialGetVideoLandscape =
            respondWithResource(
                    Endpoint.interstitialGet,
                    "com/chartboost/interstitial/get/video_landscape.json");

    public static final AssetDescriptor[] interstitialGetVideoLandscapeAssetDescriptors = {
            placeholderImage("https://a.chartboost.com/video/default_assets/replay_transparent.png"),
            placeholderVideo("https://v.chartboost.com/videoads/5705a36a04b0166bfbfc7419_568-1459987306.mp4"),
            videoFrame960x640,
            videoDefaultAssetDownload,
            placeholderImage("https://a.chartboost.com/creatives/53bb1beb1873da16e76c42c4/0e0579ac408a16198449267ac99ce89c3075dfd6.jpeg"),
            closeButtonVideo60x60
    };

    public static final ResponseDescriptor interstitialGetVideoPortrait =
            respondWithResource(
                    Endpoint.interstitialGet,
                    "com/chartboost/interstitial/get/video_portrait.json");

    public static final AssetDescriptor[] interstitialGetVideoPortraitAssetDescriptors = {
            placeholderImage("https://a.chartboost.com/video/default_assets/replay_transparent.png"),
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/9-16/video-900x1600.png"),
            placeholderVideo("https://v.chartboost.com/videoads/56f439f5a8b63c1196588441_568-1458846197.mp4"),
            placeholderImage("https://a.chartboost.com/creatives/56711c042fdf340d3cef7303/5f1318f32c95630c41e8c311da69aaff33f1306c.jpeg"),
            videoDefaultAssetDownload,
            closeButtonVideo60x60
    };

    public static final ResponseDescriptor interstitialShowImpressionRecorded =
            respondWithResource(
                    Endpoint.interstitialShow,
                    "com/chartboost/interstitial/show/impression_recorded.json");

    public static final ResponseDescriptor rewardGetWithResults =
            respondWithResource(
                    Endpoint.rewardGet,
                    "com/chartboost/reward/get/response_with_results.json");

    public static final AssetDescriptor[] rewardGetWithResultsAssetDescriptors = {
            placeholderImage("https://a.chartboost.com/video/default_assets/replay_transparent.png"),
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/9-16/video-1152x2048.png"),
            placeholderVideo("https://v.chartboost.com/videoads/57721f9c43150f39a14db63c_568-1467096988.mp4"),
            closeButtonVideo60x60,
            videoDefaultAssetPlayFree,
            videoIconRewardedCoin,
            interstitialFrameVideo2048x1536,
            videoDefaultAssetWatch,
            videoDefaultAssetDownload,
            placeholderImage("https://a.chartboost.com/creatives/576b579a43150f58de7adb11/22067746778826480108d1b2fa529caf0519dd8b.jpeg"),
            placeholderImage("https://a.chartboost.com/creatives/576b579a43150f58de7adb11/39abe48cdf23cf5392530f7b4e766035572729bf.jpeg")
    };

    public static final ResponseDescriptor v3LoadInterstitials =
            respondWithResource(
                    Endpoint.v3Load,
                    "live.chartboost.com/sdk/v3/load/interstitials.json");

    public static final ResponseDescriptor v3LoadConvertedInterstitialGetVideo =
            respondWithResource(
                    Endpoint.v3Load,
                    "live.chartboost.com/sdk/v3/load/converted/webview/v2/interstitial/get/response_video.json");

    public static final ResponseDescriptor v3LoadConvertedRewardGet =
            respondWithResource(
                    Endpoint.v3Load,
                    "live.chartboost.com/sdk/v3/load/converted/webview/v2/reward/get/response_with_results.json");

    public static final ResponseDescriptor webviewV2PrefetchWithResults =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_with_results.json");

    public static final ResponseDescriptor webviewV2PrefetchWithResultsSubDir =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_with_results_subdir.json");

    public static final AssetDescriptor[] webviewV2PrefetchWithResultsAssetDescriptors = {
            AssetDescriptor.baseTemplate2e34e6,
            AssetDescriptor.baseTemplate33cda9,
            AssetDescriptor.videoAd56ff66,
            AssetDescriptor.videoAdMagicWars2,
            AssetDescriptor.videoAdMagicWars3,
            AssetDescriptor.videoAd571459,
            AssetDescriptor.videoAd55fb49,
            AssetDescriptor.videoAd567a70,
            AssetDescriptor.videoAd571b10,
            AssetDescriptor.videoAd57148e,
            AssetDescriptor.videoAdMagicWars1

    };

    // This has the first template and the first five videos from the original reference
    public static final ResponseDescriptor webviewV2PrefetchWithResultsSplit1 =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_with_results_split_1.json");

    public static final AssetDescriptor[] webviewV2PrefetchWithResultsSplit1AssetDescriptors = {
            AssetDescriptor.baseTemplate33cda9,
            AssetDescriptor.videoAd56ff66,
            AssetDescriptor.videoAdMagicWars2,
            AssetDescriptor.videoAdMagicWars3,
            AssetDescriptor.videoAd571459,
            AssetDescriptor.videoAd55fb49
    };

    // This has the second template and the last four videos from the original reference
    public static final ResponseDescriptor webviewV2PrefetchWithResultsSplit2 =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_with_results_split_2.json");

    public static final AssetDescriptor[] webviewV2PrefetchWithResultsSplit2AssetDescriptors = {
            AssetDescriptor.baseTemplate2e34e6,
            AssetDescriptor.videoAd567a70,
            AssetDescriptor.videoAd571b10,
            AssetDescriptor.videoAd57148e,
            AssetDescriptor.videoAdMagicWars1
    };

    // Created by hand.  Has no "cache_assets" key.
    public static final ResponseDescriptor webviewV2PrefetchNoCacheAssetsKey =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_no_cache_assets_key.json");

    public static final ResponseDescriptor webviewV2RewardGetWithResults =
            respondWithResource(
                    Endpoint.webviewV2RewardGet,
                    "com/chartboost/webview/v2/reward/get/response_with_results.json");

    public static final AssetDescriptor[] webviewV2RewardGetWithResultsAssetDescriptors = {
            videoDefaultAssetDownload,
            videoFrame960x640,
            AssetDescriptor.appIcon568dd7,
            videoDefaultAssetWatch,
            AssetDescriptor.baseTemplate33cda9,
            AssetDescriptor.videoDefaultAssetReplay,
            AssetDescriptor.imageSwordOfChaos,
            videoDefaultAssetPlayFree,
            AssetDescriptor.videoSwordOfChaos,
            videoIconRewardedCoin,
            closeButtonVideo60x60
    };

    public static final ResponseDescriptor webviewV2InterstitialGetWithResults =
            respondWithResource(
                    Endpoint.webviewV2InterstitialGet,
                    "com/chartboost/webview/v2/interstitial/get/response_with_results.json");

    public static final AssetDescriptor[] webviewV2InterstitialGetWithResultsAssetDescriptors = {
            AssetDescriptor.closeButton60x60,
            AssetDescriptor.interstitialFrame1088x640,
            AssetDescriptor.baseTemplateff6e78,
            AssetDescriptor.imageJewelCrack
    };

    public static final ResponseDescriptor webviewV2InterstitialGetVideo =
            respondWithResource(
                    Endpoint.webviewV2InterstitialGet,
                    "com/chartboost/webview/v2/interstitial/get/response_video.json");

    public static final AssetDescriptor[] webviewV2InterstitialGetVideoAssetDescriptors = {
            AssetDescriptor.videoDefaultAssetReplayWhite,
            AssetDescriptor.interstitialFrameVideo1152x2048,
            AssetDescriptor.closeButtonWebView60x60,
            interstitialFrameVideo2048x1536,
            videoDefaultAssetDownload,
            AssetDescriptor.imageBustersBoostLandscape,
            AssetDescriptor.imageBustersBoostPortrait,
            AssetDescriptor.videoAdChartboost,
            AssetDescriptor.imageBustersBoostIcon,
            AssetDescriptor.baseTemplatee70814
    };

    // Nexus9 Session 1 prefetch, interstitial/get, reward/get
    public static final ResponseDescriptor webviewV2PrefetchNexus9_1 =
            respondWithResource(
                    Endpoint.webviewV2Prefetch,
                    "com/chartboost/webview/v2/prefetch/response_nexus9_1.json");

    public static final AssetDescriptor[] webviewV2PrefetchNexus9_1_AssetDescriptors = {
            AssetDescriptor.baseTemplate8790ab,
            AssetDescriptor.baseTemplatee770e6
    };

    public static final ResponseDescriptor webviewV2InterstitialGet_Nexus9_1a =
            respondWithResource(
                    Endpoint.webviewV2InterstitialGet,
                    "com/chartboost/webview/v2/interstitial/get/response_nexus9_1a.json");
    public static final ResponseDescriptor webviewV2InterstitialGet_Nexus9_1b =
            respondWithResource(
                    Endpoint.webviewV2InterstitialGet,
                    "com/chartboost/webview/v2/interstitial/get/response_nexus9_1b.json");

    public static final ResponseDescriptor webviewV2InterstitialGet_CrossInstall =
            respondWithResource(
                    Endpoint.webviewV2InterstitialGet,
                    "com/chartboost/webview/v2/interstitial/get/cross_install.json");

    public static final ResponseDescriptor webviewV2RewardGet_Nexus9_1a =
            respondWithResource(
                    Endpoint.webviewV2RewardGet,
                    "com/chartboost/webview/v2/reward/get/response_nexus9_1a.json");
    public static final ResponseDescriptor webviewV2RewardGet_Nexus9_1b =
            respondWithResource(
                    Endpoint.webviewV2RewardGet,
                    "com/chartboost/webview/v2/reward/get/response_nexus9_1a.json");

    public static ResponseDescriptor respondWithResource(Endpoint endpoint, String resourcePath) {
        byte[] contents = readResourceToByteArray(resourcePath);
        return endpoint.ok(contents);
    }

}
