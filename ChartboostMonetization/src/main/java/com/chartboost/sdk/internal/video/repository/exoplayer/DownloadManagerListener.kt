package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.logging.Logger
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager

internal class DownloadManagerListener(
    private val fakePrecacheFilesManager: FakePrecacheFilesManager,
) : DownloadManager.Listener {
    @Volatile
    private var listeners: List<VideoRequest.VideoRequestCallback> = emptyList()

    fun addListener(listener: VideoRequest.VideoRequestCallback) {
        listeners = listeners + listener
    }

    fun removeListener(listener: VideoRequest.VideoRequestCallback) {
        listeners = listeners - listener
    }

    private fun forEachListener(block: VideoRequest.VideoRequestCallback.() -> Unit) {
        listeners.forEach(block)
    }

    // This method should only be called by this class itself
    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?,
    ) {
        Logger.d(
            "onDownloadChanged() - id ${download.request.id}, " +
                "state: ${download.state.toDownloadState()}, " +
                "stopReason ${DownloadStopReason.fromInt(download.stopReason)}",
        )
        when (download.state) {
            Download.STATE_QUEUED,
            Download.STATE_STOPPED,
            -> fakePrecacheFilesManager.downloadQueued(download.asWrapper())
            Download.STATE_DOWNLOADING -> notifyTempFileIsReady(download.asWrapper())
            Download.STATE_COMPLETED -> notifyDownloadCompleted(download.asWrapper())
            Download.STATE_FAILED -> notifyDownloadFailed(download.asWrapper(), finalException)
            Download.STATE_REMOVING,
            Download.STATE_RESTARTING,
            -> { /* Nothing to do for these cases */ }
        }
    }

    private fun notifyDownloadFailed(
        download: DownloadWrapper,
        cause: Exception?,
    ) {
        val error = cause.toCBError()
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded failed ${download.uri} with error ${error.errorDesc}")
        forEachListener {
            onError(
                uri = download.uri,
                videoFileName = download.id,
                error = error,
            )
        }
    }

    // TODO Fix this with actual error type
    private fun Exception?.toCBError(): CBError =
        CBError(
            CBError.Internal.MISCELLANEOUS,
            "Error from Exoplayer DownloadManager",
        )

    private fun notifyDownloadCompleted(download: DownloadWrapper) {
        Logger.d("notifyDownloadCompleted() - download $download, listeners: $listeners")
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded success ${download.uri}")
        forEachListener {
            onSuccess(
                uri = download.uri,
                videoFileName = download.id,
            )
        }
    }

    private fun notifyTempFileIsReady(download: DownloadWrapper) {
        Logger.d("notifyTempFileIsReady() - download $download, listeners: $listeners")
        SandboxBridgeSettings.sendLogsToSandbox("Start downloading ${download.uri}")
        fakePrecacheFilesManager.downloadStarted(download)
        forEachListener {
            tempFileIsReady(
                url = download.uri,
                videoFileName = download.id,
                expectedContentSize = 0L,
                adUnitVideoPrecacheTempCallback = null,
            )
        }
    }
}
