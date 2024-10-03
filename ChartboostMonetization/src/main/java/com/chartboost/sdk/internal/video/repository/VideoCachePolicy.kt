package com.chartboost.sdk.internal.video.repository

import com.chartboost.sdk.SandboxBridgeSettings.sendLogsToSandbox
import com.chartboost.sdk.internal.Model.Percentage
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.now
import java.io.File

/**
 * Config values are represented in seconds
 */
class VideoCachePolicy(
    var maxBytes: Long = 104857600, // 100mb
    var maxUnitsPerTimeWindow: Int = 10,
    var maxUnitsPerTimeWindowCellular: Int = 8,
    var timeWindow: Long = 18000, // 5h
    var timeWindowCellular: Long = 15000,
    var ttl: Long = 7200, // 2h
    var bufferSize: Percentage = 3, // TODO Never used
    val reachability: CBReachability?,
) {
    /**
     * Initial time for this time window
     */
    @Volatile
    private var timeWindowStartTimeStamp: Long = 0

    /**
     * Amount of videos cached during one time window
     */
    @Volatile
    private var timeWindowCachedVideosCount: Int = 0

    private val timeFromLastFileCache: Long
        get() = now() - timeWindowStartTimeStamp

    private val remainingWindowTime: Long
        get() = getTimeWindowForNetworkInMs() - timeFromLastFileCache

    fun addDownloadToTimeWindow() {
        Logger.d(
            "addDownloadToTimeWindow() - " +
                "timeWindowStartTimeStamp $timeWindowStartTimeStamp, " +
                "timeWindowCachedVideosCount $timeWindowCachedVideosCount",
        )
        if (timeWindowStartTimeStamp == 0L) {
            timeWindowStartTimeStamp = now()
        }
        timeWindowCachedVideosCount++
    }

    fun isFileTimeToLeave(file: File): Boolean = isTimeToLiveExpired(file.lastModified())

    fun isTimeToLiveExpired(lastModified: Long): Boolean {
        val now = now()
        val ttlMs = ttl * 1000
        return (now - lastModified) > ttlMs
    }

    fun isVideoCacheMaxSizeReached(videosCachedSize: Long): Boolean {
        return videosCachedSize >= maxBytes
    }

    fun isMaxCountForTimeWindowReached(): Boolean {
        resetWindowWhenTimeReached()
        val isMaxCountReached = timeWindowCachedVideosCount >= getMaxUnitsPerTimeWindowForNetwork()
        if (isMaxCountReached) {
            sendLogsToSandbox("Video loading limit reached, will resume in timeToResetWindow: $remainingWindowTime")
        }
        Logger.d("isMaxCountForTimeWindowReached() - $isMaxCountReached")
        return isMaxCountReached
    }

    fun timeToWindowEnd(): Long {
        val timeWindowForNetwork = getTimeWindowForNetworkInMs()
        val timeFromLastFileCache = now() - timeWindowStartTimeStamp
        return timeWindowForNetwork - timeFromLastFileCache
    }

    // Check and reset time window and count if enough time has elapsed
    private fun resetWindowWhenTimeReached() {
        Logger.d("resetWindowWhenTimeReached()")
        val timeWindowForNetwork = getTimeWindowForNetworkInMs()
        val isTimeWindowExpired = timeFromLastFileCache > timeWindowForNetwork
        if (isTimeWindowExpired) {
            Logger.d("resetWindowWhenTimeReached() - timer and count reset")
            sendLogsToSandbox("Video loading limit reset")
            timeWindowCachedVideosCount = 0
            timeWindowStartTimeStamp = 0L
        }
    }

    private fun getTimeWindowForNetworkInMs(): Long =
        (if (reachability?.isConnectionCellular() == true) timeWindowCellular else timeWindow) * 1000

    private fun getMaxUnitsPerTimeWindowForNetwork(): Int =
        if (reachability?.isConnectionCellular() == true) {
            maxUnitsPerTimeWindowCellular
        } else {
            maxUnitsPerTimeWindow
        }
}
