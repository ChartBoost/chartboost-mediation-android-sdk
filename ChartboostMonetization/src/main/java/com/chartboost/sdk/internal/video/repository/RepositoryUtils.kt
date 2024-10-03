package com.chartboost.sdk.internal.video.repository

@SuppressWarnings("MagicNumber")
internal fun Float.toDownloadState(): DownloadState =
    when {
        this == 0f -> VideoRepository.VIDEO_STATE_EMPTY
        this < 0.25 -> VideoRepository.VIDEO_STATE_QUARTILE_1
        this < 0.5 -> VideoRepository.VIDEO_STATE_QUARTILE_2
        this < 0.75 -> VideoRepository.VIDEO_STATE_QUARTILE_3
        this < 1 -> VideoRepository.VIDEO_STATE_QUARTILE_4
        else -> VideoRepository.VIDEO_STATE_FULL
    }
