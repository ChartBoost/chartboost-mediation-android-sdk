package com.chartboost.sdk.view

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Libraries.DisplaySize
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.View.getOrientationAsString
import com.chartboost.sdk.internal.View.isScreenPortrait
import com.chartboost.sdk.internal.View.lockOrientation
import com.chartboost.sdk.internal.View.unlockOrientation
import com.chartboost.sdk.test.relaxedMockk
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class OrientationHelperTest {
    private val displayMock = mockk<Display>()

    private val windowManagerMock =
        mockk<WindowManager> {
            every { defaultDisplay } returns displayMock
        }

    private val activityMock =
        mockk<Activity> {
            every { requestedOrientation = any() } just Runs
            every { getSystemService(Context.WINDOW_SERVICE) } returns windowManagerMock
        }

    private val sdkConfigurationMock =
        relaxedMockk<SdkConfiguration> {
            every { isWebviewEnabled } returns true
            every { isWebviewLockOrientation } returns true
        }

    private val displayMeasurementMock = mockk<DisplayMeasurement>()

    @Test
    fun `is orientation portrait`() {
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeTrue()
        every { displayMock.rotation } returns Surface.ROTATION_90
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeTrue()
        every { displayMock.rotation } returns Surface.ROTATION_180
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeTrue()
        every { displayMock.rotation } returns Surface.ROTATION_270
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeTrue()
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeFalse()
        every { displayMock.rotation } returns Surface.ROTATION_90
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeFalse()
        every { displayMock.rotation } returns Surface.ROTATION_180
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeFalse()
        every { displayMock.rotation } returns Surface.ROTATION_270
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.isScreenPortrait(displayMeasurementMock).shouldBeFalse()
    }

    @Test
    fun `unlock orientation`() {
        activityMock.unlockOrientation(sdkConfigurationMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_PORTRAIT rotation 0`() {
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_PORTRAIT rotation 270`() {
        every { displayMock.rotation } returns Surface.ROTATION_270
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_REVERSE_PORTRAIT rotation 90`() {
        every { displayMock.rotation } returns Surface.ROTATION_90
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_REVERSE_PORTRAIT rotation 180`() {
        every { displayMock.rotation } returns Surface.ROTATION_180
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_LANDSCAPE rotation 0`() {
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_LANDSCAPE rotation 90`() {
        every { displayMock.rotation } returns Surface.ROTATION_90
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_REVERSE_LANDSCAPE`() {
        every { displayMock.rotation } returns Surface.ROTATION_180
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }

    @Test
    fun `lock orientation with SCREEN_ORIENTATION_LANDSCAPE rotation 270`() {
        every { displayMock.rotation } returns Surface.ROTATION_270
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.lockOrientation(sdkConfigurationMock, displayMeasurementMock)
        verify(exactly = 1) {
            activityMock.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }

    @Test
    fun `get orientation portrait as string`() {
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(50, 100)
        activityMock.getOrientationAsString(displayMeasurementMock).shouldBe("portrait")
    }

    @Test
    fun `get orientation landscape as string`() {
        every { displayMock.rotation } returns Surface.ROTATION_0
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 50)
        activityMock.getOrientationAsString(displayMeasurementMock).shouldBe("landscape")
    }
}
