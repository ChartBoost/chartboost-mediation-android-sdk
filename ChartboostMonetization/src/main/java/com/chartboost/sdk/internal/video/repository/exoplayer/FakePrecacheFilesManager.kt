package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.SandboxBridgeSettings
import java.io.File

// These files serve no business logic purposes, they are legacy from the older implementation
// with MediaPlayer but still serve for QA testing
internal class FakePrecacheFilesManager(
    private val fileCaching: ExoPlayerFileCaching,
) {
    fun downloadRemoved(download: DownloadWrapper) {
        if (SandboxBridgeSettings.isSandboxMode) {
            download.asPrecacheFile().delete()
            download.asPrecacheQueueFile().delete()
        }
    }

    fun downloadQueued(download: DownloadWrapper) {
        if (SandboxBridgeSettings.isSandboxMode) {
            download.asPrecacheQueueFile().createNewFile()
        }
    }

    fun downloadStarted(download: DownloadWrapper) {
        if (SandboxBridgeSettings.isSandboxMode) {
            download.asPrecacheQueueFile().delete()
            download.asPrecacheFile().createNewFile()
        }
    }

    private fun DownloadWrapper.asPrecacheFile(): File = asFile(fileCaching.precacheDirectory)

    private fun DownloadWrapper.asPrecacheQueueFile(): File = asFile(fileCaching.precacheQueueDirectory)
}
