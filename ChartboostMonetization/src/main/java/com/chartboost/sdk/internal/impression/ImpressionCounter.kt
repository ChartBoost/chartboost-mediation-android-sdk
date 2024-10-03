package com.chartboost.sdk.internal.impression

internal data class ImpressionCounter(
    var onVideoCompletedPlayCount: Int = 1,
    var onRewardedVideoCompletedPlayCount: Int = 1,
    var impressionNotifyDidCompleteAdPlayCount: Int = 1,
    var impressionSendVideoCompleteRequestPlayCount: Int = 1,
)
