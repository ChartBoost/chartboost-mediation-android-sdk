package com.chartboost.sdk.internal.Model

import android.content.Context
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.View.isScreenPortrait
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.DeviceInfo

internal class DeviceBodyFieldsFactory(
    private val context: Context,
    private val displayMeasurement: DisplayMeasurement,
    private val deviceFieldsWrapper: DeviceFieldsWrapper,
) {
    fun build(): DeviceBodyFields {
        return try {
            val deviceSize = displayMeasurement.getDeviceSize()
            val size = displayMeasurement.getSize()
            val packageName = context.packageName

            return DeviceBodyFields(
                deviceWidth = deviceSize.width,
                deviceHeight = deviceSize.height,
                width = size.width,
                height = size.height,
                scale = displayMeasurement.displayMetricsDensity,
                dpi = displayMeasurement.displayMetricsDensityDpi.toString(),
                ortbDeviceType = deviceFieldsWrapper.getOpenRTBDeviceType(),
                deviceType = deviceFieldsWrapper.getType(),
                packageName = packageName,
                versionName = context.packageManager.getPackageVersionName(packageName),
                isPortrait = deviceFieldsWrapper.isPortrait(),
            )
        } catch (e: Exception) {
            Logger.e("Cannot create device body", e)
            DeviceBodyFields()
        }
    }
}

internal class DeviceFieldsWrapper(
    private val context: Context,
    private val displayMeasurement: DisplayMeasurement,
) {
    fun getOpenRTBDeviceType(): Int = DeviceInfo.getOpenRTBDeviceType(context)

    fun getType(): String = DeviceInfo.getType(context)

    fun isPortrait(): Boolean {
        return context.isScreenPortrait(displayMeasurement)
    }
}
