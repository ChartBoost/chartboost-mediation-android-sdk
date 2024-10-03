package com.chartboost.sdk.internal.video

import com.chartboost.sdk.internal.logging.Logger
import java.io.File
import java.io.RandomAccessFile

/**
 * Helper class which checks if there is temporary download video file.
 * Had to do this abstractions for purpose of testing.
 */
class TempFileDownloadHelper {
    fun isAssetDownloading(
        directory: File?,
        filename: String?,
    ): Boolean {
        // network dispatcher automatically ads tmp extension and handles the file copy when finished
        if (directory == null || filename == null) {
            return false
        }
        try {
            val tempFile = getTempFile(directory, filename)
            return tempFile?.exists() ?: false
        } catch (e: Exception) {
            Logger.d(e.toString())
        }
        return false
    }

    fun getTempFile(
        directory: File?,
        filename: String?,
    ): File? =
        if (directory != null && filename != null) {
            File(directory, "$filename.tmp")
        } else {
            null
        }

    fun createRandomAccessFile(videoFile: File?): RandomAccessFile? = videoFile?.let { RandomAccessFile(it, "rwd") }
}
