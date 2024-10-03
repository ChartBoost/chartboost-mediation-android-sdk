package com.chartboost.sdk.internal.video.player.scheduler

import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener

internal typealias VideoProgressSchedulerFactory = (
    AdsVideoPlayerListener?,
    VideoProgressScheduler.VideoProgress,
    UiPoster,
) -> VideoProgressScheduler

internal interface VideoProgressScheduler {
    fun startProgressUpdate(
        @androidx.annotation.IntRange(from = MIN_PROGRESS_INTERVAL, to = MAX_PROGRESS_INTERVAL)
        progressIntervalMillis: Long = DEFAULT_PROGRESS_INTERVAL,
    )

    fun stopProgressUpdate()

    fun interface VideoProgress {
        fun currentPosition(): Long
    }

    companion object {
        const val DEFAULT_PROGRESS_INTERVAL: Long = 500
        const val MIN_PROGRESS_INTERVAL: Long = 100
        const val MAX_PROGRESS_INTERVAL: Long = 1000
    }
}
