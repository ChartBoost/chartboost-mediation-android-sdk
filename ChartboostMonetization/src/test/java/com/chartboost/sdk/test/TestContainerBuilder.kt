package com.chartboost.sdk.test

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.telephony.TelephonyManager
import com.chartboost.sdk.Chartboost.startWithAppId
import com.chartboost.sdk.LoggingLevel
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.Prefetcher
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.DeviceBodyFields
import com.chartboost.sdk.internal.Model.DeviceBodyFieldsFactory
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.Model.PrivacyBodyFields
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyBuilderImpl
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Model.SessionBodyFields
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.ConnectionType
import com.chartboost.sdk.internal.Networking.ManualNetworkExecutorService
import com.chartboost.sdk.internal.Networking.MockNetworkFactory
import com.chartboost.sdk.internal.Networking.MockNetworkResponses
import com.chartboost.sdk.internal.Networking.NetworkFactory
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import com.chartboost.sdk.internal.Telephony.Carrier
import com.chartboost.sdk.internal.Telephony.CarrierBuilder
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlRedirect
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.identity.TrackingState
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.utils.DeviceInfo
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.CBConfig
import com.chartboost.sdk.mock.android.SharedPreferencesMockWrapper
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.tracking.EventTracker
import com.chartboost.sdk.tracking.Session
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import org.json.JSONObject
import org.junit.Assert
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.internal.util.MockUtil
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicReference

// TODO This should be removed. There should be no need for dependency injection in unit testing,
//  all dependencies should be passed through constructor.
//  Singletons and statics should be mocked or refactored to be passed as dependencies.
internal class TestContainerBuilder(androidTestContainer: AndroidTestContainer) {
    @JvmField
    val androidTestContainer: AndroidTestContainer

    @JvmField
    var appId = "appid"

    @JvmField
    val appSignature = "signature"

    @JvmField
    val control: TestContainerControl

    @JvmField
    val responses: MockNetworkResponses

    @JvmField
    val mockNetworkFactory: MockNetworkFactory

    @JvmField
    val rewardedTraits: AdType

    @JvmField
    val interstitialTraits: AdType

    @JvmField
    val backgroundExecutor: ManualExecutorService

    @JvmField
    val networkExecutor: ManualNetworkExecutorService

    @JvmField
    var fileCache: FileCache

    @JvmField
    var downloader: Downloader

    @JvmField
    var identity: CBIdentity = mockk()

    @JvmField
    var networkFactory: NetworkFactory

    @JvmField
    var networkService: CBNetworkService

    @JvmField
    var prefetcher: Prefetcher

    @JvmField
    var reachability: CBReachability

    @JvmField
    var requestBodyBuilder: RequestBodyBuilder

    @JvmField
    var sdkConfigurationMock: SdkConfiguration

    @JvmField
    var sdkConfig: AtomicReference<SdkConfiguration>

    @JvmField
    val sharedPreferences: SharedPreferences

    @JvmField
    var urlResolver: UrlResolver

    @JvmField
    var urlRedirect: UrlRedirect

    @JvmField
    var intentResolver: IntentResolver

    @JvmField
    var privacyApi: PrivacyApi

    @JvmField
    var carrierBuilder: CarrierBuilder

    @JvmField
    var session: Session

    @JvmField
    var videoRepository: VideoRepository

    @JvmField
    var templateProxy: CBTemplateProxy

    val deviceBodyFieldsFactory = mockk<DeviceBodyFieldsFactory>()

    var context: Context
    var startCallback: StartCallback? = null

    private val eventTrackerMock =
        mockk<EventTracker>().apply {
            justRun { track(any()) }
        }

    @JvmField
    var mediation: Mediation

    @JvmField
    var openMeasurementImpressionCallback: OpenMeasurementImpressionCallback

    @JvmField
    var adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback

    private var installedMockNetworkFactoryAndNetworkService = false

    constructor(control: TestContainerControl?) : this(AndroidTestContainerBuilder(control).build()) {}

    fun withResponse(response: ResponseDescriptor?): TestContainerBuilder {
        responses.add(response)
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    fun withResponse(assetDescriptor: AssetDescriptor?): TestContainerBuilder {
        responses.add(assetDescriptor)
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    fun withResponses(assetDescriptors: List<AssetDescriptor?>?): TestContainerBuilder {
        responses.add(assetDescriptors)
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    fun withResponses(assetDescriptors: Array<AssetDescriptor?>?): TestContainerBuilder {
        responses.add(assetDescriptors)
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    fun withNotFound(descriptors: List<AssetDescriptor>): TestContainerBuilder {
        for (descriptor in descriptors) {
            responses.add(descriptor.notFound())
        }
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    private fun installMockNetworkFactoryAndNetworkService() {
        if (!installedMockNetworkFactoryAndNetworkService) {
            installedMockNetworkFactoryAndNetworkService = true
            networkFactory = mockNetworkFactory
            networkService =
                CBNetworkService(
                    backgroundExecutor,
                    networkFactory,
                    reachability,
                    androidTestContainer.testTimeSource,
                    androidTestContainer.uiPoster,
                    networkExecutor,
                    eventTrackerMock,
                )
        }
    }

    fun withSpyOnNetworkService(): TestContainerBuilder {
        networkService = Mockito.spy(networkService)
        return this
    }

    fun build(): TestContainer {
        Logger.level = LoggingLevel.INTEGRATION
        CBConfig.validatePermissions(androidTestContainer.applicationContext)
        return TestContainer(this)
    }

    fun startSdkWithDelegate(): TestContainer {
        return startSdk()
    }

    fun startSdk(): TestContainer {
        simulateAndroidManifest()
        startCallback = Mockito.mock(StartCallback::class.java)
        androidTestContainer.factory.installIntercept(
            NetworkFactory::class.java,
            mockNetworkFactory,
        )
        androidTestContainer.factory.installIntercept(
            ScheduledThreadPoolExecutor::class.java,
            backgroundExecutor,
        )
        androidTestContainer.factory.installIntercept(
            ThreadPoolExecutor::class.java,
            networkExecutor,
        )
        startWithAppId(androidTestContainer.applicationContext, appId, appSignature, startCallback!!)
        val tc = TestContainer(this)
        tc.run()
        return tc
    }

    private fun simulateAndroidManifest() {
        val dummyResolveInfo = Mockito.mock(ResolveInfo::class.java)
        val dummyResolveInfoList = listOf(dummyResolveInfo)

        // Android Studio underlines eq(PackageManager.MATCH_DEFAULT_ONLY) and I don't know how to suppress that.
        Mockito.`when`(
            androidTestContainer.packageManager.queryIntentActivities(
                ArgumentMatchers.any(
                    Intent::class.java,
                ),
                ArgumentMatchers.eq(PackageManager.MATCH_DEFAULT_ONLY),
            ),
        ).thenReturn(dummyResolveInfoList)
        val metaDataPackageInfo =
            Mockito.mock(
                PackageInfo::class.java,
            )
        val constants = control.constants
        metaDataPackageInfo.versionName = constants.packageVersionName
        try {
            Mockito.`when`(
                androidTestContainer.packageManager.getPackageInfo(
                    constants.packageName,
                    PackageManager.GET_META_DATA,
                ),
            ).thenReturn(metaDataPackageInfo)
        } catch (ex: PackageManager.NameNotFoundException) {
            throw Error(ex)
        }
    }

    fun withDownloader(): TestContainerBuilder {
        downloader =
            Downloader(
                backgroundExecutor,
                fileCache,
                networkService,
                reachability,
                sdkConfig,
                androidTestContainer.testTimeSource,
                eventTrackerMock,
            )
        return this
    }

    // Use this with startSdk() and startSdkWithDelegate(), so that the factory.intercept() calls
    // in the Sdk constructor know to spy on the instance.
    fun withSpyOnClass(cls: Class<*>?): TestContainerBuilder {
        androidTestContainer.factory.spyOnClass(cls)
        return this
    }

    fun withNetworkService(): TestContainerBuilder {
        installMockNetworkFactoryAndNetworkService()
        return this
    }

    fun withReachabilityConnectionType(connectionType: ConnectionType): TestContainerBuilder =
        apply {
            every { reachability.connectionTypeFromActiveNetwork() } returns connectionType
        }

    private fun assertIsMock(
        message: String,
        instance: Any,
    ) {
        Assert.assertTrue(message, MockUtil.isMock(instance))
        Assert.assertFalse(message, MockUtil.isSpy(instance))
    }

    companion object {
        @JvmStatic
        fun emptyConfig(): TestContainerBuilder {
            return TestContainerBuilder(TestContainerControl.emptyConfig())
        }

        @JvmStatic
        fun defaultWebView(): TestContainerBuilder {
            return TestContainerBuilder(TestContainerControl.defaultWebView())
        }

        @JvmStatic
        fun forFormat(): TestContainerBuilder {
            return defaultWebView()
        }
    }

    init {
        Logger.level = LoggingLevel.INTEGRATION
        this.androidTestContainer = androidTestContainer
        context = androidTestContainer.applicationContext
        control = androidTestContainer.control
        responses = MockNetworkResponses()
        mockNetworkFactory = MockNetworkFactory(androidTestContainer.testTimeSource, responses)
        backgroundExecutor =
            ManualExecutorService(
                androidTestContainer.testTimeSource,
                androidTestContainer.runnablesToRunAfterAnyRunnable,
            )
        networkExecutor =
            ManualNetworkExecutorService(androidTestContainer.runnablesToRunAfterAnyRunnable)
        sharedPreferences =
            SharedPreferencesMockWrapper(control.sharedPreferenceValues).mockSharedPreferences
        networkFactory = Mockito.mock(NetworkFactory::class.java)
        networkService = Mockito.mock(CBNetworkService::class.java)

        reachability =
            mockk<CBReachability>().apply {
                every { isNetworkAvailable } returns true
                every { cellularConnectionType() } returns TelephonyManager.NETWORK_TYPE_CDMA
                every { connectionTypeFromActiveNetwork() } returns ConnectionType.CONNECTION_UNKNOWN
                every { openRTBConnectionType() } returns NetworkType.CELLULAR_UNKNOWN
                every { connectionTypeAsString() } returns openRTBConnectionType().asString
            }

        sdkConfigurationMock = Mockito.mock(SdkConfiguration::class.java)
        Mockito.lenient().`when`(sdkConfigurationMock.isWebviewEnabled).thenReturn(true)
        sdkConfig = AtomicReference(null)
        sdkConfig.set(sdkConfigurationMock)
        control.sharedPreferenceValues["config"] = control.configure().config.toString()
        Assert.assertTrue(CBConfig.updateConfig(sdkConfig, control.configure().config))
        fileCache = FileCache(androidTestContainer.applicationContext, sdkConfig)
        setIdentityTracking(TrackingState.TRACKING_ENABLED)
        downloader = Mockito.mock(Downloader::class.java)
        prefetcher = mockk<Prefetcher>()
        urlResolver = mockk<UrlResolver>()
        urlRedirect = mockk<UrlRedirect>()
        intentResolver = mockk<IntentResolver>()
        privacyApi = Mockito.mock(PrivacyApi::class.java)
        carrierBuilder = Mockito.mock(CarrierBuilder::class.java)
        session = Mockito.mock(Session::class.java)
        Mockito.lenient().`when`(session.toSessionBodyFields()).thenReturn(SessionBodyFields())
        videoRepository = mockk()
        every { videoRepository.initialize(context) } just runs
        every { videoRepository.downloadVideoFile(any(), any(), any(), any()) } just runs
        every { videoRepository.startDownloadIfPossible(any(), any(), any()) } just runs
//        every { videoRepository.createRandomAccessFile(any()) } returns mock()
        every { videoRepository.isFileDownloadingOrDownloaded(any()) } returns false
        every { videoRepository.getVideoAsset(any()) } returns null
        every { videoRepository.getVideoDownloadState(any()) } returns 0
        every { videoRepository.removeAsset(any()) } returns false

        templateProxy = mockk()
        every { templateProxy.callOnBackgroundJSFunction(any(), any(), any()) } just runs
        every { templateProxy.callOnForegroundJSFunction(any(), any(), any()) } just runs
        every { templateProxy.callOnPlaybackTimeJSFunction(any(), any(), any(), any()) } just runs
        every { templateProxy.callOnVideoFailedJSFunction(any(), any(), any()) } just runs
        every { templateProxy.callOnVideoStartedJSFunction(any(), any(), any(), any()) } just runs
        every { templateProxy.callOnVideoEndedJSFunction(any(), any(), any()) } just runs

        if (MockUtil.isMock(androidTestContainer.applicationContext)) {
            Mockito.lenient().`when`(
                androidTestContainer.applicationContext.getSharedPreferences(
                    CBConstants.PREFERENCES_FILE_DEFAULT,
                    Context.MODE_PRIVATE,
                ),
            ).thenReturn(sharedPreferences)

            Mockito.lenient().`when`(
                androidTestContainer.applicationContext.getSharedPreferences(
                    CBConstants.PREFERENCES_FILE_TRACKING,
                    Context.MODE_PRIVATE,
                ),
            ).thenReturn(sharedPreferences)
        }
        interstitialTraits = AdType.Interstitial
        rewardedTraits = AdType.Rewarded
        val carrier =
            Carrier(
                control.constants.carrierName,
                control.constants.carrierMobileCountryCode,
                control.constants.carrierMobileNetworkCode,
                control.constants.carrierName,
                control.constants.carrierIsoCountryCode,
                control.constants.carrierPhoneType,
            )
        Mockito.lenient().`when`(carrierBuilder.build(ArgumentMatchers.any())).thenReturn(carrier)
        val privacyBodyFields = PrivacyBodyFields(0, ArrayList(), 0, null, JSONObject(), "-1")
        Mockito.lenient().`when`(privacyApi.toPrivacyBodyFields()).thenReturn(privacyBodyFields)
        mediation = Mockito.mock(Mediation::class.java)
        Mockito.lenient().`when`(mediation.toMediationBodyFields()).thenReturn(null)

        openMeasurementImpressionCallback = mockk()
        adUnitRendererImpressionCallback = mockk()
        every { openMeasurementImpressionCallback.onImpressionDestroyWebview() } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVideoStarted(any(), any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionOnWebviewPageStarted(any(), any(), any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVideoBuffer(any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVideoProgress(any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVideoComplete() } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVideoSkipped() } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyVolumeChanged(any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyStateChanged(any()) } just Runs
        every { openMeasurementImpressionCallback.onImpressionNotifyClick() } just Runs

        every { prefetcher.prefetch() } just Runs

        val deviceBodyFields =
            DeviceBodyFields(
                deviceWidth = 768,
                deviceHeight = 1024,
                width = 768,
                height = 1024,
                scale = 1f,
                dpi = "848",
                ortbDeviceType = DeviceInfo.OPENRTB_DEVICE_PHONE,
                deviceType = "phone",
                packageName = "com.some.package.name",
                versionName = "3.2.1",
                isPortrait = true,
            )

        every { deviceBodyFieldsFactory.build() } returns deviceBodyFields

        requestBodyBuilder =
            RequestBodyBuilderImpl(
                context,
                identity,
                reachability,
                sdkConfig,
                sharedPreferences,
                androidTestContainer.testTimeSource,
                carrierBuilder,
                session,
                privacyApi,
                mediation,
                deviceBodyFieldsFactory,
            )
    }

    fun setIdentityTracking(trackingState: TrackingState) {
        val identifierWithNextLine =
            "eyJnYWlkIjoiODI4YWFmZTgtYjU2NS00ODFjLThhMGEtMTBjYTFhZDFhYmUxIn0="
        val identityBodyFields =
            IdentityBodyFields(
                trackingState,
                identifierWithNextLine,
                "a uuid",
                "a gaid",
                "a setId",
                1,
            )
        identity = mockk()
        every { identity.toIdentityBodyFields() }.answers { identityBodyFields }
    }

    fun rebuildRequestBodyBuilderImpl() {
        requestBodyBuilder =
            RequestBodyBuilderImpl(
                context,
                identity,
                reachability,
                sdkConfig,
                sharedPreferences,
                androidTestContainer.testTimeSource,
                carrierBuilder,
                session,
                privacyApi,
                mediation,
                deviceBodyFieldsFactory,
            )
    }
}
