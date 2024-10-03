package com.chartboost.sdk.internal.Libraries

import android.graphics.Insets
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class DisplayMeasurementTest {
    private val androidVersionLowMock: () -> Int =
        mockk<() -> Int>().apply {
            every { this@apply.invoke() } returns Build.VERSION_CODES.LOLLIPOP
        }

    private val androidVersionHighMock: () -> Int =
        mockk<() -> Int>().apply {
            every { this@apply.invoke() } returns Build.VERSION_CODES.R
        }

    private val windowManagerMock = mockk<WindowManager>()
    private val realDisplayMetricsMock = mockk<DisplayMetrics>()

    private lateinit var displayMeasurementLow: DisplayMeasurement
    private lateinit var displayMeasurementHigh: DisplayMeasurement
    private val displayMetrics = DisplayMetrics()

    @Before
    fun setup() {
        displayMetrics.heightPixels = 1
        displayMetrics.widthPixels = 2
        displayMetrics.density = 1f
        displayMetrics.densityDpi = 1

        displayMeasurementLow =
            DisplayMeasurement(
                windowManagerMock,
                displayMetrics,
                androidVersionLowMock,
                realDisplayMetricsMock,
            )

        displayMeasurementHigh =
            DisplayMeasurement(
                windowManagerMock,
                displayMetrics,
                androidVersionHighMock,
            )
    }

    @Test
    fun `get size from display metrics`() {
        realDisplayMetricsMock.heightPixels = 3
        realDisplayMetricsMock.widthPixels = 4
        val displayMock = mockk<Display>()
        every { displayMock.getRealMetrics(any()) } just Runs
        every { windowManagerMock.defaultDisplay } returns displayMock
        every { realDisplayMetricsMock.setTo(displayMetrics) } just Runs
        val size = displayMeasurementLow.getSize()
        assertEquals(size.height, 3)
        assertEquals(size.width, 4)
    }

    @Test
    fun `get size from current window manager`() {
        // TODO This doesn't work somehow currentWindowMetrics crashes silently in the unit tests
        val rect = Rect()
        rect.set(0, 0, 1, 1)
        val windowMetricsMock = mockk<WindowMetrics>()
        every { windowMetricsMock.bounds } returns rect
        every { windowManagerMock.currentWindowMetrics } returns windowMetricsMock
        val size = displayMeasurementHigh.getSize()
        assertEquals(size.height, 0)
        assertEquals(size.width, 0)
    }

    @Test
    fun `get device size from display metrics`() {
        val size = displayMeasurementLow.getDeviceSize()
        assertEquals(size.height, 1)
        assertEquals(size.width, 2)
    }

    @Test
    fun `get device size from window manager`() {
        // TODO This doesn't work somehow currentWindowMetrics crashes silently in the unit tests
        val insetsMock = mockk<Insets>()
        val windowInsetsMock = mockk<WindowInsets>()
        val windowMetricsMock = mockk<WindowMetrics>()

        every { windowInsetsMock.getInsetsIgnoringVisibility(any()) } returns insetsMock
        every { windowMetricsMock.windowInsets } returns windowInsetsMock
        every { windowManagerMock.currentWindowMetrics } returns windowMetricsMock

        val size = displayMeasurementHigh.getDeviceSize()
        assertEquals(size.height, 0)
        assertEquals(size.width, 0)
    }

    @Test
    fun `get display metrics density`() {
        val density = displayMeasurementLow.displayMetricsDensity
        assertEquals(density, 1f)
    }

    @Test
    fun `get display metrics density dpi`() {
        val densityDpi = displayMeasurementLow.displayMetricsDensityDpi
        assertEquals(densityDpi, 1)
    }
}
