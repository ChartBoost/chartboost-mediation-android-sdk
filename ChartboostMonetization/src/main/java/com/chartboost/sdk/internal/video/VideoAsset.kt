package com.chartboost.sdk.internal.video

import com.chartboost.sdk.internal.utils.TimeStamp
import com.chartboost.sdk.internal.utils.now
import java.io.File

data class VideoAsset(
    val url: String,
    val filename: String,
    val localFile: File?,
    val directory: File?,
    val creationDate: TimeStamp = now(),
    val queueFilePath: String = "",
    var expectedFileSize: Long = 0,
)
