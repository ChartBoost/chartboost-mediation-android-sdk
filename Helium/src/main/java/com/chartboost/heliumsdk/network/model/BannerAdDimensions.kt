package com.chartboost.heliumsdk.network.model

import android.util.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BannerAdDimensions(
    @SerialName("w")
    val width: Int,

    @SerialName("h")
    val height: Int
) {
    internal constructor(size: Size): this(size.width, size.height)
}
