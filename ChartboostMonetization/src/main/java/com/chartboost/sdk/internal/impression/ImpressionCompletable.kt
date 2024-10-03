package com.chartboost.sdk.internal.impression

interface ImpressionCompletable {
    fun sendVideoCompleteRequest(
        location: String,
        videoPosition: Float?,
        videoDuration: Float?,
    )

    fun notifyDidCompleteAd()
}
