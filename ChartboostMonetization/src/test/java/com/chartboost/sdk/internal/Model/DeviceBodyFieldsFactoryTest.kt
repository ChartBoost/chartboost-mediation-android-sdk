package com.chartboost.sdk.internal.Model

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Libraries.DisplaySize
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class DeviceBodyFieldsFactoryTest {
    private val contextMock = mockk<Context>()
    private val displayMeasurementMock = mockk<DisplayMeasurement>()
    private val deviceFieldsWrapperMock = mockk<DeviceFieldsWrapper>()
    private val packageManagerMock = mockk<PackageManager>()
    private val packageInfoMock = PackageInfo()

    private val deviceBodyFieldsFactory =
        DeviceBodyFieldsFactory(
            contextMock,
            displayMeasurementMock,
            deviceFieldsWrapperMock,
        )

    @Test
    fun `build device body fields`() {
        packageInfoMock.versionName = "1.0"
        every { packageManagerMock.getPackageInfo("package.test", 128) } returns packageInfoMock
        every { contextMock.packageName } returns "package.test"
        every { contextMock.packageManager } returns packageManagerMock
        every { deviceFieldsWrapperMock.getType() } returns "type"
        every { deviceFieldsWrapperMock.isPortrait() } returns true
        every { deviceFieldsWrapperMock.getOpenRTBDeviceType() } returns 1
        every { displayMeasurementMock.displayMetricsDensity } returns 1f
        every { displayMeasurementMock.displayMetricsDensityDpi } returns 10
        every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(1, 1)
        every { displayMeasurementMock.getSize() } returns DisplaySize(2, 2)

        val fields = deviceBodyFieldsFactory.build()

        assertEquals(fields.deviceHeight, 1)
        assertEquals(fields.deviceWidth, 1)
        assertEquals(fields.width, 2)
        assertEquals(fields.height, 2)
        assertEquals(fields.dpi, "10")
        assertEquals(fields.scale, 1f)
        assertEquals(fields.deviceType, "type")
        assertEquals(fields.isPortrait, true)
        assertEquals(fields.ortbDeviceType, 1)
        assertEquals(fields.packageName, "package.test")
        assertEquals(fields.versionName, "1.0")
    }
}
