package com.chartboost.sdk.internal.video.repository.mediaplayer

import android.content.Context
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.now
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.repository.DownloadState
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.internal.video.repository.mediaplayer.VideoRepositoryMediaPlayer.StartDownloadConditions.BRING_TO_FRONT_QUEUE_AND_DOWNLOAD
import com.chartboost.sdk.internal.video.repository.mediaplayer.VideoRepositoryMediaPlayer.StartDownloadConditions.CAN_NOT_DOWNLOAD
import com.chartboost.sdk.internal.video.repository.mediaplayer.VideoRepositoryMediaPlayer.StartDownloadConditions.CREATE_ASSET_AND_DOWNLOAD
import com.chartboost.sdk.internal.video.repository.toDownloadState
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val REPEAT_INTERVAL = 5000L

/**
 * Video repository handles video download scheduling, checks cache policies and serves video assets
 */
internal class VideoRepositoryMediaPlayer(
    private val networkRequestService: CBNetworkService,
    private val policy: VideoCachePolicy,
    private val reachability: CBReachability?,
    private val fileCache: FileCache?,
    private val tempHelper: TempFileDownloadHelper = TempFileDownloadHelper(),
    private val backgroundExecutor: ScheduledExecutorService,
) : VideoRequest.VideoRequestCallback, VideoRepository {
    /**
     * Holds the queue of the video to be downloaded FIFO
     */
    private val videoQueue: Queue<VideoAsset> = ConcurrentLinkedQueue()

    /**
     * List of currently downloading urls, ideally should be list of 1 but it is possible
     * it will be bigger in some cases
     */
    private val downloadList = ConcurrentLinkedQueue<String>()

    /**
     * Notifies AdUnitManager about temp file being ready. Without this, sdk could show an ad
     * and ignore pre-cached video file
     */
    private val adUnitCallbackMap: ConcurrentHashMap<String, AdUnitVideoPrecacheTemp> =
        ConcurrentHashMap()

    /**
     * Holds easy access to video assets, can be accessed by filename
     * Needed to add map because queue is cleared once video is downloaded
     * and want to avoid accessing video files via context
     */
    private val videoMap: ConcurrentHashMap<String, VideoAsset> = ConcurrentHashMap()

    /**
     * Increase retry time for downloads with each try
     */
    private var repeatDownloadHandler = AtomicInteger(1)

    /**
     * Load cached videos into hashmap and remove expired files
     */
    override fun initialize(context: Context) {
        fileCache?.run {
            precacheFiles?.forEach { file ->
                if (file.exists() && file.name.contains(".tmp")) {
                    // remove temp files at the session start
                    deleteFile(file)
                    return
                }

                if (policy.isFileTimeToLeave(file)) {
                    deleteFile(file)
                } else {
                    val asset =
                        VideoAsset(
                            "",
                            file.name,
                            file,
                            precacheDir,
                            file.lastModified(),
                            expectedFileSize = file.length(),
                        )
                    videoMap[file.name] = asset
                }
            }
        }
    }

    /**
     * Checks if file is already downloaded
     * Check if video can be cached based on cache policies
     * and if check pass then schedule download
     */
    @Synchronized
    override fun downloadVideoFile(
        url: String,
        filename: String,
        showImmediately: Boolean,
        callback: AdUnitVideoPrecacheTemp?,
    ) {
        Logger.d("downloadVideoFile: $url")
        val destDir = fileCache?.precacheDir
        var dest: File? = fileCache?.getFileIfCached(destDir, filename)
        val isDownloadingOrDownloaded = isFileDownloadingOrDownloaded(filename)

        val startDownloadConditions =
            checkStartDownloadConditions(
                url,
                filename,
                showImmediately,
                callback,
                isDownloadingOrDownloaded,
                dest,
            )

        when (startDownloadConditions) {
            CAN_NOT_DOWNLOAD -> return
            CREATE_ASSET_AND_DOWNLOAD -> {
                // create new file and asset
                dest = File(destDir, filename)
                createAsset(url, filename, dest, destDir)
                startDownloadIfPossible(
                    if (showImmediately) filename else null,
                    repeatDownloadHandler.get(),
                    showImmediately,
                )
            }
            BRING_TO_FRONT_QUEUE_AND_DOWNLOAD -> {
                startDownloadIfPossible(
                    filename = filename,
                    forceDownload = true,
                )
            }
        }
    }

    private enum class StartDownloadConditions {
        CAN_NOT_DOWNLOAD,
        CREATE_ASSET_AND_DOWNLOAD,
        BRING_TO_FRONT_QUEUE_AND_DOWNLOAD,
    }

    private fun checkStartDownloadConditions(
        url: String,
        filename: String,
        showImmediately: Boolean,
        callback: AdUnitVideoPrecacheTemp?,
        isDownloadingOrDownloaded: Boolean,
        dest: File?,
    ): StartDownloadConditions {
        if (showImmediately) {
            if (isDownloadingOrDownloaded) {
                if (adUnitCallbackMap.containsKey(url)) {
                    Logger.d("Already downloading for show operation: $filename")
                    SandboxBridgeSettings.sendLogsToSandbox("Already downloading for show operation: $filename")
                    tempFileIsReady(url, filename, dest?.length() ?: 0, callback)
                    return CAN_NOT_DOWNLOAD
                }
                if (callback != null) {
                    Logger.d("Register callback for show operation: $filename")
                    SandboxBridgeSettings.sendLogsToSandbox("Register callback for show operation: $filename")
                    tempFileIsReady(url, filename, dest?.length() ?: 0, callback)
                    return CAN_NOT_DOWNLOAD
                }
            } else {
                Logger.d("Not downloading for show operation: $filename")
                if (callback != null) {
                    if (videoMap[filename]?.filename == filename || adUnitCallbackMap.containsKey(url)) {
                        // This case means we already set callback for this ad but it didn't have time to start
                        // download which is schedule to start soon. You can reproduce it by pressing cache and
                        // show quickly multiple times or schedule show right in the cache callback
                        adUnitCallbackMap[url] = callback
                        return BRING_TO_FRONT_QUEUE_AND_DOWNLOAD
                    }
                }
            }
            if (callback != null) {
                Logger.d("Register callback for show operation: $filename")
                SandboxBridgeSettings.sendLogsToSandbox("Register callback for show operation: $filename")
                adUnitCallbackMap[url] = callback
            }
        } else {
            if (isAlreadyInTheQueue(url, filename) || isDownloadingOrDownloaded) {
                Logger.d("Already queued or downloading for cache operation: $filename")
                SandboxBridgeSettings.sendLogsToSandbox("Already queued or downloading for cache operation: $filename")
                return CAN_NOT_DOWNLOAD
            }
        }
        return CREATE_ASSET_AND_DOWNLOAD
    }

    /**
     * Check queue and if possible start download or in case lack of internet schedule next download in the future
     * filename: filename of the video to download, if null then it will download next video in the queue
     * repeat: number of times to repeat download if it fails
     * forceDownload: force new asset download even if another asset is already downloading
     */
    override fun startDownloadIfPossible(
        filename: String?,
        repeat: Int,
        forceDownload: Boolean,
    ) {
        Logger.d("startDownloadIfPossible: $filename")
        if (videoQueue.size > 0) {
            if (forceDownload || shouldStartDownloadNow()) {
                getVideoAssetFromTheQueue(filename)?.let {
                    startDownloadNow(it)
                }
            } else {
                SandboxBridgeSettings.sendLogsToSandbox("Can't cache next video at the moment")
                val delay = REPEAT_INTERVAL * repeat // first retry after 5 seconds then increment
                backgroundExecutor.schedule(downloadRunnable, delay, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun shouldStartDownloadNow(): Boolean =
        reachability?.isNetworkAvailable ?: false &&
            !policy.isMaxCountForTimeWindowReached() &&
            downloadList.isEmpty()

    private fun getVideoAssetFromTheQueue(filename: String?): VideoAsset? {
        var videoAsset: VideoAsset? = null
        if (filename == null) {
            videoAsset = videoQueue.poll()
        } else {
            videoQueue.iterator().forEach {
                if (it.filename == filename) {
                    videoAsset = it
                }
            }
        }
        videoAsset?.let { deleteQueueFile(it) }
        return videoAsset
    }

    private fun startDownloadNow(videoAsset: VideoAsset) {
        Logger.d("startDownloadNow: ${videoAsset.url}")
        if (isFileDownloadingOrDownloaded(videoAsset.filename)) {
            SandboxBridgeSettings.sendLogsToSandbox("File already downloaded or downloading: ${videoAsset.filename}")
            videoAsset.url.let {
                adUnitCallbackMap.remove(it)?.tempVideoFileIsReady(it)
            }
            return
        }

        SandboxBridgeSettings.sendLogsToSandbox("Start downloading ${videoAsset.url}")
        // save timestamp of the first video within the time window
        policy.addDownloadToTimeWindow()
        downloadList.add(videoAsset.url)
        val request =
            VideoRequest(
                reachability,
                videoAsset.localFile!!,
                videoAsset.url,
                this,
                Priority.NORMAL,
                networkRequestService.appId,
            )
        networkRequestService.submit(request)
    }

    override fun isFileDownloadingOrDownloaded(videoFilename: String): Boolean {
        val asset = getVideoAsset(videoFilename)
        var isAlreadyDownloading = false
        var isDownloaded = false

        if (asset != null && isFileDownloading(asset)) {
            isAlreadyDownloading = true
        }

        if (asset != null && isFileCached(asset)) {
            isDownloaded = true
        }
        return isAlreadyDownloading || isDownloaded
    }

    override fun getVideoAsset(filename: String): VideoAsset? {
        return videoMap[filename]
    }

    override fun getVideoDownloadState(asset: VideoAsset?): DownloadState {
        asset?.let {
            if (isFileCached(it)) {
                return VideoRepository.VIDEO_STATE_FULL
            }

            val tempFileSize = getTempFileWhileDownloading(it)?.length() ?: 0
            if (it.expectedFileSize == 0L) {
                // At this point expectedContentSize should not be 0
                return VideoRepository.VIDEO_STATE_EMPTY
            }

            val loadedPercentage = tempFileSize.toFloat() / it.expectedFileSize.toFloat()
            return loadedPercentage.toDownloadState()
        }
        return VideoRepository.VIDEO_STATE_EMPTY
    }

    override fun removeAsset(videoAsset: VideoAsset?): Boolean {
        videoAsset?.let { asset ->
            if (isFileCached(asset)) {
                val file = asset.localFile
                val filename = asset.filename
                fileCache?.let { fileCache ->
                    if (fileCache.deleteFile(file)) {
                        videoMap.remove(filename)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isFileDownloading(asset: VideoAsset): Boolean {
        return tempHelper.isAssetDownloading(asset.directory, asset.filename)
    }

    private fun getTempFileWhileDownloading(asset: VideoAsset): File? {
        return tempHelper.getTempFile(asset.directory, asset.filename)
    }

    private fun cleanCachedAssetsWhenReachedCacheLimit() {
        if (isVideoCacheSizeReached()) {
            videoMap.values.sortedWith(compareBy { it.creationDate }).forEach { asset ->
                removeAsset(asset)
                if (!isVideoCacheSizeReached()) {
                    return
                }
            }
        }
    }

    private fun isVideoCacheSizeReached(): Boolean {
        fileCache?.let {
            val cacheSize = it.getFolderSize(it.precacheDir)
            return policy.isVideoCacheMaxSizeReached(cacheSize)
        }
        return false
    }

    private fun createAsset(
        url: String,
        filename: String,
        dest: File,
        destDir: File?,
    ) {
        // create video asset even if file is not created to still hold the url
        val asset =
            VideoAsset(
                url,
                filename,
                dest,
                destDir,
                queueFilePath = fileCache?.precacheQueueDir?.absolutePath + File.separator + filename,
            )

        dest.setLastModified(asset.creationDate)
        createQueueFile(asset)
        asset.let {
            videoMap.putIfAbsent(filename, it)
            videoQueue.offer(it)
        }
    }

    private fun createQueueFile(videoAsset: VideoAsset) {
        if (SandboxBridgeSettings.isSandboxMode) {
            val tempQueueFile = File(videoAsset.queueFilePath)
            try {
                tempQueueFile.createNewFile()
                tempQueueFile.setLastModified(now())
            } catch (ioException: IOException) {
                Logger.e("Error while creating queue empty file: $ioException")
            }
        }
    }

    private fun deleteQueueFile(videoAsset: VideoAsset) {
        if (SandboxBridgeSettings.isSandboxMode) {
            val pathToQueueFile = videoAsset.queueFilePath
            val tempQueueFile = File(pathToQueueFile)
            if (tempQueueFile.exists()) {
                tempQueueFile.delete()
            }
        }
    }

    private fun isFileCached(asset: VideoAsset?): Boolean {
        if (asset == null) {
            return false
        }

        if (asset.localFile == null) {
            return false
        }

        fileCache?.let {
            val file = asset.localFile
            return it.isFileCached(file)
        }
        return false
    }

    override fun tempFileIsReady(
        url: String,
        videoFileName: String,
        expectedContentSize: Long,
        adUnitVideoPrecacheTempCallback: AdUnitVideoPrecacheTemp?,
    ) {
        Logger.d("tempFileIsReady: $videoFileName")
        val asset: VideoAsset? = getVideoAsset(videoFileName)
        if (expectedContentSize > 0) {
            asset?.expectedFileSize = expectedContentSize
        }

        asset?.let {
            videoMap.remove(videoFileName)
            videoMap.putIfAbsent(videoFileName, it)
        }

        var callback = adUnitVideoPrecacheTempCallback
        if (callback == null) {
            callback = adUnitCallbackMap[url]
        }

        // Most of the time after 9.0.0 this callback is not attached as ads are cached first
        // and we don't support cacheOnShow anymore
        callback?.tempVideoFileIsReady(url)
    }

    private fun isAlreadyInTheQueue(
        nextUrl: String,
        nextFilename: String,
    ): Boolean {
        if (videoQueue.size > 0) {
            val queueIterator = videoQueue.iterator()
            while (queueIterator.hasNext()) {
                val asset = queueIterator.next()
                if (asset.url == nextUrl && asset.filename == nextFilename) {
                    return true
                }
            }
        }
        return false
    }

    override fun onSuccess(
        uri: String,
        videoFileName: String,
    ) {
        Logger.d("onSuccess: $uri")
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded success $uri")
        cleanCachedAssetsWhenReachedCacheLimit()
        downloadList.remove(uri)
        adUnitCallbackMap.remove(uri)
        repeatDownloadHandler = AtomicInteger(1)
        deleteAssetFromQueue(uri)
        // after error check the queue and try to download next asset
        startDownloadIfPossible(null, repeatDownloadHandler.get(), false)
    }

    override fun onError(
        uri: String,
        videoFileName: String,
        error: CBError?,
    ) {
        Logger.d("onError: $uri")
        val errorDesc = error?.errorDesc ?: "Unknown error"
        val videoAsset = getVideoAsset(videoFileName)
        // delete local file that didn't complete download
        videoAsset?.localFile?.delete()
        if (error != null && (error.type == CBError.Internal.INTERNET_UNAVAILABLE)) {
            // no connection - add video to the queue
            videoAsset?.let { asset ->
                videoQueue.add(asset)
                createQueueFile(asset)
            }
        } else {
            deleteAssetFromQueue(uri)
            // force the flow in case of an error to avoid ads being stuck
            adUnitCallbackMap[uri]?.tempVideoFileIsReady(uri) ?: Logger.e(
                "Missing callback on error",
            )
        }

        adUnitCallbackMap.remove(uri)
        videoMap.remove(videoFileName)
        // after error check the queue and try to download next asset
        startDownloadIfPossible(null, repeatDownloadHandler.get(), false)
        Logger.e("Video download failed: $uri with error $errorDesc")
        SandboxBridgeSettings.sendLogsToSandbox("Video downloaded failed $uri with error $errorDesc")
        downloadList.remove(uri)
    }

    private fun deleteAssetFromQueue(url: String) {
        val tempQueue: Queue<VideoAsset> = LinkedList(videoQueue)
        for (asset in tempQueue) {
            if (asset != null && asset.url == url) {
                videoQueue.remove(asset)
            }
        }
    }

    private val downloadRunnable =
        Runnable {
            startDownloadIfPossible(null, repeatDownloadHandler.incrementAndGet(), false)
        }
}
