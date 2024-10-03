package com.chartboost.sdk.internal.video.repository.exoplayer

import android.net.Uri
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.errorMessage
import com.chartboost.sdk.internal.video.VideoAsset
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.upstream.DataSource
import java.io.IOException

private typealias ExoPlayerDownloadState = Int
private typealias Url = String

internal class ExoPlayerDownloadManagerImpl(
    private val dependencies: ExoPlayerDownloadManagerDependencies = ExoPlayerDownloadManagerDependencies(),
) : ExoPlayerDownloadManager,
    DownloadManager.Listener,
    ChartboostCacheEvictor.EvictUrlCallback {
    private lateinit var downloadManager: DownloadManager
    private lateinit var cacheDataSourceFactory: DataSource.Factory
    private lateinit var fileCaching: ExoPlayerFileCaching

    // Creates fake precache files for QA
    private lateinit var fakePrecacheFilesManager: FakePrecacheFilesManager

    @Volatile
    private var listeners: List<VideoRequest.VideoRequestCallback> = emptyList()

    // Stores latest state sent to each listener to avoid sending the same state several times
    @Volatile
    private var latestDownloadStateSent: Map<Url, ExoPlayerDownloadState> = emptyMap()

    override fun addListener(listener: VideoRequest.VideoRequestCallback) {
        listeners = listeners + listener
    }

    override fun removeListener(listener: VideoRequest.VideoRequestCallback) {
        listeners = listeners - listener
    }

    private fun forEachListener(
        state: ExoPlayerDownloadState,
        url: Url,
        block: VideoRequest.VideoRequestCallback.() -> Unit,
    ) {
        listeners.forEach {
            // Avoid calling the callback several times for the same status
            // ExoPlayer sends several updates for the same download
            if (latestDownloadStateSent[url] != state) {
                latestDownloadStateSent = latestDownloadStateSent + (url to state)
                it.block()
            }
        }
    }

    override fun getDownloadManager(): DownloadManager {
        if (!::downloadManager.isInitialized) {
            val databaseProvider = dependencies.databaseProviderFactory(dependencies.context)
            fileCaching = dependencies.fileCachingFactory(dependencies.context)
            val cache =
                dependencies.cacheFactory(
                    fileCaching,
                    dependencies.videoCachePolicy,
                    databaseProvider,
                    this@ExoPlayerDownloadManagerImpl,
                )
            cacheDataSourceFactory =
                dependencies.cacheDataSourceFactoryFactory(
                    cache,
                    dependencies.httpDataSourceFactory,
                )
            fakePrecacheFilesManager = dependencies.fakePrecacheFilesManagerFactory(fileCaching)
            downloadManager =
                dependencies.downloadManagerFactory(
                    dependencies.context,
                    databaseProvider,
                    cache,
                    dependencies.httpDataSourceFactory,
                    this@ExoPlayerDownloadManagerImpl,
                )
        }
        return downloadManager
    }

    @Synchronized
    override fun initialize() {
        Logger.d("initialize()")
        dependencies.setCookieHandler()
        getDownloadManager()
    }

    override fun removeDownload(id: String) {
        Logger.d("removeDownload() - id: $id")
        getDownloadManager().download(id)?.remove()
    }

    override fun cleanDownloads() {
        getDownloadManager()
            .downloads()
            .cleanDownloadsByTTL()
    }

    private fun List<DownloadWrapper>.cleanDownloadsByTTL(): List<DownloadWrapper> =
        apply {
            filter { it.isTimeToLiveExpired() }.removeAll()
        }

    override fun isDownloadingOrDownloaded(id: String): Boolean =
        download(id)
            ?.run { state == Download.STATE_COMPLETED || state == Download.STATE_DOWNLOADING }
            ?: false

    override fun addDownload(
        asset: VideoAsset,
        stopReason: DownloadStopReason,
    ) {
        Logger.d("addDownload() - asset: $asset, stopReason $stopReason")
        asset.sendAddDownload(stopReason)
    }

    override fun startDownload(asset: VideoAsset) {
        Logger.d("startDownload() - asset: $asset")
        with(asset) {
            clearLastState()
            stopAllDownloadsExceptThisOne()
            sendAddDownload()
        }
    }

    private fun VideoAsset.clearLastState() {
        latestDownloadStateSent = latestDownloadStateSent - url
    }

    private fun VideoAsset.stopAllDownloadsExceptThisOne() {
        getDownloadManager().downloads().forEach { download ->
            if (download.id != filename) {
                download.sendStopReason(DownloadStopReason.FORCED_OUT)
            }
        }
    }

    // percentDownloaded returns a float between 0 and 100, we need to convert it to per one
    override fun downloadPercentage(id: String): Percentage = (download(id)?.percentDownloaded ?: 0f) / 100f

    override fun dataSourceFactory(): DataSource.Factory = cacheDataSourceFactory

    override fun download(id: String): DownloadWrapper? = getDownloadManager().download(id)

    override fun startNextDownload(currentDownloadStopReason: DownloadStopReason) {
        getDownloadManager().currentDownloads
            .firstOrNull()
            ?.asWrapper()
            ?.sendStopReason(currentDownloadStopReason)
    }

    // Cannot be unit tested since Download is final and cannot be mocked
    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?,
    ) {
        Logger.d(
            "onDownloadChanged() - state ${download.state.toDownloadState()}, finalException $finalException",
        )
        when (download.state) {
            Download.STATE_QUEUED,
            Download.STATE_STOPPED,
            -> fakePrecacheFilesManager.downloadQueued(download.asWrapper())

            Download.STATE_DOWNLOADING -> notifyTempFileIsReady(download.asWrapper())
            Download.STATE_COMPLETED -> notifyDownloadCompleted(download.asWrapper())
            Download.STATE_FAILED -> notifyDownloadFailed(download.asWrapper(), finalException)
            Download.STATE_REMOVING -> onDownloadRemoved(download.asWrapper())
            Download.STATE_RESTARTING -> { /* Nothing to do for these cases */ }
        }
    }

    private fun notifyDownloadFailed(
        download: DownloadWrapper,
        cause: Exception?,
    ) {
        val error = cause.toCBError()
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded failed ${download.uri} with error ${error.errorDesc}")
        forEachListener(state = Download.STATE_FAILED, url = download.uri) {
            onError(
                uri = download.uri,
                videoFileName = download.id,
                error = error,
            )
        }
    }

    private fun Exception?.toCBError(): CBError =
        when (this) {
            is IOException ->
                CBError(
                    CBError.Internal.NETWORK_FAILURE,
                    errorMessage,
                )

            else ->
                CBError(
                    CBError.Internal.MISCELLANEOUS,
                    errorMessage,
                )
        }

    private fun notifyDownloadCompleted(download: DownloadWrapper) {
        Logger.d("notifyDownloadCompleted() - download $download, listeners: $listeners")
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded success ${download.uri}")
        forEachListener(state = Download.STATE_COMPLETED, url = download.uri) {
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
        forEachListener(state = Download.STATE_DOWNLOADING, url = download.uri) {
            tempFileIsReady(
                url = download.uri,
                videoFileName = download.id,
                expectedContentSize = 0L,
                adUnitVideoPrecacheTempCallback = null,
            )
        }
    }

    private fun onDownloadRemoved(download: DownloadWrapper) {
        Logger.d("downloadRemoved() - download $download, listeners: $listeners")
        fakePrecacheFilesManager.downloadRemoved(download)
        latestDownloadStateSent = latestDownloadStateSent - download.uri
    }

    // Called when CacheEvictor is evicting a specific cache span that has this URL as key
    override fun onEvictingUrl(url: String) {
        getDownloadManager().downloads().find { it.uri == url }?.remove()
    }

    private fun DownloadWrapper.sendStopReason(stopReason: DownloadStopReason) {
        Logger.d("Download.sendStopReason() - download $this, stopReason $stopReason")
        try {
            DownloadService.sendSetStopReason(
                dependencies.context,
                VideoRepositoryDownloadService::class.java,
                id,
                stopReason.value,
                false,
            )
        } catch (e: Exception) {
            // java.lang.IllegalStateException: Not allowed to start service Intent: app is in background
            Logger.e("Error sending stop reason", e)
        }
    }

    private fun VideoAsset.sendAddDownload(stopReason: DownloadStopReason = DownloadStopReason.NONE) {
        Logger.d("VideoAsset.addDownload() - videoAsset $this, stopReason $stopReason")
        if (url.isNotBlank()) {
            try {
                DownloadService.sendAddDownload(
                    dependencies.context,
                    VideoRepositoryDownloadService::class.java,
                    DownloadRequest.Builder(
                        filename,
                        Uri.parse(url),
                    ).build(),
                    stopReason.value,
                    false,
                )
            } catch (e: Exception) {
                // java.lang.IllegalStateException: Not allowed to start service Intent: app is in background
                Logger.e("Error sending add download", e)
            }
        }
    }

    private fun List<DownloadWrapper>.removeAll() {
        forEach { it.remove() }
    }

    private fun DownloadWrapper.remove() {
        try {
            DownloadService.sendRemoveDownload(
                dependencies.context,
                VideoRepositoryDownloadService::class.java,
                id,
                false,
            )
            fakePrecacheFilesManager.downloadRemoved(this)
        } catch (e: Exception) {
            // java.lang.IllegalStateException: Not allowed to start service Intent: app is in background
            Logger.e("Error sending remove download", e)
        }
    }

    private fun DownloadWrapper.isTimeToLiveExpired(): Boolean = dependencies.videoCachePolicy.isTimeToLiveExpired(updateTime)
}
