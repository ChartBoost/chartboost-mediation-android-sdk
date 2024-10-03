package com.chartboost.sdk.internal.video.repository.exoplayer

import com.google.android.exoplayer2.offline.Download
import java.io.File

data class DownloadWrapper(val download: Download) {
    val uri: String get() = download.request.uri.toString()
    val id: String get() = download.request.id
    val state: Int get() = download.state
    val updateTime: Long get() = download.updateTimeMs
    val percentDownloaded: Float get() = download.percentDownloaded
}

internal fun Download.asWrapper(): DownloadWrapper = DownloadWrapper(this)

internal fun Int.toDownloadState(): String =
    when (this) {
        Download.STATE_DOWNLOADING -> "STATE_DOWNLOADING"
        Download.STATE_COMPLETED -> "STATE_COMPLETED"
        Download.STATE_FAILED -> "STATE_FAILED"
        Download.STATE_QUEUED -> "STATE_QUEUED"
        Download.STATE_REMOVING -> "STATE_REMOVING"
        Download.STATE_RESTARTING -> "STATE_RESTARTING"
        Download.STATE_STOPPED -> "STATE_STOPPED"
        else -> "UNKNOWN STATE $this"
    }

internal fun DownloadWrapper.asFile(parentDirectory: File?): File = File(parentDirectory, id)
