package com.chartboost.sdk.internal.video.player

interface AdsVideoPlayerListener {
    fun onVideoDisplayStarted()

    fun onVideoDisplayPrepared(duration: Long)

    fun onVideoDisplayProgress(position: Long)

    fun onVideoDisplayCompleted()

    fun onVideoDisplayError(error: String)

    fun removeAssetOnError()

    fun onVideoBufferStart()

    fun onVideoBufferFinish()
}
