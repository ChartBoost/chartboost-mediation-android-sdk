package com.chartboost.sdk.internal.video.repository.exoplayer

import com.google.android.exoplayer2.offline.Download

@SuppressWarnings("MagicNumber")
enum class DownloadStopReason(val value: Int) {
    NONE(Download.STOP_REASON_NONE), // 0
    STOPPED_QUEUE(1),
    MAX_COUNT_TIME_WINDOW(2),
    FORCED_OUT(3),
    ;

    companion object {
        fun fromInt(value: Int): DownloadStopReason? = entries.find { it.value == value }
    }
}
