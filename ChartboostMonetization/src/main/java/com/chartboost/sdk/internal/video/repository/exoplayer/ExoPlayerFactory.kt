package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.source.MediaSource

internal class ExoPlayerFactory(
    context: Context,
    downloadManager: ExoPlayerDownloadManager =
        ChartboostDependencyContainer.applicationComponent.exoPlayerDownloadManager,
    private val mediaSourceFactory: () -> MediaSource.Factory =
        { downloadManager.dataSourceFactory().toMediaSource() },
    private val loadControlFactory: () -> LoadControl = { loadControl() },
) {
    private val context: Context = context.applicationContext

    operator fun invoke(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory())
            .setLoadControl(loadControlFactory())
            .build()
}
