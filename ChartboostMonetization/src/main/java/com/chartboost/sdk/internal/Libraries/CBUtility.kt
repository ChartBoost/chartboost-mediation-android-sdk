package com.chartboost.sdk.internal.Libraries

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Generic utility methods
 */
object CBUtility {
    /**
     * Gets the user agent string.
     *
     * @return the user agent string
     */
    @JvmStatic
    fun getUserAgent(): String = "${CBConstants.SDK_USERAGENT_BASE}  ${CBConstants.SDK_VERSION}"

    /**
     * Gets the current timezone.
     *
     * @return the current timezone
     */
    @JvmStatic
    fun getCurrentTimezone(): String {
        val timezoneFormat =
            SimpleDateFormat("ZZZZ", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
        return timezoneFormat.format(Date())
    }

    /**
     * Lists files in a directory, optionally recursively.
     *
     * @param directory the directory to list files from
     * @param recursive whether to list files recursively
     *
     * @return a list of files in the directory
     */
    @JvmStatic
    fun listFiles(
        directory: File?,
        recursive: Boolean,
    ): List<File> {
        directory ?: return emptyList()
        val fileList = mutableListOf<File>()
        val files = directory.listFiles() ?: return fileList
        for (file in files) {
            when {
                file.isFile && file.name != ".nomedia" -> fileList.add(file)
                file.isDirectory && recursive ->
                    fileList.addAll(
                        listFiles(file, recursive),
                    )
            }
        }
        return fileList
    }
}
