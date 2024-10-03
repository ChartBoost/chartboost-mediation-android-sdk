package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.internal.video.VideoAsset
import com.google.android.exoplayer2.MediaItem

class ExoPlayerMediaItemFactory(
    private val downloadManager: ExoPlayerDownloadManager,
) {
    fun mediaItemFrom(asset: VideoAsset): MediaItem? = downloadManager.download(asset.filename)?.download?.request?.toMediaItem()
}
