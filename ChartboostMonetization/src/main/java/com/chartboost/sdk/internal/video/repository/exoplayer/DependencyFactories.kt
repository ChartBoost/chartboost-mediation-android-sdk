package com.chartboost.sdk.internal.video.repository.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import com.chartboost.sdk.internal.Libraries.FileCacheLocations
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.DefaultDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors

internal fun Context.preCacheDirectory(): File = FileCacheLocations(cacheDir).precacheDir

internal fun Context.preCacheQueueDirectory(): File = FileCacheLocations(cacheDir).precacheQueueDir

internal fun cache(
    fileCaching: ExoPlayerFileCaching,
    databaseProvider: DatabaseProvider,
    cachePolicy: VideoCachePolicy,
    evictorCallback: ChartboostCacheEvictor.EvictUrlCallback,
    evictor: CacheEvictor = ChartboostCacheEvictor(cachePolicy.maxBytes, evictorCallback),
): Cache =
    SimpleCache(
        fileCaching.precachingInternalDirectory,
        evictor,
        databaseProvider,
    )

internal fun cacheDataSourceFactory(
    cache: Cache,
    httpDataSourceFactory: HttpDataSource.Factory,
): CacheDataSource.Factory =
    CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)

private const val THREAD_POOL_SIZE = 2
private const val MAX_PARALLEL_DOWNLOADS = 1

internal fun downloadManager(
    context: Context,
    databaseProvider: DatabaseProvider,
    cache: Cache,
    httpDataSourceFactory: HttpDataSource.Factory,
    listener: DownloadManager.Listener,
    threadPoolSize: Int = THREAD_POOL_SIZE,
    maxParallelDownloads: Int = MAX_PARALLEL_DOWNLOADS,
): DownloadManager =
    DownloadManager(
        context,
        databaseProvider,
        cache,
        httpDataSourceFactory,
        Executors.newFixedThreadPool(threadPoolSize),
    ).apply {
        this.maxParallelDownloads = maxParallelDownloads
        addListener(listener)
    }

internal fun databaseProvider(context: Context): DatabaseProvider =
    DefaultDatabaseProvider(
        ExoPlayerSQLiteOpenHelper(context),
    )

internal fun setCookieHandler() {
    CookieHandler.setDefault(
        CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER) },
    )
}

private const val MIN_BUFFER_MS = 500

internal fun loadControl(
    minBufferMs: Int = MIN_BUFFER_MS,
    maxBufferMs: Int = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
): LoadControl =
    DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            minBufferMs,
            maxBufferMs,
            minBufferMs,
            minBufferMs,
        ).build()

internal fun DataSource.Factory.toMediaSource(): MediaSource.Factory = DefaultMediaSourceFactory(this)

// We don't need RECEIVE_BOOT_COMPLETED permission because we never schedule downloads on boot
@SuppressLint("MissingPermission")
@SuppressWarnings("MagicNumber")
internal fun scheduler(
    context: Context,
    jobId: Int = 1,
): Scheduler? =
    if (Util.SDK_INT >= 21) {
        PlatformScheduler(context, jobId)
    } else {
        null
    }
