package com.chartboost.sdk.internal.di

import com.chartboost.sdk.internal.AnalyticsApi
import com.chartboost.sdk.internal.ChartboostApi
import com.chartboost.sdk.internal.Libraries.BidderTokenGenerator
import com.chartboost.sdk.internal.initialization.InitConfigRequest
import com.chartboost.sdk.internal.initialization.InitInstallRequest
import com.chartboost.sdk.internal.initialization.ProviderInstallerHelper
import com.chartboost.sdk.internal.initialization.SdkInitializer

internal interface SdkComponent {
    val chartboostApi: ChartboostApi
    val analyticsApi: AnalyticsApi
    val sdkInitializer: SdkInitializer
    val tokenGenerator: BidderTokenGenerator
}

internal class SdkModule(
    androidComponent: AndroidComponent,
    executorComponent: ExecutorComponent,
    applicationComponent: ApplicationComponent,
    openMeasurementComponent: OpenMeasurementComponent,
    trackerComponent: TrackerComponent,
) : SdkComponent {
    override val chartboostApi: ChartboostApi by lazy {
        ChartboostApi(
            androidComponent.context,
            executorComponent.backgroundExecutor,
            sdkInitializer,
            tokenGenerator,
            applicationComponent.identity,
        )
    }

    override val analyticsApi: AnalyticsApi by lazy {
        AnalyticsApi(
            sdkInitializer,
            applicationComponent.networkService,
            applicationComponent.requestBodyBuilder,
            eventTracker = trackerComponent.eventTracker,
        )
    }

    override val sdkInitializer: SdkInitializer by lazy {
        SdkInitializer(
            androidComponent.context,
            androidComponent.sharedPreferences,
            androidComponent.uiPoster,
            applicationComponent.privacyApi,
            applicationComponent.sdkConfig,
            applicationComponent.prefetcher,
            applicationComponent.downloader,
            applicationComponent.session,
            applicationComponent.videoCachePolicy,
            lazy { applicationComponent.videoRepository },
            initInstallRequest,
            initConfigRequest,
            applicationComponent.reachability,
            providerInstallerHelper,
            applicationComponent.identity,
            openMeasurementComponent.openMeasurementManager,
        )
    }

    private val initInstallRequest: InitInstallRequest by lazy {
        InitInstallRequest(
            networkService = applicationComponent.networkService,
            requestBodyBuilder = applicationComponent.requestBodyBuilder,
            eventTracker = trackerComponent.eventTracker,
            endpointRepository = applicationComponent.endpointRepository,
        )
    }

    private val initConfigRequest: InitConfigRequest by lazy {
        InitConfigRequest(
            networkService = applicationComponent.networkService,
            requestBodyBuilder = applicationComponent.requestBodyBuilder,
            eventTracker = trackerComponent.eventTracker,
            endpointRepository = applicationComponent.endpointRepository,
        )
    }

    private val providerInstallerHelper: ProviderInstallerHelper by lazy {
        ProviderInstallerHelper(
            androidComponent.context,
            androidComponent.uiPoster,
        )
    }

    override val tokenGenerator: BidderTokenGenerator by lazy {
        BidderTokenGenerator(
            androidComponent.context,
            androidComponent.base64Wrapper,
            applicationComponent.identity,
            applicationComponent.sdkConfig,
            openMeasurementComponent.openMeasurementManager,
        )
    }
}
