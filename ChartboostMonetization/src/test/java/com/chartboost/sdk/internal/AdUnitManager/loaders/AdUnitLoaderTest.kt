package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetDownloadedCallback
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetsDownloader
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnitBannerData
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class AdUnitLoaderTest {
    private val interstitialTraits = AdType.Interstitial
    private val rewardedTraits = AdType.Rewarded
    private val bannerTraits = AdType.Banner
    private val fileCacheMock = mockk<FileCache>()
    private val reachabilityMock = mockk<CBReachability>()
    private val videoRepository = mockk<VideoRepository>()
    private val assetDownloaderMock = mockk<AssetsDownloader>()
    private val adLoaderMock = mockk<AdLoader>()
    private val ortbLoaderMock = mockk<OrtbLoader>()
    private val mediationMock = mockk<Mediation>()
    private val adUnitLoaderAdCallbackMock = mockk<AdUnitLoaderAdCallback>()
    private val loadResultMock = mockk<LoadResult>()
    private val appRequestMock = mockk<AppRequest>()
    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()

    private val loadParamsSlot = CapturingSlot<LoadParams>()
    private val resultsSlot = CapturingSlot<(LoadResult) -> Unit>()
    private val appRequestSlot = CapturingSlot<AppRequest>()
    private val adTraitNameSlot = CapturingSlot<String>()
    private val onAssetDownloadedCallbackSlot = CapturingSlot<AssetDownloadedCallback>()
    private val adUnitLoaderCallbackSlot = CapturingSlot<AdUnitLoaderCallback>()

    private val location = "test_location"
    private val adUnit = AdUnit(impressionId = "1")

    private val validBidResponse =
        "{\n" +
            "\t\"id\": \"cac2faea23eeecc135bf4e678116c0c339fa915f\",\n" +
            "\t\"seatbid\": [{\n" +
            "\t\t\"bid\": [{\n" +
            "\t\t\t\"id\": \"sbk-incubation-mode-bidid\",\n" +
            "\t\t\t\"impid\": \"sdk-incubation-mode-impid\",\n" +
            "\t\t\t\"price\": 0.1,\n" +
            "\t\t\t\"adid\": \"1\",\n" +
            "\t\t\t\"adm\": \"PGRpdiBpZD0iY29udGFpbmVyIiBzdHlsZT0id2lkdGg6MTAwJTsgaGVpZ2h0OjEwMCU7IGJhY2tncm91bmQtY29sb3I6IzAwMDAwMDsiPgogIDxhIGhyZWY9Imh0dHBzOi8vd3d3LmNoYXJ0Ym9vc3QuY29tIj4KICAgIDxpbWcKICAgICAgc3JjPSJodHRwczovL2EuY2hhcnRib29zdC5jb20vYmlkZGVyL2NyZWF0aXZlcy9kZWZhdWx0L2Jhbm5lci9hZC5qcGciCiAgICAgIHN0eWxlPSJkaXNwbGF5OmJsb2NrO2hlaWdodDoxMDAlO3dpZHRoOjEwMCUiCiAgICAgIGhlaWdodD0iNTAiCiAgICAgIHdpZHRoPSIzMjAiCiAgICAvPgo8L2E+CjwvZGl2Pg==\",\n" +
            "\t\t\t\"adomain\": [\"wwf2.ios.zynga.com\"],\n" +
            "\t\t\t\"bundle\": \"1196764367\",\n" +
            "\t\t\t\"cid\": \"94fe3fd0fb02c65ad2d415f1364824e55c0f3613\",\n" +
            "\t\t\t\"crid\": \"770458c0756801371a1422000a8873b9\",\n" +
            "\t\t\t\"ext\": {\n" +
            "\t\t\t\t\"adId\": \"VyUdEMeZCkSbtF2ufD9uCJTXMriabw0M7BkceZGk6l854TPI780WjJIIA==\",\n" +
            "\t\t\t\t\"cgn\": \"1\",\n" +
            "\t\t\t\t\"crtype\": \"HTML5\",\n" +
            "\t\t\t\t\"template\": \"https://t.chartboost.com/base_templates/html/mraid-iframe-open-df82555530.html\",\n" +
            "\t\t\t\t\"trackingId\": \"sdk-incubation-trackingId\",\n" +
            "\t\t\t\t\"params\": \"{\\\"encoding\\\": \\\"base64\\\",\\\"isNativePlayer\\\": \\\"true\\\", \\\"ShowCloseButton\\\": \\\"true\\\", \\\"AdType\\\": \\\"Interstitial\\\"}\",\n" +
            "\t\t\t\t\"videoUrl\": \"https://d1gnoa8d4rh1fn.cloudfront.net/19898/sniper3d_v_202010_104_en_portrait.mp4\"\n" +
            "\t\t\t}\n" +
            "\t\t}]\n" +
            "\t}]\n" +
            "}"

    private val adUnitLoaderInterstitial =
        AdUnitLoader(
            interstitialTraits,
            fileCacheMock,
            reachabilityMock,
            videoRepository,
            assetDownloaderMock,
            adLoaderMock,
            ortbLoaderMock,
            mediationMock,
            eventTracker = eventTrackerMock,
        )

    private val adUnitLoaderRewarded =
        AdUnitLoader(
            rewardedTraits,
            fileCacheMock,
            reachabilityMock,
            videoRepository,
            assetDownloaderMock,
            adLoaderMock,
            ortbLoaderMock,
            mediationMock,
            eventTracker = eventTrackerMock,
        )

    private val adUnitLoaderBanner =
        AdUnitLoader(
            bannerTraits,
            fileCacheMock,
            reachabilityMock,
            videoRepository,
            assetDownloaderMock,
            adLoaderMock,
            ortbLoaderMock,
            mediationMock,
            eventTracker = eventTrackerMock,
        )

    @BeforeEach
    fun setup() {
        every { reachabilityMock.isNetworkAvailable } returns true
        every { adLoaderMock.loadAd(any(), any()) }.answers { }
        every { ortbLoaderMock.loadAd(any(), any()) }.answers { }
        every { assetDownloaderMock.downloadAdUnitAssets(any(), any(), any(), any()) }.answers {}
        every { adUnitLoaderAdCallbackMock.onCacheSuccess(any(), any()) }.answers { }
        every { adUnitLoaderAdCallbackMock.onAdFailToLoad(any(), any()) }.answers { }
        every { loadResultMock.readDataNs } returns 1
        every { loadResultMock.requestResponseCodeNs } returns 1
        every { loadResultMock.appRequest } returns appRequestMock
        every { loadResultMock.error } returns null
        every { loadResultMock.adUnit } returns adUnit
        every { videoRepository.downloadVideoFile(any(), any(), any(), any()) }.answers { }
    }

    @Test
    fun `load banner success`() {
        adUnitLoaderBanner.load(location, adUnitLoaderAdCallbackMock, null, AdUnitBannerData(mockk(), 100, 100))
        io.mockk.verify(exactly = 1) {
            adLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        assertEquals(100, loadParamsSlot.captured.bannerHeight)
        assertEquals(100, loadParamsSlot.captured.bannerWidth)

        resultsSlot.captured.invoke(loadResultMock)

        io.mockk.verify(exactly = 1) {
            assetDownloaderMock.downloadAdUnitAssets(
                capture(appRequestSlot),
                capture(adTraitNameSlot),
                capture(onAssetDownloadedCallbackSlot),
                capture(adUnitLoaderCallbackSlot),
            )
        }

        assertNotNull(appRequestSlot.captured.adUnit)
        assertEquals(adTraitNameSlot.captured, AdType.Banner.name)
        adUnitLoaderCallbackSlot.captured.onAdUnitCacheSuccess(appRequestSlot.captured, TrackingEventName.Cache.START)
        io.mockk.verify(exactly = 1) { adUnitLoaderAdCallbackMock.onCacheSuccess(any(), any()) }
    }

    @Test
    fun `load rewarded success`() {
        adUnitLoaderRewarded.load(location, adUnitLoaderAdCallbackMock)
        io.mockk.verify(exactly = 1) {
            adLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        assertNull(loadParamsSlot.captured.bannerHeight)
        assertNull(loadParamsSlot.captured.bannerWidth)
        resultsSlot.captured.invoke(loadResultMock)

        io.mockk.verify(exactly = 1) {
            assetDownloaderMock.downloadAdUnitAssets(
                capture(appRequestSlot),
                capture(adTraitNameSlot),
                capture(onAssetDownloadedCallbackSlot),
                capture(adUnitLoaderCallbackSlot),
            )
        }

        assertNotNull(appRequestSlot.captured.adUnit)
        assertEquals(adTraitNameSlot.captured, AdType.Rewarded.name)
        adUnitLoaderCallbackSlot.captured.onAdUnitCacheSuccess(appRequestSlot.captured, TrackingEventName.Cache.START)
        io.mockk.verify(exactly = 1) { adUnitLoaderAdCallbackMock.onCacheSuccess(any(), any()) }
    }

    @Test
    fun `load interstitial success`() {
        adUnitLoaderInterstitial.load(location, adUnitLoaderAdCallbackMock)
        io.mockk.verify(exactly = 1) {
            adLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        assertNull(loadParamsSlot.captured.bannerHeight)
        assertNull(loadParamsSlot.captured.bannerWidth)
        resultsSlot.captured.invoke(loadResultMock)

        io.mockk.verify(exactly = 1) {
            assetDownloaderMock.downloadAdUnitAssets(
                capture(appRequestSlot),
                capture(adTraitNameSlot),
                capture(onAssetDownloadedCallbackSlot),
                capture(adUnitLoaderCallbackSlot),
            )
        }

        assertNotNull(appRequestSlot.captured.adUnit)
        assertEquals(adTraitNameSlot.captured, AdType.Interstitial.name)
        adUnitLoaderCallbackSlot.captured.onAdUnitCacheSuccess(appRequestSlot.captured, TrackingEventName.Cache.START)
        io.mockk.verify(exactly = 1) { adUnitLoaderAdCallbackMock.onCacheSuccess(any(), any()) }
    }

    @Test
    fun `load interstitial failure`() {
        val error =
            CBError(
                CBError.Internal.INVALID_RESPONSE,
                "Error parsing response",
            )
        every { loadResultMock.error }.returns(error)

        adUnitLoaderInterstitial.load(location, adUnitLoaderAdCallbackMock)
        io.mockk.verify(exactly = 1) {
            adLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        assertNull(loadParamsSlot.captured.bannerHeight)
        assertNull(loadParamsSlot.captured.bannerWidth)
        resultsSlot.captured.invoke(loadResultMock)

        assertNull(loadParamsSlot.captured.appRequest.adUnit)
        verify(exactly = 1) {
            adUnitLoaderAdCallbackMock.onAdFailToLoad(any(), CBError.Impression.INVALID_RESPONSE)
        }
    }

    @Test
    fun `load interstitial open rtb success`() {
        adUnitLoaderInterstitial.load(location, adUnitLoaderAdCallbackMock, validBidResponse)
        io.mockk.verify(exactly = 1) {
            ortbLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        assertNull(loadParamsSlot.captured.bannerHeight)
        assertNull(loadParamsSlot.captured.bannerWidth)
        resultsSlot.captured.invoke(loadResultMock)

        assertNotNull(loadParamsSlot.captured.appRequest.adUnit)
        io.mockk.verify(exactly = 1) { adUnitLoaderAdCallbackMock.onCacheSuccess(any(), any()) }
    }

    @Test
    fun `load interstitial open rtb failure`() {
        val error =
            CBError(
                CBError.Internal.UNEXPECTED_RESPONSE,
                "Error parsing response",
            )
        every { loadResultMock.error }.returns(error)

        adUnitLoaderInterstitial.load(location, adUnitLoaderAdCallbackMock, "{].}")
        io.mockk.verify(exactly = 1) {
            ortbLoaderMock.loadAd(
                capture(loadParamsSlot),
                capture(resultsSlot),
            )
        }

        try {
            assertNull(loadParamsSlot.captured.bannerHeight)
            assertNull(loadParamsSlot.captured.bannerWidth)
            resultsSlot.captured.invoke(loadResultMock)
            assertNull(loadParamsSlot.captured.appRequest.adUnit)
        } catch (e: UninitializedPropertyAccessException) {
            assert(true)
        }
        io.mockk.verify(exactly = 1) { adUnitLoaderAdCallbackMock.onAdFailToLoad(any(), CBError.Impression.INTERNAL) }
    }

    @Test
    fun `async load interstitial success`() {
        runBlocking {
            withContext(Dispatchers.Default) {
                runAsync {
                    val loadParamsSlotLocal = CapturingSlot<LoadParams>()
                    val resultsSlotLocal = CapturingSlot<(LoadResult) -> Unit>()

                    adUnitLoaderInterstitial.load(location, adUnitLoaderAdCallbackMock)
                    io.mockk.verify(atLeast = 1) {
                        adLoaderMock.loadAd(
                            capture(loadParamsSlotLocal),
                            capture(resultsSlotLocal),
                        )
                    }
                    resultsSlotLocal.captured.invoke(loadResultMock)

                    io.mockk.verify {
                        assetDownloaderMock.downloadAdUnitAssets(
                            any(),
                            any(),
                            any(),
                            any(),
                        )
                    }
                }
            }
        }
    }

    // I run 10 coroutines with 100 actions each. More than that could lead to out of memory issues
    // for example: 100 x 1000 was causing problems
    suspend fun runAsync(action: suspend () -> Unit) {
        val n = 3 // number of coroutines to launch
        val k = 100 // times an action is repeated by each coroutine
        val time =
            measureTimeMillis {
                coroutineScope { // scope for coroutines
                    repeat(n) {
                        launch {
                            repeat(k) { action() }
                        }
                    }
                }
            }
        println("Completed ${n * k} actions in $time ms")
    }
}
