package com.chartboost.sdk.internal.video.player

import com.chartboost.sdk.internal.video.VideoAsset

interface AdsVideoPlayer : ScreenOrientationChangeListener {
    fun asset(asset: VideoAsset)

    fun play()

    fun pause()

    fun stop()

    fun mute()

    fun unmute()

    fun wasMediaStartedForTheFirstTime(): Boolean

    fun volume(): Float
}
