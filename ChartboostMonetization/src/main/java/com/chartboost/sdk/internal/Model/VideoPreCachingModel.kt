package com.chartboost.sdk.internal.Model

import org.json.JSONObject

private const val PRECACHE_MAXBYTES = "maxBytes"
private const val PRECACHE_MAX_UNITS_PER_TIME_WINDOW = "maxUnitsPerTimeWindow"
private const val PRECACHE_MAX_UNITS_PER_TIME_WINDOW_CELLULAR = "maxUnitsPerTimeWindowCellular"
private const val PRECACHE_TIME_WINDOW = "timeWindow"
private const val PRECACHE_TIME_WINDOW_CELLULAR = "timeWindowCellular"
private const val PRECACHE_TTL = "ttl"
private const val PRECACHE_BUFFER_SIZE = "bufferSize"
private const val PRECACHE_VIDEO_PLAYER = "videoPlayer"

private const val DEFAULT_MAX_BYTES = 52428800L
private const val DEFAULT_UNITS_PER_TIME_WINDOW = 10
private const val DEFAULT_TIME_WINDOW = 18000L
private const val DEFAULT_TTL = 604800L
private const val DEFAULT_BUFFER_SIZE = 3 // percentage
private val DEFAULT_VIDEO_PLAYER = VideoPreCachingModel.VideoPlayerType.EXO_PLAYER.value

internal typealias Bytes = Long
internal typealias Seconds = Long
internal typealias Percentage = Int

data class VideoPreCachingModel(
    val maxBytes: Bytes = DEFAULT_MAX_BYTES,
    val maxUnitsPerTimeWindow: Int = DEFAULT_UNITS_PER_TIME_WINDOW,
    val maxUnitsPerTimeWindowCellular: Int = DEFAULT_UNITS_PER_TIME_WINDOW,
    val timeWindow: Seconds = DEFAULT_TIME_WINDOW,
    val timeWindowCellular: Seconds = DEFAULT_TIME_WINDOW,
    val ttl: Long = DEFAULT_TTL,
    val bufferSize: Percentage = DEFAULT_BUFFER_SIZE,
    val videoPlayer: VideoPlayerType = VideoPlayerType.EXO_PLAYER,
) {
    companion object {
        @JvmStatic
        fun parseVideoPreCachingModelConfig(config: JSONObject): VideoPreCachingModel {
            return VideoPreCachingModel(
                maxBytes = config.optLong(PRECACHE_MAXBYTES, DEFAULT_MAX_BYTES),
                maxUnitsPerTimeWindow =
                    config.optInt(
                        PRECACHE_MAX_UNITS_PER_TIME_WINDOW,
                        DEFAULT_UNITS_PER_TIME_WINDOW,
                    ),
                maxUnitsPerTimeWindowCellular =
                    config.optInt(
                        PRECACHE_MAX_UNITS_PER_TIME_WINDOW_CELLULAR,
                        DEFAULT_UNITS_PER_TIME_WINDOW,
                    ),
                timeWindow = config.optLong(PRECACHE_TIME_WINDOW, DEFAULT_TIME_WINDOW),
                timeWindowCellular =
                    config.optLong(
                        PRECACHE_TIME_WINDOW_CELLULAR,
                        DEFAULT_TIME_WINDOW,
                    ),
                ttl = config.optLong(PRECACHE_TTL, DEFAULT_TTL),
                bufferSize = config.optInt(PRECACHE_BUFFER_SIZE, DEFAULT_BUFFER_SIZE),
                videoPlayer =
                    config.optString(PRECACHE_VIDEO_PLAYER, DEFAULT_VIDEO_PLAYER)
                        .let { VideoPlayerType.fromString(it) },
            )
        }
    }

    enum class VideoPlayerType(val value: String) {
        EXO_PLAYER("exoplayer"),
        MEDIA_PLAYER("mediaplayer"),
        ;

        companion object {
            fun fromString(value: String): VideoPlayerType = entries.firstOrNull { it.value == value } ?: EXO_PLAYER
        }
    }
}
