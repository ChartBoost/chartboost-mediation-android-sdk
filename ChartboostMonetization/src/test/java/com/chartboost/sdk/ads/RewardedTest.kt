package com.chartboost.sdk.ads

import android.Manifest
import android.content.Context
import android.os.Build
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.RewardedCallback
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import java.lang.Thread.sleep

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RewardedTest {
    private val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    private val rewardedCallbackMock = mockk<RewardedCallback>()
    private val mediationMock = Mediation("mediation", "test", "test")
    private val rewarded = Rewarded("test", rewardedCallbackMock, mediationMock)

    @Before
    fun setup() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        Chartboost.startWithAppId(app, "test", "test") {}
        sleep(500)
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = false
    }

    @Test
    fun `cache when sdk not started`() {
        val eventCaptor = CapturingSlot<CacheEvent>()
        val errorCaptor = CapturingSlot<CacheError>()
        rewarded.cache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdLoaded(
                capture(eventCaptor),
                capture(errorCaptor),
            )
        }
        assertNotNull(eventCaptor.captured)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `cache with bid response when sdk not started`() {
        val eventCaptor = CapturingSlot<CacheEvent>()
        val errorCaptor = CapturingSlot<CacheError>()
        rewarded.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdLoaded(
                capture(eventCaptor),
                capture(errorCaptor),
            )
        }
        assertNotNull(eventCaptor.captured)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `cache when sdk is started`() {
        // TODO need to figure out the mock of AdUnitManager
    }

    @Test
    fun `cache with bid response when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        rewarded.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        assertNotNull(rewarded.isCached())
    }

    @Test
    fun `isCached when sdk is not started`() {
        assertFalse(rewarded.isCached())
    }

    @Test
    fun `isCached when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        // TODO need to figure out the mock of AdUnitManager
    }

    @Test
    fun `clearCache when sdk is not started`() {
        rewarded.clearCache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 0) { rewardedCallbackMock.onAdLoaded(any(), any()) }
    }

    @Test
    fun `show when sdk is not started`() {
        val eventCaptor = CapturingSlot<ShowEvent>()
        val errorCaptor = CapturingSlot<ShowError>()
        rewarded.show()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            rewardedCallbackMock.onAdShown(
                capture(eventCaptor),
                capture(errorCaptor),
            )
        }
        assertNotNull(eventCaptor.captured)
        assertEquals(ShowError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }
}
