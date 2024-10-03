package com.chartboost.sdk.view

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import com.chartboost.sdk.internal.AdUnitManager.render.RendererActivityBridge
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.SdkConfiguration
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ImpressionActivityPresenterTest {
    private val activityMock = mockk<CBImpressionActivity>()
    private val resourcesMock = mockk<Resources>()
    private val configurationMock = mockk<Configuration>()
    private val sdkConfigurationMock = mockk<SdkConfiguration>()
    private val displayMeasurementMock = mockk<DisplayMeasurement>()
    private val rendererActivityBridgeMock = mockk<RendererActivityBridge>()
    private val viewMock = mockk<ImpressionActivityContract.ImpressionActivityView>()
    private val presenter =
        ImpressionActivityPresenter(
            viewMock,
            rendererActivityBridgeMock,
            sdkConfigurationMock,
            displayMeasurementMock,
        )

    @Before
    fun setup() {
        every { rendererActivityBridgeMock.setActivityRendererInterface(any(), any()) } just Runs
        every { rendererActivityBridgeMock.onStart() } just Runs
        every { rendererActivityBridgeMock.onResume() } just Runs
        every { rendererActivityBridgeMock.onPause() } just Runs
        every { rendererActivityBridgeMock.onDestroy() } just Runs
        every { rendererActivityBridgeMock.finishActivity() } just Runs
        every { rendererActivityBridgeMock.failure(any()) } just Runs
        every { rendererActivityBridgeMock.onConfigurationChange() } just Runs
        every { rendererActivityBridgeMock.onBackPressed() } returns true
        every { viewMock.setFullscreen() }.answers { }
        every { viewMock.getActivity() }.returns(activityMock)
        every { viewMock.finishActivity() }.answers { }
        every { viewMock.isActivityHardwareAccelerated() }.returns(true)
        configurationMock.orientation = ORIENTATION_PORTRAIT
        every { resourcesMock.configuration } returns configurationMock
        every { activityMock.resources } returns resourcesMock
        every { activityMock.requestedOrientation }.returns(1)
        every { activityMock.requestedOrientation = any() } just Runs
    }

    @Test
    fun `on create`() {
        presenter.onCreate()
        verify(exactly = 1) { viewMock.setFullscreen() }
        verify(exactly = 1) {
            rendererActivityBridgeMock.setActivityRendererInterface(
                presenter,
                activityMock,
            )
        }
        verify(exactly = 2) { viewMock.getActivity() }
        verify(exactly = 1) { activityMock.requestedOrientation }
    }

    @Test
    fun `on start`() {
        presenter.onStart()
        verify(exactly = 1) { rendererActivityBridgeMock.onStart() }
    }

    @Test
    fun `on resume`() {
        presenter.onResume()
        verify(exactly = 1) { viewMock.setFullscreen() }
        verify(exactly = 1) { rendererActivityBridgeMock.onResume() }
    }

    @Test
    fun `on pause`() {
        presenter.onPause()
        verify(exactly = 1) { rendererActivityBridgeMock.onPause() }
    }

    @Test
    fun `on destroy`() {
        presenter.onDestroy()
        verify(exactly = 1) { rendererActivityBridgeMock.onDestroy() }
    }

    @Test
    fun `on back button pressed`() {
        presenter.onBackPressed()
        verify(exactly = 1) { rendererActivityBridgeMock.onBackPressed() }
    }

    @Test
    fun `on configuration change`() {
        presenter.onConfigurationChange()
        verify(exactly = 1) { rendererActivityBridgeMock.onConfigurationChange() }
    }

    @Test
    fun `on view attached no hardware acceleration`() {
        every { viewMock.isActivityHardwareAccelerated() }.returns(false)
        presenter.onViewAttached()
        verify(exactly = 1) {
            rendererActivityBridgeMock.failure(
                CBError.Impression.HARDWARE_ACCELERATION_DISABLED,
            )
        }
        verify(exactly = 1) { viewMock.finishActivity() }
    }

    @Test
    fun `on view attached with hardware acceleration`() {
        presenter.onViewAttached()
        verify(exactly = 0) {
            rendererActivityBridgeMock.failure(
                CBError.Impression.HARDWARE_ACCELERATION_DISABLED,
            )
        }
        verify(exactly = 0) { viewMock.finishActivity() }
    }

    @Test
    fun `apply orientation properties portrait`() {
        val forceOrientation = 1
        val allowOrientationChange = true
        presenter.applyOrientationProperties(forceOrientation, allowOrientationChange)
        verify(exactly = 1) { activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    @Test
    fun `apply orientation properties landscape`() {
        val forceOrientation = 0
        val allowOrientationChange = true
        presenter.applyOrientationProperties(forceOrientation, allowOrientationChange)
        verify(exactly = 1) { activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
    }

    @Test
    fun `apply orientation properties force other with allowOrientationChange true`() {
        val forceOrientation = 2
        val allowOrientationChange = true
        presenter.applyOrientationProperties(forceOrientation, allowOrientationChange)
        verify(exactly = 1) { activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    @Test
    fun `apply orientation properties force other with allowOrientationChange false`() {
        val forceOrientation = 2
        val allowOrientationChange = false
        presenter.applyOrientationProperties(forceOrientation, allowOrientationChange)
        verify(exactly = 1) { activityMock.requestedOrientation = ORIENTATION_PORTRAIT }
    }

    @Test
    fun `restore original orientation`() {
        presenter.restoreOriginalOrientation()
        verify(exactly = 1) { viewMock.getActivity() }
        verify(exactly = 1) { activityMock.requestedOrientation = -1 }
    }
}
