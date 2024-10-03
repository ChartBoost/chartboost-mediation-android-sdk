package com.chartboost.sdk.internal.video.repository

import android.content.Context
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.VideoAsset

internal typealias DownloadState = Int

interface VideoRepository {
    fun initialize(context: Context)

    fun downloadVideoFile(
        url: String,
        filename: String,
        showImmediately: Boolean,
        callback: AdUnitVideoPrecacheTemp?,
    )

    // This function should not be exposed, there's already a downloadVideoFile() function, seems redundant
    fun startDownloadIfPossible(
        filename: String? = null,
        repeat: Int = 0,
        forceDownload: Boolean = false,
    )

    // Why does any external class need to know this?
    fun isFileDownloadingOrDownloaded(videoFilename: String): Boolean

    // Why does any external class need a video asset?
    fun getVideoAsset(filename: String): VideoAsset?

    // Suspicious public API, unless we want to show download state anywhere
    fun getVideoDownloadState(asset: VideoAsset?): DownloadState

    // Should be handled automatically by the repository
    fun removeAsset(videoAsset: VideoAsset?): Boolean

    companion object {
        const val VIDEO_STATE_FULL = 5 // 100%
        const val VIDEO_STATE_QUARTILE_4 = 4 // 76-99%
        const val VIDEO_STATE_QUARTILE_3 = 3 // 51-75%
        const val VIDEO_STATE_QUARTILE_2 = 2 // 26-50%
        const val VIDEO_STATE_QUARTILE_1 = 1 // 1-25%
        const val VIDEO_STATE_EMPTY = 0 // 0%
    }
}
