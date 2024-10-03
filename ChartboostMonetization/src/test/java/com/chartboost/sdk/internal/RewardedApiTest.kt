package com.chartboost.sdk.internal

import android.os.Build
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.ads.Rewarded
import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
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
import org.junit.Test
import org.mockito.kotlin.eq
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

class RewardedApiTest {
    private val adUnitLoaderMock = mockk<AdUnitLoader>()
    private val adUnitRendererMock = mockk<AdUnitRenderer>()
    private val uiPosterMock = mockk<FakeUiPoster>(relaxed = true)
    private val sdkConfigurationMock = mockk<SdkConfiguration>()
    private val sdkConfigurationRef = AtomicReference<SdkConfiguration>()
    private val backgroundServiceMock = mockk<ScheduledExecutorService>()
    private val rewardedMock = mockk<Rewarded>()
    private val rewardedCallbackMock = mockk<RewardedCallback>()
    private val adApiCallbackSenderMock = mockk<AdApiCallbackSender>()
    private val sessionMock = mockk<Session>()
    private val base64WrapperMock = mockk<Base64Wrapper>()
    private val mediationMock = mockk<Mediation>()
    private val appRequestMock = mockk<AppRequest>()
    private val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

    private val cacheEventCaptor = slot<CacheEvent>()
    private val cacheErrorCaptor = slot<CacheError>()
    private val lambdaCaptor = slot<() -> Unit>()
    private val runnableCaptor = slot<Runnable>()

    private val rewardedApi =
        RewardedApi(
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

    private val rewardedApiKitkat =
        RewardedApi(
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
        every { adUnitLoaderMock.load(any(), any(), any()) } answers {}
        every { adUnitLoaderMock.removeAppRequest() }.answers { }
        every { adUnitLoaderMock.getAppRequest() }.returns(appRequestMock)
        every { adUnitRendererMock.mediation }.returns(mediationMock)
        every { adUnitRendererMock.render(any(), any()) }.answers { }
        every { adUnitRendererMock.detachBannerImpression() }.answers { }
        sdkConfigurationRef.set(sdkConfigurationMock)
        every { sdkConfigurationMock.getPublisherDisable() } returns false
        every { rewardedMock.location } returns "test"
        every { appRequestMock.adUnit } returns AdUnit()
        every { backgroundServiceMock.execute(capture(runnableCaptor)) } just runs
    }

    @Test
    fun `cache but session not started cause publisher is disabled `() {
        every { sdkConfigurationMock.getPublisherDisable() } returns true
        every { rewardedCallbackMock.onAdLoaded(any(), any()) } just runs
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals("test", cacheEventCaptor.captured.ad.location)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, cacheErrorCaptor.captured.code)
    }

    @Test
    fun `cache but session not started cause sdk below Lollipop `() {
        every { rewardedCallbackMock.onAdLoaded(any(), any()) } just runs
        rewardedApiKitkat.cache(rewardedMock, rewardedCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals("test", cacheEventCaptor.captured.ad.location)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, cacheErrorCaptor.captured.code)
    }

    @Test
    fun `cache but session not started cause invalid location `() {
        every { rewardedMock.location } returns ""
        every { rewardedCallbackMock.onAdLoaded(any(), any()) } just runs
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals("", cacheEventCaptor.captured.ad.location)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, cacheErrorCaptor.captured.code)
    }

    @Test
    fun `cache valid ad`() {
        val backgroundRunnableCaptor = slot<Runnable>()
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        verify(exactly = 1) { backgroundServiceMock.execute(capture(backgroundRunnableCaptor)) }
        backgroundRunnableCaptor.captured.run()
        verify(exactly = 1) { adUnitLoaderMock.load("test", any(), any()) }
    }

    @Test
    fun `on cache success`() {
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        rewardedApi.onCacheSuccess("1111", TrackingEventName.Cache.START)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendCacheCallbackInMainThread(
                any(),
                eq(null),
                eq(rewardedMock),
                eq(rewardedCallbackMock),
            )
        }
    }

    @Test
    fun `don't clear cache when app request is missing`() {
        every { adUnitLoaderMock.getAppRequest() }.returns(null)
        rewardedApi.clearCache()
        verify(exactly = 0) { adUnitLoaderMock.removeAppRequest() }
    }

    @Test
    fun `clear cache when app request is valid`() {
        every { adUnitLoaderMock.getAppRequest() }.returns(appRequestMock)
        rewardedApi.clearCache()
        verify(exactly = 1) { adUnitLoaderMock.getAppRequest() }
        verify(exactly = 1) { adUnitLoaderMock.removeAppRequest() }
    }

    @Test
    fun `rewarded calls the callback onAdDismiss`() {
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        rewardedApi.onImpressionDismissed("test")
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendDismissCallbackOnMainThread(
                eq("test"),
                eq(rewardedMock),
                eq(rewardedCallbackMock),
            )
        }
    }

    @Test
    fun `on ad show failure after show success is not sent`() {
        every { sessionMock.sessionCounter } returns 1
        every { sessionMock.addSessionImpression(any()) } just runs
        every { sessionMock.getSessionImpressionsCounter(any<AdType>()) } returns 1
        every { sessionMock.getSessionImpressionsCounter(any<String>()) } returns 1

        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        rewardedApi.show(rewardedMock, rewardedCallbackMock)
        rewardedApi.onShowSuccess("test")
        rewardedApi.onShowFailure("test", CBError.Impression.ERROR_CREATING_VIEW)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendShowCallbackInMainThread(
                eq("test"),
                eq(null),
                eq(rewardedMock),
                eq(rewardedCallbackMock),
            )
        }
    }

    @Test
    fun `on ad show failure is sent`() {
        val showErrorSlot = CapturingSlot<ShowError>() // argumentCaptor<ShowError>()
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        rewardedApi.show(rewardedMock, rewardedCallbackMock)
        rewardedApi.onShowFailure("test", CBError.Impression.ERROR_CREATING_VIEW)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendShowCallbackInMainThread(
                eq("test"),
                capture(showErrorSlot),
                eq(rewardedMock),
                eq(rewardedCallbackMock),
            )
        }
        assertEquals(ShowError.Code.PRESENTATION_FAILURE, showErrorSlot.captured.code)
    }

    @Test
    fun `call on impression success`() {
        val impressionId = "test"
        rewardedApi.cache(rewardedMock, rewardedCallbackMock)
        rewardedApi.onImpressionSuccess(impressionId)
        verify(exactly = 1) {
            adApiCallbackSenderMock.sendImpressionRecordedCallbackOnMainThread(
                impressionId,
                rewardedMock,
                rewardedCallbackMock,
            )
        }
    }

    @Test
    fun `cache with bid response valid response`() {
        val bidResponseBase64 = "adajdjkd=="
        val bidResponseDecoded = "{ decoded }"
        val backgroundRunnableCaptor = slot<Runnable>()
        every { base64WrapperMock.decode(any()) } returns bidResponseDecoded
        rewardedApi.cache(rewardedMock, rewardedCallbackMock, bidResponseBase64)
        verify(exactly = 1) {
            backgroundServiceMock.execute(capture(backgroundRunnableCaptor))
        }
        backgroundRunnableCaptor.captured.run()
        verify(exactly = 1) { base64WrapperMock.decode(bidResponseBase64) }
        verify(exactly = 1) { adUnitLoaderMock.load("test", any(), bidResponseDecoded) }
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
        onCacheErrorTest(CBError.Impression.NETWORK_FAILURE, CacheError.Code.NETWORK_FAILURE)
        onCacheErrorTest(CBError.Impression.NO_AD_FOUND, CacheError.Code.NO_AD_FOUND)
        onCacheErrorTest(
            CBError.Impression.SESSION_NOT_STARTED,
            CacheError.Code.SESSION_NOT_STARTED,
        )
        onCacheErrorTest(CBError.Impression.INVALID_RESPONSE, CacheError.Code.SERVER_ERROR)
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
        onCacheErrorTest(CBError.Impression.INTERNAL, CacheError.Code.INTERNAL)
    }

    private fun onCacheErrorTest(
        impressionError: CBError.Impression,
        cacheErrorCode: CacheError.Code,
    ) {
        val localImpressionIdSlot = CapturingSlot<String>()
        val localCacheErrorSlot = CapturingSlot<CacheError>()
        val rewardedSlot = CapturingSlot<Rewarded>()
        val rewardedCallbackSlot = CapturingSlot<RewardedCallback>()
        val uiMock = mockk<FakeUiPoster>()
        every { uiMock.invoke(any()) } just runs

        val locaLambdaCaptor = slot<() -> Unit>()
        every { uiMock.invoke(capture(locaLambdaCaptor)) } just runs

        val localRewardedMock = mockk<Rewarded>()
        val localRewardedCallbackMock = mockk<RewardedCallback>()
        val localAdApiCallbackSenderMock = mockk<AdApiCallbackSender>()
        every {
            localAdApiCallbackSenderMock.sendCacheCallbackInMainThread(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every { localRewardedMock.location } returns "test"

        val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

        val api =
            RewardedApi(
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

        api.cache(localRewardedMock, localRewardedCallbackMock)
        api.onAdFailToLoad("error", impressionError)

        verify(exactly = 1) {
            localAdApiCallbackSenderMock.sendCacheCallbackInMainThread(
                capture(localImpressionIdSlot),
                capture(localCacheErrorSlot),
                capture(rewardedSlot),
                capture(rewardedCallbackSlot),
            )
        }
        assertNotNull(localCacheErrorSlot.captured)
        assertEquals(cacheErrorCode.errorCode, localCacheErrorSlot.captured.code.errorCode)
    }

//    @Throws(Exception::class)
//    fun setFinalStatic(field: Field, newValue: Any?) {
//        field.isAccessible = true
//        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
//        modifiersField.isAccessible = true
//        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
//        field.set(null, newValue)
//    }
}
