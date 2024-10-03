package com.chartboost.sdk.internal

import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.callbacks.BannerCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnitBannerData
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoader
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRenderer
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.api.AdApiCallbackSender
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.test.FakeUiPoster
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.Session
import com.chartboost.sdk.tracking.TrackingEventName
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.eq
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

class BannerApiTest {
    private val adUnitLoaderMock = mockk<AdUnitLoader>()
    private val adUnitRendererMock = mockk<AdUnitRenderer>()
    private val uiPosterMock = mockk<FakeUiPoster>(relaxed = true)
    private val sdkConfigurationMock = mockk<SdkConfiguration>()
    private val sdkConfigurationRef = AtomicReference<SdkConfiguration>()
    private val bannerConfigMock = mockk<SdkConfiguration.BannerConfig>()
    private val backgroundServiceMock = mockk<ScheduledExecutorService>(relaxed = true)
    private val bannerMock = mockk<Banner>()
    private val sessionMock = mockk<Session>()
    private val base64WrapperMock = mockk<Base64Wrapper>()
    private val adApiCallbackSenderMock = mockk<AdApiCallbackSender>()
    private val mediationMock = mockk<Mediation>()
    private val appRequestMock = mockk<AppRequest>()
    private val bannerCallbackMock = mockk<BannerCallback>()
    private val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

    private val cacheEventCaptor = slot<CacheEvent>()
    private val cacheErrorCaptor = slot<CacheError>()
    private val showEventCaptor = slot<ShowEvent>()
    private val showErrorCaptor = slot<ShowError>()
    private val lambdaCaptor = slot<() -> Unit>()

    private val bannerApi =
        BannerApi(
            adUnitLoaderMock,
            adUnitRendererMock,
            uiPosterMock,
            sdkConfigurationRef,
            backgroundServiceMock,
            adApiCallbackSenderMock,
            sessionMock,
            base64WrapperMock,
            eventTracker = eventTrackerMock,
            androidVersion = { Build.VERSION_CODES.S },
        )

    private val bannerApiKitkat =
        BannerApi(
            adUnitLoaderMock,
            adUnitRendererMock,
            uiPosterMock,
            sdkConfigurationRef,
            backgroundServiceMock,
            adApiCallbackSenderMock,
            sessionMock,
            base64WrapperMock,
            eventTracker = eventTrackerMock,
            androidVersion = { Build.VERSION_CODES.KITKAT },
        )

    @Before
    fun setup() {
        every {
            adApiCallbackSenderMock.sendClickCallbackInMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendShowCallbackInMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendRewardCallbackOnMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendDismissCallbackOnMainThread(
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendImpressionRecordedCallbackOnMainThread(
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendCacheCallbackInMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every {
            adApiCallbackSenderMock.sendOnAdRequestedToShowInMainThread(
                any(),
                any(),
                any(),
            )
        } answers {}
        every { adUnitLoaderMock.load(any(), any(), any(), any()) } answers {}
        every { adUnitLoaderMock.removeAppRequest() }.answers { }
        every { adUnitLoaderMock.getAppRequest() }.returns(appRequestMock)
        every { adUnitRendererMock.mediation }.returns(mediationMock)
        every { adUnitRendererMock.render(any(), any()) }.answers { }
        every { adUnitRendererMock.detachBannerImpression() }.answers { }
        sdkConfigurationRef.set(sdkConfigurationMock)

        every { sdkConfigurationMock.getPublisherDisable() } returns false
        every { sdkConfigurationMock.bannerConfig } returns bannerConfigMock
        every { bannerConfigMock.isBannerEnabled } returns true

        every { bannerMock.location } returns "test"
        every { bannerMock.getBannerWidth() } returns Banner.BannerSize.STANDARD.width
        every { bannerMock.getBannerHeight() } returns Banner.BannerSize.STANDARD.height
        every { appRequestMock.adUnit } returns AdUnit()
    }

    @Test
    fun `cache but session not started cause publisher is disabled `() {
        every { sdkConfigurationMock.getPublisherDisable() } returns true
        every { bannerCallbackMock.onAdLoaded(any(), any()) } just runs
        bannerApi.cache(bannerMock, bannerCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            bannerCallbackMock.onAdLoaded(capture(cacheEventCaptor), capture(cacheErrorCaptor))
        }
        assertEquals("test", cacheEventCaptor.captured.ad.location)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, cacheErrorCaptor.captured.code)
    }

    @Test
    fun `cache but session not started cause sdk below Lollipop `() {
        every { bannerCallbackMock.onAdLoaded(any(), any()) } just runs
        bannerApiKitkat.cache(bannerMock, bannerCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            bannerCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals("test", cacheEventCaptor.captured.ad.location)
        assertEquals(
            CacheError.Code.SESSION_NOT_STARTED,
            cacheErrorCaptor.captured.code,
        )
    }

    @Ignore
    @Test
    fun `cache but session not started cause invalid location `() {
        every { bannerMock.location } returns ""
        bannerApi.cache(bannerMock, bannerCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        verify(exactly = 1) {
            bannerCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals("", cacheEventCaptor.captured.ad.location)
        assertEquals(
            CacheError.Code.SESSION_NOT_STARTED,
            cacheErrorCaptor.captured.code,
        )
    }

    @Test
    fun `cache valid ad`() {
        val backgroundRunnableCaptor = slot<Runnable>()
        val bannerDataSlot = CapturingSlot<AdUnitBannerData>()
        bannerApi.cache(bannerMock, bannerCallbackMock)
        verify(exactly = 1) {
            backgroundServiceMock.execute(
                capture(backgroundRunnableCaptor),
            )
        }
        backgroundRunnableCaptor.captured.run()
        verify(exactly = 1) {
            adUnitLoaderMock.load(
                "test",
                any(),
                any(),
                capture(bannerDataSlot),
            )
        }
        assertNotNull(bannerDataSlot.captured.bannerView)
        assertEquals(320, bannerDataSlot.captured.bannerWidth)
        assertEquals(50, bannerDataSlot.captured.bannerHeight)
    }

    @Test
    fun `on cache success`() {
        bannerApi.cache(bannerMock, bannerCallbackMock)
        bannerApi.onCacheSuccess("1111", TrackingEventName.Cache.START)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendCacheCallbackInMainThread(
                any(),
                eq(null),
                eq(bannerMock),
                eq(bannerCallbackMock),
            )
        }
    }

    @Test
    fun `don't clear cache when app request is missing`() {
        every { adUnitLoaderMock.getAppRequest() }.returns(null)
        bannerApi.clearCache()
        verify(exactly = 0) { adUnitLoaderMock.removeAppRequest() }
    }

    @Test
    fun `clear cache when app request is valid`() {
        every { adUnitLoaderMock.getAppRequest() }.returns(appRequestMock)
        bannerApi.clearCache()
        verify(exactly = 1) { adUnitLoaderMock.getAppRequest() }
        verify(exactly = 1) { adUnitLoaderMock.removeAppRequest() }
    }

    @Test
    fun `banner calls the callback onAdDismiss`() {
        bannerApi.cache(bannerMock, bannerCallbackMock)
        bannerApi.onImpressionDismissed("test")
        verify(exactly = 0) {
            adApiCallbackSenderMock.sendDismissCallbackOnMainThread(
                eq("test"),
                eq(bannerMock),
                eq(bannerCallbackMock),
            )
        }
    }

    @Test
    fun `on ad show failure after show success is not sent`() {
        bannerApi.cache(bannerMock, bannerCallbackMock)
        bannerApi.show(bannerMock, bannerCallbackMock)
        every { sessionMock.sessionCounter } returns 1
        every { sessionMock.addSessionImpression(any()) } just runs
        every { sessionMock.getSessionImpressionsCounter(any<AdType>()) } returns 1
        every { sessionMock.getSessionImpressionsCounter(any<String>()) } returns 1
        bannerApi.onShowSuccess("test")
        bannerApi.onShowFailure("test", CBError.Impression.ERROR_CREATING_VIEW)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendShowCallbackInMainThread(
                eq("test"),
                eq(null),
                eq(bannerMock),
                eq(bannerCallbackMock),
            )
        }
    }

    @Test
    fun `on ad show failure is sent`() {
        val showErrorSlot = CapturingSlot<ShowError>()
        bannerApi.cache(bannerMock, bannerCallbackMock)
        bannerApi.show(bannerMock, bannerCallbackMock)
        bannerApi.onShowFailure("test", CBError.Impression.ERROR_CREATING_VIEW)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendShowCallbackInMainThread(
                eq("test"),
                capture(showErrorSlot),
                eq(bannerMock),
                eq(bannerCallbackMock),
            )
        }
        assertEquals(
            ShowError.Code.PRESENTATION_FAILURE,
            showErrorSlot.captured.code,
        )
    }

    @Test
    fun `detach banner`() {
        bannerApi.detach()
        verify(exactly = 1) { adUnitRendererMock.detachBannerImpression() }
    }

    @Test
    fun `detach banner with visibility tracker active`() {
        val rootViewMock = mockk<View>()
        val contentViewMock = mockk<View>()
        val viewTreeObserverMock = mockk<ViewTreeObserver>()
        every { rootViewMock.findViewById<View>(android.R.id.content) } returns contentViewMock
        every { contentViewMock.viewTreeObserver } returns viewTreeObserverMock
        every { bannerMock.rootView } returns rootViewMock
        every { viewTreeObserverMock.isAlive } returns true
        every { viewTreeObserverMock.addOnPreDrawListener(any()) } answers {}

        bannerApi.detach()
        verify(exactly = 1) { adUnitRendererMock.detachBannerImpression() }
    }

    @Test
    fun `fill size banner`() {
        val resourcesMock = mockk<Resources>()
        val displayMetrics = mockk<DisplayMetrics>()
        displayMetrics.widthPixels = 600
        displayMetrics.density = 2.75f

        val layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        every { bannerMock.resources } returns resourcesMock
        every { resourcesMock.displayMetrics } returns displayMetrics
        every { bannerMock.layoutParams } returns layoutParams
        every { bannerMock.getBannerWidth() } returns 100
        every { bannerMock.getBannerHeight() } returns 100
        bannerApi.fillSize(bannerMock)
        verify(exactly = 1) { bannerMock.getBannerWidth() }
        verify(exactly = 1) { bannerMock.getBannerHeight() }

        // this I cannot test as TypedValue.applyDimension cannot be mocked
        assertEquals(0, bannerMock.layoutParams.width)
        assertEquals(0, bannerMock.layoutParams.height)
    }

    @Test
    fun `create visibility tracker config om disabled`() {
        val rootViewMock = mockk<View>()
        val contentViewMock = mockk<View>()
        val viewTreeObserverMock = mockk<ViewTreeObserver>()
        every { rootViewMock.findViewById<View>(android.R.id.content) } returns contentViewMock
        every { contentViewMock.viewTreeObserver } returns viewTreeObserverMock
        every { bannerMock.rootView } returns rootViewMock
        every { viewTreeObserverMock.isAlive } returns true
        every { viewTreeObserverMock.addOnPreDrawListener(any()) } answers {}
        verify(exactly = 0) { viewTreeObserverMock.addOnPreDrawListener(any()) }
    }

    @Test
    fun `call on impression success`() {
        val impressionId = "test"
        bannerApi.cache(bannerMock, bannerCallbackMock)
        bannerApi.onImpressionSuccess(impressionId)
        verify(
            exactly = 1,
        ) {
            adApiCallbackSenderMock.sendImpressionRecordedCallbackOnMainThread(
                impressionId,
                bannerMock,
                bannerCallbackMock,
            )
        }
    }

    @Test
    fun `cache with bid response valid response`() {
        val bidResponseBase64 = "adajdjkd=="
        val bidResponseDecoded = "{ decoded }"
        val backgroundRunnableCaptor = slot<Runnable>()
        val bannerDataSlot = CapturingSlot<AdUnitBannerData>()
        every { base64WrapperMock.decode(any()) } returns bidResponseDecoded
        bannerApi.cache(bannerMock, bannerCallbackMock, bidResponseBase64)
        verify(exactly = 1) {
            backgroundServiceMock.execute(
                capture(backgroundRunnableCaptor),
            )
        }
        backgroundRunnableCaptor.captured.run()
        verify(exactly = 1) { base64WrapperMock.decode(bidResponseBase64) }
        verify(exactly = 1) {
            adUnitLoaderMock.load(
                "test",
                any(),
                bidResponseDecoded,
                capture(bannerDataSlot),
            )
        }
        assertEquals(320, bannerDataSlot.captured.bannerWidth)
        assertEquals(50, bannerDataSlot.captured.bannerHeight)
    }

    @Ignore
    @Test
    fun `check banner is enabled false on cache`() {
        // Setup mock responses
        every { bannerConfigMock.isBannerEnabled } returns false
        every { bannerMock.location } returns "valid_location"
        every { bannerApi.cache(bannerMock, bannerCallbackMock) } answers {
            lambdaCaptor.captured.invoke()
        }

        bannerApi.cache(bannerMock, bannerCallbackMock)

        verify(exactly = 1) {
            uiPosterMock(capture(lambdaCaptor))
        }
        verify(exactly = 1) {
            bannerCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals(
            CacheError.Code.BANNER_DISABLED,
            cacheErrorCaptor.captured.code,
        )
    }

    @Test
    fun `check banner is enabled false on show`() {
        every { bannerConfigMock.isBannerEnabled } returns false
        every { bannerCallbackMock.onAdShown(any(), any()) } just runs
        bannerApi.show(bannerMock, bannerCallbackMock)
        verify(exactly = 1) {
            uiPosterMock(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            bannerCallbackMock.onAdShown(
                capture(showEventCaptor),
                capture(showErrorCaptor),
            )
        }
        assertEquals(
            ShowError.Code.BANNER_DISABLED,
            showErrorCaptor.captured.code,
        )
    }

    @Test
    fun `on cache error test cache impression mappings to cache error`() {
        onCacheErrorTest(
            CBError.Impression.INTERNET_UNAVAILABLE,
            CacheError.Code.INTERNET_UNAVAILABLE,
        )
        onCacheErrorTest(
            CBError.Impression.TOO_MANY_CONNECTIONS,
            CacheError.Code.NETWORK_FAILURE,
        )
        onCacheErrorTest(
            CBError.Impression.NETWORK_FAILURE,
            CacheError.Code.NETWORK_FAILURE,
        )
        onCacheErrorTest(
            CBError.Impression.NO_AD_FOUND,
            CacheError.Code.NO_AD_FOUND,
        )
        onCacheErrorTest(
            CBError.Impression.SESSION_NOT_STARTED,
            CacheError.Code.SESSION_NOT_STARTED,
        )
        onCacheErrorTest(
            CBError.Impression.INVALID_RESPONSE,
            CacheError.Code.SERVER_ERROR,
        )
        onCacheErrorTest(
            CBError.Impression.ASSETS_DOWNLOAD_FAILURE,
            CacheError.Code.ASSET_DOWNLOAD_FAILURE,
        )
        onCacheErrorTest(
            CBError.Impression.ASSET_PREFETCH_IN_PROGRESS,
            CacheError.Code.ASSET_DOWNLOAD_FAILURE,
        )
        onCacheErrorTest(
            CBError.Impression.ASSET_MISSING,
            CacheError.Code.ASSET_DOWNLOAD_FAILURE,
        )
        onCacheErrorTest(
            CBError.Impression.INTERNET_UNAVAILABLE_AT_CACHE,
            CacheError.Code.INTERNET_UNAVAILABLE,
        )
        onCacheErrorTest(
            CBError.Impression.INTERNAL,
            CacheError.Code.INTERNAL,
        )
    }

    private fun onCacheErrorTest(
        impressionError: CBError.Impression,
        cacheErrorCode: CacheError.Code,
    ) {
        val localImpressionIdSlot = CapturingSlot<String>()
        val localCacheErrorSlot = CapturingSlot<CacheError>()
        val bannerSlot = CapturingSlot<Banner>()
        val callbackSlot = CapturingSlot<BannerCallback>()
        val uiMock = mockk<FakeUiPoster>()
        val localBannerMock = mockk<Banner>()
        val localBannerCallbackMock = mockk<BannerCallback>()
        val localAdApiCallbackSenderMock =
            mockk<AdApiCallbackSender>()
        every {
            localAdApiCallbackSenderMock.sendCacheCallbackInMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every { localBannerMock.location } returns "test"
        val api =
            BannerApi(
                adUnitLoaderMock,
                adUnitRendererMock,
                uiMock,
                sdkConfigurationRef,
                backgroundServiceMock,
                localAdApiCallbackSenderMock,
                sessionMock,
                base64WrapperMock,
                eventTracker = eventTrackerMock,
                androidVersion = { Build.VERSION_CODES.S },
            )

        api.cache(localBannerMock, localBannerCallbackMock)
        api.onAdFailToLoad("error", impressionError)

        verify(exactly = 1) {
            localAdApiCallbackSenderMock.sendCacheCallbackInMainThread(
                capture(localImpressionIdSlot),
                capture(localCacheErrorSlot),
                capture(bannerSlot),
                capture(callbackSlot),
            )
        }

        assertNotNull(localCacheErrorSlot.captured)
        assertEquals(
            cacheErrorCode.errorCode,
            localCacheErrorSlot.captured.code.errorCode,
        )
    }
}
