/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import android.util.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BannerAdDimensions(
    @SerialName("w")
    val width: Int,
    @SerialName("h")
    val height: Int,
) {
    internal constructor(size: Size) : this(size.width, size.height)
}
