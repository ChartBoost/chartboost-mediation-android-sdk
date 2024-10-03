package com.chartboost.sdk.internal.di

import android.content.Context
import android.view.SurfaceView
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.Prefetcher
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.Model.DeviceBodyFieldsFactory
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyBuilderImpl
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Model.VideoPreCachingModel
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.EndpointRepositoryImpl
import com.chartboost.sdk.internal.Networking.NetworkFactory
import com.chartboost.sdk.internal.Telephony.CarrierBuilder
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.identity.AmazonAdvertisingId
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.identity.GoogleAdvertisingId
import com.chartboost.sdk.internal.identity.IFA
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.player.AdsVideoPlayer
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.exoplayer.AdsExoPlayer
import com.chartboost.sdk.internal.video.player.mediaplayer.AdsMediaPlayer
import com.chartboost.sdk.internal.video.player.mediaplayer.VideoBuffer
import com.chartboost.sdk.internal.video.player.mediaplayer.VideoBufferFactory
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressSchedulerCoroutines
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressSchedulerFactory
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerDownloadManager
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerDownloadManagerImpl
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerMediaItemFactory
import com.chartboost.sdk.internal.video.repository.exoplayer.VideoRepositoryExoplayer
import com.chartboost.sdk.internal.video.repository.mediaplayer.VideoRepositoryMediaPlayer
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.tracking.Session
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

internal typealias AdsVideoPlayerFactory = (
    context: Context,
    surface: SurfaceView,
    callback: AdsVideoPlayerListener?,
    uiPoster: UiPoster,
    fileCache: FileCache,
) -> AdsVideoPlayer

private typealias SDKConfigFactory = (AndroidComponent) -> SdkConfiguration

internal interface ApplicationComponent {
    val networkService: CBNetworkService
    val timeSource: TimeSource
    val session: Session
    val reachability: CBReachability
    val identity: CBIdentity
    val sdkConfig: AtomicReference<SdkConfiguration>
    val fileCache: FileCache
    val downloader: Downloader
    val carrierBuilder: CarrierBuilder
    val videoRepository: VideoRepository
    val videoCachePolicy: VideoCachePolicy
    val adsVideoPlayerFactory: AdsVideoPlayerFactory
    val tempFileDownloadHelper: TempFileDownloadHelper
    val requestBodyBuilder: RequestBodyBuilder
    val prefetcher: Prefetcher
    val privacyApi: PrivacyApi
    val exoPlayerDownloadManager: ExoPlayerDownloadManager
    val exoPlayerMediaItemFactory: ExoPlayerMediaItemFactory
    val intentResolver: IntentResolver
    val deviceBodyFieldsFactory: DeviceBodyFieldsFactory
    val endpointRepository: EndpointRepository
}

private val SDK_CONFIG_FACTORY: SDKConfigFactory = {
    val configJson: JSONObject =
        try {
            val config: String = it.sharedPreferences.getString(CBConstants.CONFIG_KEY, "{}") ?: "{}"
            JSONObject(config)
        } catch (ex: Exception) {
            Logger.e("Error reading config from shared preferences", ex)
            JSONObject()
        }
    SdkConfiguration(configJson)
}

internal class ApplicationModule(
    androidComponent: AndroidComponent,
    executorComponent: ExecutorComponent,
    privacyComponent: PrivacyComponent,
    sdkConfigFactory: SDKConfigFactory = SDK_CONFIG_FACTORY,
    trackerComponent: TrackerComponent,
) : ApplicationComponent {
    override val prefetcher: Prefetcher by lazy {
        Prefetcher(
            downloader,
            fileCache,
            networkService,
            requestBodyBuilder,
            sdkConfig,
            trackerComponent.eventTracker,
            endpointRepository,
        )
    }

    override val privacyApi: PrivacyApi by lazy { privacyComponent.privacyApi }

    override val requestBodyBuilder: RequestBodyBuilderImpl by lazy {
        RequestBodyBuilderImpl(
            androidComponent.context,
            identity,
            reachability,
            sdkConfig,
            androidComponent.sharedPreferences,
            timeSource,
            carrierBuilder,
            session,
            privacyComponent.privacyApi,
            null, // Mediation is only set in ad requests (cache and show)
            deviceBodyFieldsFactory,
        )
    }

    override val deviceBodyFieldsFactory: DeviceBodyFieldsFactory by lazy {
        DeviceBodyFieldsFactory(
            androidComponent.context,
            androidComponent.displayMeasurement,
            androidComponent.deviceFieldsWrapper,
        )
    }

    override val endpointRepository: EndpointRepository by lazy {
        EndpointRepositoryImpl(
            sdkConfiguration = sdkConfig.get(),
        )
    }

    override val networkService: CBNetworkService by lazy {
        CBNetworkService(
            executorComponent.backgroundExecutor,
            networkFactory,
            reachability,
            timeSource,
            androidComponent.uiPoster,
            executorComponent.networkExecutor,
            trackerComponent.eventTracker,
        )
    }

    override val timeSource: TimeSource by lazy {
        TimeSource()
    }

    override val session: Session by lazy {
        Session(androidComponent.sharedPreferences)
    }
    override val reachability: CBReachability by lazy { CBReachability(androidComponent.context) }

    override val identity: CBIdentity by lazy {
        CBIdentity(
            androidComponent.context,
            androidComponent.android,
            ifa,
            androidComponent.base64Wrapper,
        )
    }

    override val fileCache: FileCache by lazy {
        FileCache(androidComponent.context, sdkConfig)
    }

    override val sdkConfig: AtomicReference<SdkConfiguration> by lazy {
        AtomicReference<SdkConfiguration>(sdkConfigFactory(androidComponent))
    }

    private val networkFactory: NetworkFactory by lazy {
        NetworkFactory()
    }

    override val downloader: Downloader by lazy {
        Downloader(
            executorComponent.backgroundExecutor,
            fileCache,
            networkService,
            reachability,
            sdkConfig,
            timeSource,
            trackerComponent.eventTracker,
        )
    }

    override val carrierBuilder: CarrierBuilder by lazy {
        CarrierBuilder()
    }

    override val tempFileDownloadHelper: TempFileDownloadHelper by lazy {
        TempFileDownloadHelper()
    }

    override val exoPlayerDownloadManager: ExoPlayerDownloadManager by lazy {
        ExoPlayerDownloadManagerImpl()
    }

    override val exoPlayerMediaItemFactory: ExoPlayerMediaItemFactory by lazy {
        ExoPlayerMediaItemFactory(
            downloadManager = exoPlayerDownloadManager,
        )
    }

    override val intentResolver: IntentResolver by lazy {
        IntentResolver(
            androidComponent.context.packageManager,
        )
    }

    private val videoPlayerType: VideoPreCachingModel.VideoPlayerType by lazy {
        (sdkConfig.get()?.precacheConfig?.videoPlayer ?: VideoPreCachingModel.VideoPlayerType.EXO_PLAYER)
            .apply {
                Logger.d("Video player type: $this")
            }
    }

    override val videoRepository: VideoRepository
        get() =
            when (videoPlayerType) {
                VideoPreCachingModel.VideoPlayerType.MEDIA_PLAYER -> videoRepositoryMediaPlayer
                VideoPreCachingModel.VideoPlayerType.EXO_PLAYER -> videoRepositoryExoplayer
            }.apply {
                Logger.d("Video repository: $this")
            }

    private val videoRepositoryMediaPlayer: VideoRepository by lazy {
        VideoRepositoryMediaPlayer(
            networkService,
            videoCachePolicy,
            reachability,
            fileCache,
            tempFileDownloadHelper,
            executorComponent.backgroundExecutor,
        )
    }

    private val videoRepositoryExoplayer: VideoRepository by lazy {
        VideoRepositoryExoplayer(
            downloadManager = exoPlayerDownloadManager,
            policy = videoCachePolicy,
        )
    }

    // Keep as singleton to be able to update it everywhere when the SDK config is refreshed
    override val videoCachePolicy: VideoCachePolicy by lazy {
        with(VideoPreCachingModel()) {
            VideoCachePolicy(
                maxBytes,
                maxUnitsPerTimeWindow,
                maxUnitsPerTimeWindowCellular,
                timeWindow,
                timeWindowCellular,
                ttl,
                bufferSize,
                reachability,
            )
        }
    }

    override val adsVideoPlayerFactory: AdsVideoPlayerFactory
        get() =
            when (videoPlayerType) {
                VideoPreCachingModel.VideoPlayerType.MEDIA_PLAYER -> adsMediaPlayerFactory
                VideoPreCachingModel.VideoPlayerType.EXO_PLAYER -> adsExoPlayerFactory
            }

    private val adsMediaPlayerFactory: AdsVideoPlayerFactory by lazy {
        { _, s, cb, h, fc ->
            AdsMediaPlayer(
                surface = s,
                callback = cb,
                uiPoster = h,
                fileCache = fc,
                videoProgressFactory = videoProgressSchedulerFactory,
                videoBufferFactory = videoBufferFactory,
            )
        }
    }

    private val adsExoPlayerFactory: AdsVideoPlayerFactory by lazy {
        { cxt, s, cb, h, _ ->
            AdsExoPlayer(
                context = cxt,
                exoPlayerMediaItemFactory = exoPlayerMediaItemFactory,
                surfaceView = s,
                callback = cb,
                uiPoster = h,
                videoProgressFactory = videoProgressSchedulerFactory,
            )
        }
    }

    private val videoProgressSchedulerFactory: VideoProgressSchedulerFactory by lazy {
        { l, vp, _ ->
            VideoProgressSchedulerCoroutines(
                callback = l,
                videoProgress = vp,
            )
        }
    }

    private val videoBufferFactory: VideoBufferFactory by lazy {
        { va, l, d, fc ->
            VideoBuffer(
                videoAsset = va,
                listener = l,
                coroutineDispatcher = d,
                fileCache = fc,
            )
        }
    }

    private val ifa: IFA by lazy {
        IFA(
            googleAdvertisingId,
            amazonAdvertisingId,
        )
    }

    private val googleAdvertisingId: GoogleAdvertisingId by lazy {
        GoogleAdvertisingId(androidComponent.context)
    }

    private val amazonAdvertisingId: AmazonAdvertisingId by lazy {
        AmazonAdvertisingId(
            androidComponent.context,
            androidComponent.contentResolver,
        )
    }
}
