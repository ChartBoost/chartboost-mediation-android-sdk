package com.chartboost.sdk.internal.Networking.requests.models

import com.chartboost.sdk.internal.Model.ImpressionMediaType

internal data class ClickParams(
    val location: String,
    val adId: String,
    val to: String,
    val cgn: String,
    val creative: String,
    val videoPosition: Float?,
    val videoDuration: Float?,
    val impressionMediaType: ImpressionMediaType,
    val retargetReinstall: Boolean?,
)
