package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.repository.DownloadState
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.internal.video.repository.toDownloadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Video pre-caching repository for ExoPlayer.
 * This class implements the pre-caching logic for video assets as follows:
 * - ExoPlayerDownloadManager is used to download the video file. Max concurrent downloads is 2.
 * - Download queue is managed by ExoPlayerDownloadManager by using DownloadStopReason to queue the
 * downloads without starting them.
 * - DownloadStopReason.NONE is used to start the downloads.
 * For more details on video pre-caching, see:
 * https://chartboost.atlassian.net/wiki/spaces/SDK/pages/1972994199/Video+Pre-Caching
 */
internal class VideoRepositoryExoplayer(
    private val policy: VideoCachePolicy,
    private val downloadManager: ExoPlayerDownloadManager,
    private val fileCachingFactory: FileCachingFactory = { c -> ExoPlayerFileCachingImpl(c) },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VideoRepository, VideoRequest.VideoRequestCallback {
    // We don't really need this, mostly it is to make sure getVideoAsset(filename: String) returns a valid video asset
    private val filenameToAsset: ConcurrentHashMap<String, VideoAsset> by lazy { ConcurrentHashMap() }

    private val urlToCallback: ConcurrentHashMap<String, AdUnitVideoPrecacheTemp> by lazy { ConcurrentHashMap() }
    private var fileCaching: ExoPlayerFileCaching? = null

    private var retryJob: Job? = null

    override fun initialize(context: Context) {
        Logger.d("initialize()")
        fileCaching = fileCachingFactory(context)
        with(downloadManager) {
            initialize()
            addListener(this@VideoRepositoryExoplayer)
            cleanDownloads()
        }
    }

    override fun downloadVideoFile(
        url: String,
        filename: String,
        showImmediately: Boolean,
        callback: AdUnitVideoPrecacheTemp?,
    ) {
        Logger.d(
            "downloadVideoFile() - url: $url, filename: $filename, " +
                "showImmediately: $showImmediately, callback: $callback",
        )

        callback?.let { urlToCallback[url] = it }

        cachedFile(filename)
            ?.asAsset(url)
            ?.link()
            ?.queueDownload()
            ?: Logger.d("downloadVideoFile() - cache file is null")

        startDownloadIfPossible(
            filename = filename,
            forceDownload = showImmediately,
        )
    }

    private fun cachedFile(filename: String): File? = fileCaching?.cachedFile(filename)

    private fun VideoAsset.link(): VideoAsset =
        apply {
            filenameToAsset[filename] = this
        }

    private fun VideoAsset.queueDownload(): VideoAsset =
        apply {
            Logger.d("queueDownload() - asset: $this")
            sendDownloadToDownloadManager(this, DownloadStopReason.STOPPED_QUEUE)
        }

    private fun File.asAsset(url: String): VideoAsset =
        VideoAsset(
            url = url,
            filename = name,
            localFile = this,
            directory = parentFile,
        ).apply {
            this@asAsset.setLastModified(creationDate)
        }

    // If we have the asset, go through the logic of downloading this asset
    // If we don't have the asset, resume next download
    override fun startDownloadIfPossible(
        filename: String?,
        repeat: Int,
        forceDownload: Boolean,
    ) {
        Logger.d("startDownloadIfPossible() - filename $filename, forceDownload $forceDownload")
        filename?.let(filenameToAsset::get)?.let { asset ->
            Logger.d("startDownloadIfPossible() - asset: $asset")
            if (forceDownload) {
                startForcedDownload(asset)
            } else {
                startNonForcedDownload(asset)
            }
        } ?: run {
            Logger.d("startDownloadIfPossible() - null asset, resume next download in Download Manager index")
            nextDownload()
        }
    }

    private fun nextDownload() {
        checkIsMaxCountForTimeWindowReached { stopReason ->
            if (stopReason == DownloadStopReason.NONE) {
                policy.addDownloadToTimeWindow()
            }
            downloadManager.startNextDownload(stopReason)
        }
    }

    private fun startForcedDownload(asset: VideoAsset) {
        Logger.d("startForcedDownload() - $asset")
        policy.addDownloadToTimeWindow()
        downloadManager.startDownload(asset)
    }

    private fun startNonForcedDownload(asset: VideoAsset) {
        checkIsMaxCountForTimeWindowReached { stopReason ->
            sendDownloadToDownloadManager(asset, stopReason)
        }
    }

    // We don't need to check for a max download count because download manager will just queue
    // the downloads without starting them.
    // We do need to check for max download count for the current time window, so overall
    // download manager will have less or equal download count in the queue for the current time window
    // than specified by cache policy.
    private inline fun checkIsMaxCountForTimeWindowReached(block: (DownloadStopReason) -> Unit) =
        if (policy.isMaxCountForTimeWindowReached()) {
            retryNonForcedDownloadAfterTimeWindowEnds()
            DownloadStopReason.MAX_COUNT_TIME_WINDOW
        } else {
            DownloadStopReason.NONE
        }.let(block)

    private fun retryNonForcedDownloadAfterTimeWindowEnds() {
        if (retryJob == null) {
            retryJob =
                CoroutineScope(dispatcher).launch {
                    delay(policy.timeToWindowEnd())
                    retryJob = null
                    try {
                        startDownloadIfPossible()
                    } catch (e: IllegalStateException) {
                        // If we are in background and the service is dead, this call will attempt to wake up
                        // the service, resulting in an IllegalStateException for trying to start up a service
                        // from a background process.
                        Logger.e("Cannot start download", e)
                    }
                }
        }
    }

    private fun sendDownloadToDownloadManager(
        asset: VideoAsset,
        reason: DownloadStopReason = DownloadStopReason.NONE,
    ) {
        Logger.d("sendDownloadToDownloadManager() - $asset")
        if (reason == DownloadStopReason.NONE) {
            policy.addDownloadToTimeWindow()
        }
        downloadManager.addDownload(asset, reason)
    }

    override fun isFileDownloadingOrDownloaded(videoFilename: String): Boolean = downloadManager.isDownloadingOrDownloaded(videoFilename)

    override fun getVideoAsset(filename: String): VideoAsset? = filenameToAsset[filename]

    override fun getVideoDownloadState(asset: VideoAsset?): DownloadState =
        asset?.let {
            downloadManager.downloadPercentage(it.filename).toDownloadState()
        } ?: VideoRepository.VIDEO_STATE_EMPTY

    override fun removeAsset(videoAsset: VideoAsset?): Boolean =
        videoAsset?.let {
            downloadManager.removeDownload(it.filename)
            filenameToAsset.remove(it.filename)
            true
        } ?: false

    override fun tempFileIsReady(
        url: String,
        videoFileName: String,
        expectedContentSize: Long,
        adUnitVideoPrecacheTempCallback: AdUnitVideoPrecacheTemp?,
    ) {
        Logger.d("tempFileIsReady() - url $url, videoFileName $videoFileName")
        (adUnitVideoPrecacheTempCallback ?: urlToCallback[url])?.tempVideoFileIsReady(url)
    }

    override fun onSuccess(
        uri: String,
        videoFileName: String,
    ) {
        Logger.d("onSuccess() - uri $uri, videoFileName $videoFileName")
        urlToCallback.remove(uri)
        startDownloadIfPossible()
    }

    override fun onError(
        uri: String,
        videoFileName: String,
        error: CBError?,
    ) {
        Logger.d("onError() - uri $uri, videoFileName $videoFileName, error $error")
        urlToCallback.remove(uri)
    }
}
