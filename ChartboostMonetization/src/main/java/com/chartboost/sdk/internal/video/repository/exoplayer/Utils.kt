package com.chartboost.sdk.internal.video.repository.exoplayer

import com.google.android.exoplayer2.offline.DownloadCursor
import com.google.android.exoplayer2.offline.DownloadManager

internal fun DownloadCursor.asList(): List<DownloadWrapper> {
    val downloadList = mutableListOf<DownloadWrapper>()
    while (moveToNext()) {
        downloadList.add(download.asWrapper())
    }
    return downloadList
}

internal fun DownloadManager.downloads(): List<DownloadWrapper> = downloadIndex.getDownloads().asList()

internal fun DownloadManager.download(id: String): DownloadWrapper? = downloadIndex.getDownload(id)?.asWrapper()
