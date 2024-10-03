package com.chartboost.sdk.ads

import android.Manifest
import android.content.Context
import android.os.Build
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.InterstitialCallback
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
class InterstitialTest {
    private val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    private val interstitialCallbackMock = mockk<InterstitialCallback>()
    private val mediationMock = Mediation("mediation", "test", "test")
    private val interstitial = Interstitial("test", interstitialCallbackMock, mediationMock)

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
        interstitial.cache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            interstitialCallbackMock.onAdLoaded(
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
        interstitial.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            interstitialCallbackMock.onAdLoaded(
                capture(eventCaptor),
                capture(errorCaptor),
            )
        }
        assertNotNull(eventCaptor.captured)
        assertEquals(CacheError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }

    @Test
    fun `cache when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        interstitial.cache()
        Robolectric.flushForegroundThreadScheduler()
        assertNotNull(interstitial.isCached())
    }

    @Test
    fun `cache with bid response when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        interstitial.cache("bid response")
        Robolectric.flushForegroundThreadScheduler()
        assertNotNull(interstitial.isCached())
    }

    @Test
    fun `isCached when sdk is not started`() {
        assertFalse(interstitial.isCached())
    }

    @Test
    fun `isCached when sdk is started`() {
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        // TODO need to figure out the mock of AdUnitManager
    }

    @Test
    fun `clearCache when sdk is not started`() {
        interstitial.clearCache()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 0) { interstitialCallbackMock.onAdLoaded(any(), any()) }
    }

    @Test
    fun `show when sdk is not started`() {
        val eventCaptor = CapturingSlot<ShowEvent>()
        val errorCaptor = CapturingSlot<ShowError>()
        interstitial.show()
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) {
            interstitialCallbackMock.onAdShown(
                capture(eventCaptor),
                capture(errorCaptor),
            )
        }
        assertNotNull(eventCaptor.captured)
        assertEquals(ShowError.Code.SESSION_NOT_STARTED, errorCaptor.captured.code)
    }
}
