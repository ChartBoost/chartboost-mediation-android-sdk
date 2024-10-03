package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource

private typealias DownloadManagerFactory = (
    Context,
    DatabaseProvider,
    Cache,
    HttpDataSource.Factory,
    DownloadManager.Listener,
) -> DownloadManager

private typealias CacheDataSourceFactoryFactory = (Cache, HttpDataSource.Factory) -> CacheDataSource.Factory

private typealias CacheFactory = (
    ExoPlayerFileCaching,
    VideoCachePolicy,
    DatabaseProvider,
    ChartboostCacheEvictor.EvictUrlCallback,
) -> Cache

private typealias FakePrecacheFilesManagerFactory = (ExoPlayerFileCaching) -> FakePrecacheFilesManager

internal data class ExoPlayerDownloadManagerDependencies(
    val context: Context = ChartboostDependencyContainer.androidComponent.context.applicationContext,
    val videoCachePolicy: VideoCachePolicy = ChartboostDependencyContainer.applicationComponent.videoCachePolicy,
    val fileCachingFactory: FileCachingFactory = { c -> ExoPlayerFileCachingImpl(c) },
    val cacheFactory: CacheFactory = {
            fc: ExoPlayerFileCaching,
            vcp: VideoCachePolicy,
            dp: DatabaseProvider,
            c: ChartboostCacheEvictor.EvictUrlCallback,
        ->
        cache(fc, dp, vcp, c)
    },
    val cacheDataSourceFactoryFactory: CacheDataSourceFactoryFactory = ::cacheDataSourceFactory,
    val httpDataSourceFactory: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory(),
    val downloadManagerFactory: DownloadManagerFactory = {
            c: Context,
            dp: DatabaseProvider,
            ca: Cache,
            hf: HttpDataSource.Factory,
            l: DownloadManager.Listener,
        ->
        downloadManager(c, dp, ca, hf, l)
    },
    val databaseProviderFactory: (Context) -> DatabaseProvider = ::databaseProvider,
    val setCookieHandler: () -> Unit = ::setCookieHandler,
    val fakePrecacheFilesManagerFactory: FakePrecacheFilesManagerFactory = { fc ->
        FakePrecacheFilesManager(
            fc,
        )
    },
)
