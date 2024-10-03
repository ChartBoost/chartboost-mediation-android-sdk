package com.chartboost.sdk.test

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.api.WebView.WebViewGetResponseBuilder
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.Prefetcher
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.Model.DeviceBodyFieldsFactory
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.ManualNetworkExecutorService
import com.chartboost.sdk.internal.Networking.MockNetworkFactory
import com.chartboost.sdk.internal.Networking.MockNetworkResponses
import com.chartboost.sdk.internal.Telephony.CarrierBuilder
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.WebView.CBMraidWebViewProtocol
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlRedirect
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.identity.TrackingState
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.CBConfig
import com.chartboost.sdk.mock.android.UiPosterScheduledExecutionQueue
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.Session
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// TODO This should be removed. There should be no need for dependency injection in unit testing,
//  all dependencies should be passed through constructor.
//  Singletons and statics should be mocked or refactored to be passed as dependencies.
internal class TestContainer
    @JvmOverloads
    constructor(
        val builder: TestContainerBuilder = TestContainerBuilder.emptyConfig(),
    ) : AutoCloseable {
        private val expectedExceptionGuard = ExpectedExceptionGuard()

        @JvmField
        val control: TestContainerControl = builder.control

        @JvmField
        val androidTestContainer: AndroidTestContainer = builder.androidTestContainer

        @JvmField
        val activity: Activity

        @JvmField
        val applicationContext: Context
        private val handlerMockWrapper: UiPosterScheduledExecutionQueue

        @JvmField
        val testTimeSource: TestTimeSource = androidTestContainer.testTimeSource
        val cacheDir: File

        @JvmField
        val internalBaseDir: File

        @JvmField
        val appId: String
        val appSignature: String

        @JvmField
        val responses: MockNetworkResponses

        @JvmField
        val mockNetworkFactory: MockNetworkFactory
        private val manualBackgroundExecutor: ManualExecutorService
        private val manualNetworkExecutor: ManualNetworkExecutorService
        val interstitialTraits: AdType
        private val rewardedTraits: AdType

        @JvmField
        val backgroundExecutor: Executor

        @JvmField
        val downloader: Downloader

        @JvmField
        val fileCache: FileCache

        @JvmField
        internal var identity: CBIdentity

        @JvmField
        val networkExecutor: Executor

        @JvmField
        val networkService: CBNetworkService

        @JvmField
        val prefetcher: Prefetcher

        @JvmField
        val reachability: CBReachability

        @JvmField
        var requestBodyBuilder: RequestBodyBuilder

        @JvmField
        val sdkConfig: AtomicReference<SdkConfiguration>

        @JvmField
        val sharedPreferences: SharedPreferences

        @JvmField
        val timeSource: TimeSource

        @JvmField
        internal val uiPoster: UiPoster

        @JvmField
        internal val urlResolver: UrlResolver

        @JvmField
        internal val urlRedirect: UrlRedirect

        @JvmField
        internal val intentResolver: IntentResolver

        @JvmField
        var privacyApi: PrivacyApi

        @JvmField
        val carrierBuilder: CarrierBuilder

        @JvmField
        var session: Session

        @JvmField
        val videoRepository: VideoRepository

        @JvmField
        internal val templateProxy: CBTemplateProxy

        @JvmField
        val mediation: Mediation

        @JvmField
        val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback

        @JvmField
        internal val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback

        @JvmField
        internal val deviceBodyFieldsFactory: DeviceBodyFieldsFactory

        fun getCacheFile(filename: String): File {
            return File(cacheDir, filename)
        }

        fun advanceUptime(
            n: Long,
            timeUnit: TimeUnit?,
        ) {
            testTimeSource.advanceUptime(n, timeUnit)
        }

        fun advanceUptimeMs(ms: Long) {
            advanceUptime(ms, TimeUnit.MILLISECONDS)
        }

        override fun close() {
            androidTestContainer.close()
            expectedExceptionGuard.close()
            cacheDir.delete()
            internalBaseDir.delete()
        }

        fun installConfig() {
            Assert.assertTrue(CBConfig.updateConfig(sdkConfig, control.configure().config))
        }

        internal fun minimalAdUnit(): AdUnit {
            return webviewInterstitialBuilder().toAdUnit()
        }

        internal fun impressionBuilder(traits: AdType?): ImpressionBuilder {
            return ImpressionBuilder(this, traits)
        }

        fun runNextNetworkRunnable() {
            Assert.assertNotNull(manualNetworkExecutor)
            manualNetworkExecutor.runNext()
        }

        fun runNextBackgroundRunnable(): Runnable {
            Assert.assertNotNull(manualBackgroundExecutor)
            return manualBackgroundExecutor.runNext()
        }

        private fun networkRunnableCount(): Int {
            return manualNetworkExecutor.queueSize()
        }

        fun backgroundRunnableCount(): Int {
            return manualBackgroundExecutor.readyCount()
        }

        private fun scheduledBackgroundRunnableCount(): Int {
            return manualBackgroundExecutor.allScheduledRunnablesCount()
        }

        private fun scheduledUiRunnableCount(): Int {
            return handlerMockWrapper.allScheduledRunnablesCount()
        }

        private fun uiRunnableCount(): Int {
            return handlerMockWrapper.readyCount()
        }

        fun runNextUiRunnable() {
            handlerMockWrapper.runNext()
        }

        fun run() {
            var iterations = 0
            while (manualBackgroundExecutor.readyCount() > 0 ||
                manualNetworkExecutor.queueSize() > 0 || handlerMockWrapper.readyCount() > 0
            ) {
                if (++iterations > 100) {
                    throw Error("More than 100 iterations. Likely an infinite loop.")
                }
                if (manualBackgroundExecutor.readyCount() > 0) runNextBackgroundRunnable()
                if (manualNetworkExecutor.queueSize() > 0) runNextNetworkRunnable()
                if (handlerMockWrapper.readyCount() > 0) runNextUiRunnable()
            }
        }

        fun verifyNoMoreRunnables() {
            MatcherAssert.assertThat(
                "have no network runnables",
                networkRunnableCount(),
                Matchers.`is`(0),
            )
            MatcherAssert.assertThat(
                "have no background runnables",
                backgroundRunnableCount(),
                Matchers.`is`(0),
            )
            MatcherAssert.assertThat("have no ui runnables", uiRunnableCount(), Matchers.`is`(0))
        }

        internal fun newCBWebViewProtocol(
            callback: AdUnitRendererImpressionCallback,
            impressionInterface: ImpressionInterface,
            webViewTimeoutInterface: WebViewTimeoutInterface,
            nativeBridgeCommand: NativeBridgeCommand,
            eventTracker: EventTrackerExtensions,
        ): CBMraidWebViewProtocol {
            return CBMraidWebViewProtocol(
                applicationContext,
                "default",
                MediaTypeOM.UNKNOWN,
                "interstitial",
                fileCache,
                networkService,
                FakeUiPoster(),
                templateProxy,
                mediation,
                " {templateHTML} ",
                openMeasurementImpressionCallback,
                callback,
                impressionInterface,
                webViewTimeoutInterface,
                nativeBridgeCommand,
                eventTracker = eventTracker,
            )
        }

        override fun toString(): String {
            val runnableCount = backgroundRunnableCount() + networkRunnableCount() + uiRunnableCount()
            return if (runnableCount == 0) {
                if (scheduledBackgroundRunnableCount() == 0 && scheduledUiRunnableCount() == 0) {
                    "none ready or scheduled"
                } else {
                    val sb = StringBuilder()
                    sb.append("none ready.")
                    if (scheduledBackgroundRunnableCount() != 0) {
                        sb.append(" bg: ")
                        sb.append(manualBackgroundExecutor.toString())
                    }
                    if (scheduledUiRunnableCount() != 0) {
                        sb.append(" ui: ")
                        sb.append(handlerMockWrapper.toString())
                    }
                    sb.toString()
                }
            } else {
                val sb = StringBuilder()
                sb.append(runnableCount)
                sb.append(" ready:")
                if (backgroundRunnableCount() != 0) {
                    sb.append(" ")
                    sb.append(backgroundRunnableCount())
                    sb.append(" bg")
                }
                if (networkRunnableCount() != 0) {
                    sb.append(" ")
                    sb.append(networkRunnableCount())
                    sb.append(" net")
                }
                if (uiRunnableCount() != 0) {
                    sb.append(" ")
                    sb.append(uiRunnableCount())
                    sb.append(" ui")
                }
                sb.toString()
            }
        }

        private fun webviewInterstitialBuilder(): WebViewGetResponseBuilder {
            return WebViewGetResponseBuilder.interstitialReturned()
                .withHtmlBodyElement(AssetDescriptor.baseTemplate2e34e6)
        }

        fun setIdentityTracking(trackingState: TrackingState) {
            builder.setIdentityTracking(trackingState)
            identity = builder.identity
            builder.rebuildRequestBodyBuilderImpl()
            requestBodyBuilder = builder.requestBodyBuilder
        }

        companion object {
            @JvmStatic
            fun forFormat(): TestContainer {
                return TestContainerBuilder.forFormat().build()
            }

            @JvmStatic
            fun emptyConfig(): TestContainer {
                return TestContainerBuilder.emptyConfig().build()
            }

            @JvmStatic
            fun defaultWebView(): TestContainer {
                return TestContainerBuilder.defaultWebView().build()
            }
        }

        init {
            timeSource = testTimeSource
            activity = androidTestContainer.activity
            applicationContext = androidTestContainer.applicationContext
            handlerMockWrapper = androidTestContainer.handlerMockWrapper
            appId = builder.appId
            appSignature = builder.appSignature
            cacheDir = androidTestContainer.cacheDir
            internalBaseDir = File(androidTestContainer.cacheDir, ".chartboost")
            responses = builder.responses
            mockNetworkFactory = builder.mockNetworkFactory
            manualBackgroundExecutor = builder.backgroundExecutor
            manualNetworkExecutor = builder.networkExecutor
            backgroundExecutor = builder.backgroundExecutor
            interstitialTraits = builder.interstitialTraits
            rewardedTraits = builder.rewardedTraits
            downloader = builder.downloader
            fileCache = builder.fileCache
            identity = builder.identity
            networkExecutor = builder.networkExecutor
            networkService = builder.networkService
            prefetcher = builder.prefetcher
            reachability = builder.reachability
            requestBodyBuilder = builder.requestBodyBuilder
            sdkConfig = builder.sdkConfig
            sharedPreferences = builder.sharedPreferences
            uiPoster = androidTestContainer.uiPoster
            urlResolver = builder.urlResolver
            urlRedirect = builder.urlRedirect
            intentResolver = builder.intentResolver
            privacyApi = builder.privacyApi
            carrierBuilder = builder.carrierBuilder
            session = builder.session
            videoRepository = builder.videoRepository
            templateProxy = builder.templateProxy
            mediation = builder.mediation
            openMeasurementImpressionCallback = builder.openMeasurementImpressionCallback
            adUnitRendererImpressionCallback = builder.adUnitRendererImpressionCallback
            deviceBodyFieldsFactory = builder.deviceBodyFieldsFactory
        }
    }
