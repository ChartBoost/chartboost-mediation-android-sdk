package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import java.io.File

internal typealias FileCachingFactory = (Context) -> ExoPlayerFileCaching

private const val INTERNAL_EXOPLAYER_CACHE_DIRECTORY = "exoplayer-cache"

internal interface ExoPlayerFileCaching {
    val precacheDirectory: File
    val precacheQueueDirectory: File
    val precachingInternalDirectory: File

    fun preCachedFile(id: String): File?

    fun preCachedFiles(): List<File>

    fun cachedFile(id: String): File
}

internal class ExoPlayerFileCachingImpl(
    context: Context,
    override val precacheDirectory: File = context.preCacheDirectory(),
    override val precacheQueueDirectory: File = context.preCacheQueueDirectory(),
    override val precachingInternalDirectory: File = File(precacheDirectory, INTERNAL_EXOPLAYER_CACHE_DIRECTORY),
) : ExoPlayerFileCaching {
    override fun preCachedFile(id: String): File? = preCachedFiles().find { it.name == id }

    override fun preCachedFiles(): List<File> =
        precacheDirectory
            .listFiles()
            ?.filter { it.isValidPrecachedFile() }
            ?.toList()
            ?: emptyList()

    private fun File.isValidPrecachedFile(): Boolean =
        !path.endsWith(".uid") && // ExoPlayer file
            !isDirectory

    override fun cachedFile(id: String): File = File(precacheDirectory, id)
}
