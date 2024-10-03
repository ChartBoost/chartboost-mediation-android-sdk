package com.chartboost.sdk.internal.video.player.mediaplayer

import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.RandomAccessFileWrapper
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.VideoAsset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal typealias VideoBufferFactory = (
    videoAsset: VideoAsset,
    listener: VideoBuffer.VideoBufferListener,
    coroutineDispatcher: CoroutineDispatcher,
    fileCache: FileCache?,
) -> VideoBuffer

private typealias RandomAccessFileFactory = (
    videoAsset: VideoAsset,
    tempHelper: TempFileDownloadHelper,
    fileCache: FileCache?,
) -> RandomAccessFileWrapper?

@VisibleForTesting
const val DEFAULT_BUFFER_UNLOCK_THRESHOLD = 0.01f // 1%

@VisibleForTesting
const val BUFFER_WAIT_TIME = 1500L

internal class VideoBuffer(
    videoAsset: VideoAsset,
    private val listener: VideoBufferListener,
    private var bufferUnlockThreshold: Float = DEFAULT_BUFFER_UNLOCK_THRESHOLD,
    tempHelper: TempFileDownloadHelper = TempFileDownloadHelper(),
    fileCache: FileCache?,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
    randomAccessFileFactory: RandomAccessFileFactory = ::createRandomAccessFile,
) {
    val randomAccessVideoFile: RandomAccessFileWrapper? by lazy {
        randomAccessFileFactory(
            videoAsset,
            tempHelper,
            fileCache,
        )
    }

    private var expectedVideoSize: Long = videoAsset.expectedFileSize

    private var sizeOnBuffer = 0L

    private var calculateBufferStatusJob: Job? = null

    fun buffer() {
        if (sizeOnBuffer == 0L) {
            sizeOnBuffer = randomAccessVideoFile?.length() ?: 0L
        }
    }

    fun checkBufferDownload() {
        calculateBufferStatusJob =
            CoroutineScope(coroutineDispatcher).launch {
                delay(BUFFER_WAIT_TIME)
                calculateBufferStatus()
            }
    }

    private fun calculateBufferStatus() {
        // file is fully downloaded no need to wait
        val fileSize = randomAccessVideoFile?.length() ?: 0L
        if (fileSize == expectedVideoSize) {
            unlockBuffer()
            return
        }

        // bufferUnlockThreshold over 1.5 seconds to unlock if slower then we keep on waiting
        // and eventually template should close - more info in calculateBufferThreshold()
        val sizeAfterWait = fileSize - sizeOnBuffer
        val percentDownload = sizeAfterWait.toFloat() / expectedVideoSize.toFloat()
        if (percentDownload > bufferUnlockThreshold) {
            unlockBuffer()
            return
        }

        checkBufferDownload()
    }

    private fun unlockBuffer() {
        sizeOnBuffer = 0
        stop()
        listener.onVideoBuffered()
    }

    fun calculateBufferThreshold(totalVideoDuration: Int) {
        // https://blog.frame.io/2017/03/06/calculate-video-bitrates/?__cf_chl_jschl_tk__=pmd_Nmv_v3doGG4fC0_LiOs9qu4OjGMpyW6jwCayaRff6mw-1632572240-0-gqNtZGzNAfujcnBszQi9
        // Bitrate = file size / (number of minutes * .0075)
        if (expectedVideoSize > 0 && totalVideoDuration > 0) {
            val videoSizeInMb = expectedVideoSize.toFloat() / (1000f * 1000f) // convert bytes to MiB
            val videoSizeInGb = videoSizeInMb / 1000f // convert to GiB
            val durationInMinutes = totalVideoDuration.toFloat() / 60000f // convert milliseconds to minutes

            // bitrate measured in mbps - ex. 7.5 is 720p quality
            // optimal connection for this bitrate would be 8mbps, on the other hand connection
            // of 2mbps is not enough and video could go to the end card but it won't force user
            // to watch a video when it would be buffering to much. Going to end card is controlled
            // by the template. If video is not buffer enough to reach the threshold to display 1 second
            // of the video within time decided by the template it will go to the end card
            // 0.0075f is constant for average bitrate 1/8 * 60 / 1000 = 0.0075 - read blog
            val bitrate = videoSizeInGb / (durationInMinutes * 0.0075f)

            // convert Mb to Mbits to be used in percentage calculation
            val sizeInMbits = videoSizeInMb * 8

            // bufferUnlockThreshold as percent to compare to the download percent in the buffer function
            // if we decide to make it less restrictive, then divide bufferUnlockThreshold until optimal value is reached
            // default value is 0.01 meaning 1% of the video should be downloaded within time given by template
            // 1% could be 1 second or 1 milliseconds of the video depends on the video
            // so it will result scattered video display, lots of loading screens and long video display
            // using calculated bitrate will ensure better quality display for the user but will require
            // better connection. Following the example bitrate of 7.5 mbps will require 5% of the video to be
            // downloaded before template decides to close so connection speed of 1mbps will only
            // manages to download 4% which is below the threshold so video will go to the end card
            // for given video minimum connection speed to ensure quality is 2mbps

            // bitrateAsPercent = bitrate as Mbits per second / total size in MB converted to Mbits
            bufferUnlockThreshold = bitrate / sizeInMbits
        }
    }

    fun stop() {
        calculateBufferStatusJob?.cancel()
        calculateBufferStatusJob = null
    }

    interface VideoBufferListener {
        fun onVideoBuffered()
    }
}

private fun createRandomAccessFile(
    videoAsset: VideoAsset,
    tempHelper: TempFileDownloadHelper,
    fileCache: FileCache?,
): RandomAccessFileWrapper? {
    return try {
        val file =
            fileCache?.run {
                getFileIfCached(precacheDir, videoAsset.filename)
            }
        if (file?.exists() == true) {
            tempHelper.createRandomAccessFile(file)
        } else {
            tempHelper.getTempFile(videoAsset.directory, videoAsset.filename)?.let {
                tempHelper.createRandomAccessFile(it)
            }
        }
    } catch (e: Exception) {
        Logger.e(e.toString())
        null
    }?.let {
        RandomAccessFileWrapper(it)
    }
}
