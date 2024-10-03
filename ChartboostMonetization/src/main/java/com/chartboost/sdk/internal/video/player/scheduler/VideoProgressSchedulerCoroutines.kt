package com.chartboost.sdk.internal.video.player.scheduler

import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class VideoProgressSchedulerCoroutines(
    private val callback: AdsVideoPlayerListener? = null,
    private val videoProgress: VideoProgressScheduler.VideoProgress,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : VideoProgressScheduler {
    private var progressJob: Job? = null

    override fun startProgressUpdate(progressIntervalMillis: Long) {
        Logger.d("startProgressUpdate()")
        // Avoid creating a new job if there's one running already
        if (progressJob != null) return
        progressJob =
            CoroutineScope(coroutineDispatcher).launch {
                while (true) {
                    delay(progressIntervalMillis)
                    callback?.onVideoDisplayProgress(videoProgress.currentPosition())
                }
            }
    }

    override fun stopProgressUpdate() {
        Logger.d("stopProgressUpdate()")
        progressJob?.cancel()
        progressJob = null
    }
}
