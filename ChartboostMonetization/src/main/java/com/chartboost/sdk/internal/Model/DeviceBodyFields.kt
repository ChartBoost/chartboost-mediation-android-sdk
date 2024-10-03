package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.internal.utils.DeviceInfo

data class DeviceBodyFields(
    val deviceWidth: Int = 0,
    val deviceHeight: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val scale: Float = 0f,
    val dpi: String? = "",
    val ortbDeviceType: Int = DeviceInfo.OPENRTB_DEVICE_PHONE,
    val deviceType: String = "phone",
    val packageName: String? = null,
    val versionName: String? = null,
    val isPortrait: Boolean = true,
)
