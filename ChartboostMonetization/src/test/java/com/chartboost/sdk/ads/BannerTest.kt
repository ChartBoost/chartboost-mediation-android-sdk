package com.chartboost.sdk.ads

import android.Manifest
import android.content.Context
import android.os.Build
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.DebugAwareRobolectricTestRunner
import com.chartboost.sdk.DebugOnlyTest
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.BannerCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import java.lang.Thread.sleep

@RunWith(DebugAwareRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class BannerTest {
    private val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    private val bannerCallbackMock = mockk<BannerCallback>()
    private val mediationMock = Mediation("mediation", "test", "test")
    private val banner = Banner(app, "test", Banner.BannerSize.STANDARD, bannerCallbackMock, mediationMock)

    @Before
    fun setup() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        Chartboost.startWithAppId(app, "test", "test") {}
        sleep(500)
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = false
    }

    @DebugOnlyTest
    @Test
    fun `cache when sdk not started`() {
        val eventCaptor = CapturingSlot<CacheEvent>()
        val errorCaptor = CapturingSlot<CacheError>()
        banner.cache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) { bannerCallbackMock.onAdLoaded(capture(eventCaptor), capture(errorCaptor)) }
        assertNotNull(eventCaptor.captured)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `cache with bid response when sdk not started`() {
        val eventCaptor = CapturingSlot<CacheEvent>()
        val errorCaptor = CapturingSlot<CacheError>()
        banner.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) { bannerCallbackMock.onAdLoaded(capture(eventCaptor), capture(errorCaptor)) }
        assertNotNull(eventCaptor.captured)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `cache when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        banner.cache()
        Robolectric.flushForegroundThreadScheduler()
        assertNotNull(banner.isCached())
    }

    @Test
    fun `cache with bid response when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        banner.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        assertNotNull(banner.isCached())
    }

    @Test
    fun `isCached when sdk is not started`() {
        assertFalse(banner.isCached())
    }

    @Test
    fun `isCached when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        // TODO need to figure out the mock of AdUnitManager
    }

    @Test
    fun `clearCache when sdk is not started`() {
        banner.clearCache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 0) { bannerCallbackMock.onAdLoaded(any(), any()) }
    }

    @Test
    fun `clearCache when sdk is started`() {
        // TODO cannot test that yet cause isCached is not implemented and cannot inject the adunit manager
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        banner.clearCache()
    }

    @Test
    fun `show when sdk is not started`() {
        val eventCaptor = CapturingSlot<ShowEvent>()
        val errorCaptor = CapturingSlot<ShowError>()
        banner.show()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) { bannerCallbackMock.onAdShown(capture(eventCaptor), capture(errorCaptor)) }
        assertNotNull(eventCaptor.captured)
        assertEquals(ShowError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `fill size banner standard`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        banner.show()
        Robolectric.flushForegroundThreadScheduler()
        assertEquals(320, banner.layoutParams.width)
        assertEquals(50, banner.layoutParams.height)
    }

    @Test
    fun `fill size banner leaderboard`() {
        val bannerLeaderboard = Banner(app, "test", Banner.BannerSize.LEADERBOARD, bannerCallbackMock, mediationMock)
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        bannerLeaderboard.show()
        Robolectric.flushForegroundThreadScheduler()
        assertEquals(728, bannerLeaderboard.layoutParams.width)
        assertEquals(90, bannerLeaderboard.layoutParams.height)
    }

    @Test
    fun `fill size banner medium`() {
        val bannerLeaderboard = Banner(app, "test", Banner.BannerSize.MEDIUM, bannerCallbackMock, mediationMock)
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        bannerLeaderboard.show()
        Robolectric.flushForegroundThreadScheduler()
        assertEquals(300, bannerLeaderboard.layoutParams.width)
        assertEquals(250, bannerLeaderboard.layoutParams.height)
    }
}
