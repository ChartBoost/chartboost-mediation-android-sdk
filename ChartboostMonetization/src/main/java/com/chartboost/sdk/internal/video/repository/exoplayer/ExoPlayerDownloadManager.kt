package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.video.VideoAsset
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DataSource

// Percentage is a Float between 0 and 1
internal typealias Percentage = Float

interface ExoPlayerDownloadManager {
    fun getDownloadManager(): DownloadManager

    fun initialize()

    fun addListener(listener: VideoRequest.VideoRequestCallback)

    fun removeListener(listener: VideoRequest.VideoRequestCallback)

    fun removeDownload(id: String)

    fun cleanDownloads()

    fun isDownloadingOrDownloaded(id: String): Boolean

    fun addDownload(
        asset: VideoAsset,
        stopReason: DownloadStopReason = DownloadStopReason.NONE,
    )

    fun startDownload(asset: VideoAsset)

    fun downloadPercentage(id: String): Percentage

    fun dataSourceFactory(): DataSource.Factory

    fun download(id: String): DownloadWrapper?

    fun startNextDownload(currentDownloadStopReason: DownloadStopReason = DownloadStopReason.NONE)
}
