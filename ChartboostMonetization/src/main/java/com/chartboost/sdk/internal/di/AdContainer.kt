package com.chartboost.sdk.internal.di

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetsDownloader
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetsDownloaderImpl
import com.chartboost.sdk.internal.AdUnitManager.impression.ImpressionBuilder
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdLoader
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdLoaderImpl
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoader
import com.chartboost.sdk.internal.AdUnitManager.loaders.OrtbLoader
import com.chartboost.sdk.internal.AdUnitManager.parsers.AdUnitParser
import com.chartboost.sdk.internal.AdUnitManager.parsers.OpenRTBAdUnitParser
import com.chartboost.sdk.internal.AdUnitManager.parsers.SDKBiddingTemplateParser
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRenderer
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererShowRequest
import com.chartboost.sdk.internal.AssetLoader.TemplateLoader
import com.chartboost.sdk.internal.BannerApi
import com.chartboost.sdk.internal.InterstitialApi
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyBuilderImpl
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.RewardedApi
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.api.AdApiCallbackSender
import com.chartboost.sdk.internal.clickthrough.UrlParser
import com.chartboost.sdk.internal.clickthrough.UrlRedirect
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.impression.ImpressionViewProtocolBuilder
import com.chartboost.sdk.internal.measurement.OpenMeasurementController
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.Session
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

internal fun createInterstitialApi(mediation: Mediation?): InterstitialApi {
    return AdApiFactory(AdType.Interstitial, { ::InterstitialApi }, mediation).build()
}

internal fun createRewardedApi(mediation: Mediation?): RewardedApi {
    return AdApiFactory(AdType.Rewarded, { ::RewardedApi }, mediation).build()
}

internal fun createBannerApi(mediation: Mediation?): BannerApi {
    return AdApiFactory(AdType.Banner, { ::BannerApi }, mediation).build()
}

internal interface AdUnitManagerComponent {
    val adUnitLoader: AdUnitLoader
    val adUnitRenderer: AdUnitRenderer
}

private typealias ApiFactoryGet<T> = () -> (
    AdUnitLoader,
    AdUnitRenderer,
    UiPoster,
    AtomicReference<SdkConfiguration>,
    ScheduledExecutorService,
    AdApiCallbackSender,
    Session,
    Base64Wrapper,
    EventTrackerExtensions,
) -> T

internal class AdApiFactory<T>(
    adType: AdType,
    private val get: ApiFactoryGet<T>,
    private val mediation: Mediation?,
    private val dependencyContainer: ChartboostDependencyContainer = ChartboostDependencyContainer,
) {
    private val adUnitManagerModule by lazy {
        AdUnitManagerModule(
            dependencyContainer.androidComponent,
            dependencyContainer.applicationComponent,
            adType,
            dependencyContainer.renderComponent,
            dependencyContainer.openMeasurementComponent,
            mediation,
            dependencyContainer.impressionComponent,
            dependencyContainer.trackerComponent,
        )
    }

    private val adUnitLoader = adUnitManagerModule.adUnitLoader

    private val adUnitRenderer = adUnitManagerModule.adUnitRenderer

    fun build(): T =
        get()(
            adUnitLoader,
            adUnitRenderer,
            uiPoster,
            sdkConfig,
            executor,
            adApiCallbackSender,
            session,
            base64Wrapper,
            dependencyContainer.trackerComponent.eventTracker,
        )

    private val uiPoster = dependencyContainer.androidComponent.uiPoster

    private val sdkConfig: AtomicReference<SdkConfiguration> by lazy {
        dependencyContainer.applicationComponent.sdkConfig
    }

    private val executor = dependencyContainer.executorComponent.backgroundExecutor

    private val session = dependencyContainer.applicationComponent.session

    private val base64Wrapper = dependencyContainer.androidComponent.base64Wrapper

    private val adApiCallbackSender =
        AdApiCallbackSenderModule(dependencyContainer.androidComponent).adApiCallbackSender
}

internal interface AdApiCallbackSenderComponent {
    val adApiCallbackSender: AdApiCallbackSender
}

internal class AdApiCallbackSenderModule(private val androidComponent: AndroidComponent) :
    AdApiCallbackSenderComponent {
    override val adApiCallbackSender: AdApiCallbackSender by lazy {
        AdApiCallbackSender(androidComponent.uiPoster)
    }
}

internal class AdUnitManagerModule(
    private val androidComponent: AndroidComponent,
    private val applicationComponent: ApplicationComponent,
    private val adType: AdType,
    private val renderComponent: RenderComponent,
    private val openMeasurementComponent: OpenMeasurementComponent,
    private val mediation: Mediation?,
    private val impressionComponent: ImpressionComponent,
    private val trackerComponent: TrackerComponent,
) : AdUnitManagerComponent {
    private val assetsDownloader: AssetsDownloader by lazy {
        AssetsDownloaderImpl(
            applicationComponent.downloader,
            applicationComponent.timeSource,
            applicationComponent.videoRepository,
            adType,
            mediation,
        )
    }

    private val cbTemplateProxy: CBTemplateProxy by lazy {
        CBTemplateProxy(trackerComponent.eventTracker)
    }

    private val urlRedirect: UrlRedirect by lazy {
        UrlRedirect()
    }

    private val urlResolver: UrlResolver by lazy {
        UrlResolver(urlRedirect)
    }

    private val requestBodyBuilder: RequestBodyBuilder by lazy {
        RequestBodyBuilderImpl(
            androidComponent.context,
            applicationComponent.identity,
            applicationComponent.reachability,
            applicationComponent.sdkConfig,
            androidComponent.sharedPreferences,
            applicationComponent.timeSource,
            applicationComponent.carrierBuilder,
            applicationComponent.session,
            applicationComponent.privacyApi,
            mediation,
            applicationComponent.deviceBodyFieldsFactory,
        )
    }

    private val adLoader: AdLoader by lazy {
        AdLoaderImpl(
            adType,
            applicationComponent.fileCache,
            requestBodyBuilder,
            applicationComponent.networkService,
            AdUnitParser(androidComponent.base64Wrapper),
            openRTBAdUnitParser,
            openMeasurementComponent.openMeasurementManager,
            eventTracker = trackerComponent.eventTracker,
            endpointRepository = applicationComponent.endpointRepository,
        )
    }

    private val ortbLoader: OrtbLoader by lazy {
        OrtbLoader(
            adType,
            applicationComponent.downloader,
            openRTBAdUnitParser,
            eventTracker = trackerComponent.eventTracker,
        )
    }

    private val openRTBAdUnitParser: OpenRTBAdUnitParser by lazy {
        OpenRTBAdUnitParser(androidComponent.base64Wrapper)
    }

    private val sdkBiddingTemplateParser: SDKBiddingTemplateParser by lazy {
        SDKBiddingTemplateParser()
    }

    private val impressionBuilder: ImpressionBuilder by lazy {
        ImpressionBuilder(
            applicationComponent.fileCache,
            applicationComponent.downloader,
            urlResolver,
            applicationComponent.intentResolver,
            adType,
            applicationComponent.networkService,
            applicationComponent.requestBodyBuilder,
            mediation,
            openMeasurementComponent.openMeasurementManager,
            sdkBiddingTemplateParser,
            openMeasurementController,
            impressionComponent.impressionFactory,
            trackerComponent.eventTracker,
            endpointRepository = applicationComponent.endpointRepository,
        )
    }

    private val viewProtocolBuilder: ImpressionViewProtocolBuilder by lazy {
        ImpressionViewProtocolBuilder(
            androidComponent.context,
            androidComponent.uiPoster,
            applicationComponent.fileCache,
            cbTemplateProxy,
            applicationComponent.videoRepository,
            mediation,
            applicationComponent.networkService,
            openMeasurementController,
            eventTracker = trackerComponent.eventTracker,
        )
    }

    private val adUnitRendererShowRequest: AdUnitRendererShowRequest by lazy {
        AdUnitRendererShowRequest(
            applicationComponent.networkService,
            applicationComponent.requestBodyBuilder,
            trackerComponent.eventTracker,
        )
    }

    override val adUnitLoader: AdUnitLoader
        get() {
            return AdUnitLoader(
                adType,
                applicationComponent.fileCache,
                applicationComponent.reachability,
                applicationComponent.videoRepository,
                assetsDownloader,
                adLoader,
                ortbLoader,
                mediation,
                eventTracker = trackerComponent.eventTracker,
            )
        }

    override val adUnitRenderer: AdUnitRenderer
        get() {
            return AdUnitRenderer(
                adType,
                applicationComponent.reachability,
                applicationComponent.fileCache,
                applicationComponent.videoRepository,
                impressionBuilder,
                adUnitRendererShowRequest,
                openMeasurementController,
                viewProtocolBuilder,
                renderComponent.rendererActivityBridge,
                nativeBridgeCommand,
                templateLoader,
                mediation,
                eventTracker = trackerComponent.eventTracker,
                endpointRepository = applicationComponent.endpointRepository,
            )
        }

    private val nativeBridgeCommand: NativeBridgeCommand by lazy {
        NativeBridgeCommand(
            androidComponent.uiPoster,
            urlParser,
        )
    }

    private val templateLoader: TemplateLoader by lazy {
        TemplateLoader(
            eventTracker = trackerComponent.eventTracker,
        )
    }

    private val urlParser: UrlParser by lazy {
        UrlParser()
    }

    private val openMeasurementController: OpenMeasurementController by lazy {
        openMeasurementComponent.openMeasurementController
    }
}
