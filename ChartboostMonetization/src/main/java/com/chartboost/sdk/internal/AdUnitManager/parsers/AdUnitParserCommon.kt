package com.chartboost.sdk.internal.AdUnitManager.parsers

import android.net.Uri

internal fun retrieveFilenameFromVideoUrl(url: String): String {
    var filename = ""
    var videoUrl = url
    if (videoUrl.isNotEmpty()) {
        // Uri is not handling properly the url without protocol hence, needs to be added
        if (!videoUrl.startsWith("https://") && !videoUrl.startsWith("http://")) {
            videoUrl = "https://$videoUrl"
        }

        val uri = Uri.parse(videoUrl) ?: return filename
        val segments = uri.pathSegments
        filename = segments.joinToString(separator = "_")
    }
    return filename
}

fun parseMtype(mtype: Int): MediaTypeOM {
    return MediaTypeOM.entries.find { it.intValue == mtype } ?: MediaTypeOM.UNKNOWN
}
