package com.chartboost.sdk.internal.initialization

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.Prefetcher
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.identity.TrackingState
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.test.FakeUiPoster
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.Session
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

@RunWith(MockitoJUnitRunner::class)
class SdkInitializerTest {
    private val coroutineRunCounter = AtomicInteger()

    private val contextMock = relaxedMockk<Context>()
    private val sharedPreferencesMock = relaxedMockk<SharedPreferences>()
    private val editorMock = relaxedMockk<SharedPreferences.Editor>()
    private val uiPosterMock = relaxedMockk<FakeUiPoster>()
    private val privacyApiMock = relaxedMockk<PrivacyApi>()
    private val identityMock = relaxedMockk<CBIdentity>()
    private val sdkConfigMock = relaxedMockk<SdkConfiguration>()
    private val sdkConfigRef = AtomicReference(sdkConfigMock)
    private val backgroundExecutorMock = relaxedMockk<ScheduledExecutorService>()
    private val prefetcherMock = relaxedMockk<Prefetcher>()
    private val downloaderMock = relaxedMockk<Downloader>()
    private val sessionMock = relaxedMockk<Session>()
    private val videoRepositoryMock = relaxedMockk<VideoRepository>()
    private val initInstallRequestMock = relaxedMockk<InitInstallRequest>()
    private val initConfigRequestMock = relaxedMockk<InitConfigRequest>()
    private val reachabilityMock = relaxedMockk<CBReachability>()
    private val startCallbackMock = relaxedMockk<StartCallback>()
    private val providerInstallerHelperMock = relaxedMockk<ProviderInstallerHelper>()
    private val videoCachePolicyMock = VideoCachePolicy(reachability = reachabilityMock)
    private val openMeasurementManagerMock = mockk<OpenMeasurementManager>()

    private val sdkInitializer =
        SdkInitializer(
            contextMock,
            sharedPreferencesMock,
            uiPosterMock,
            privacyApiMock,
            sdkConfigRef,
            prefetcherMock,
            downloaderMock,
            sessionMock,
            videoCachePolicyMock,
            lazyOf(videoRepositoryMock),
            initInstallRequestMock,
            initConfigRequestMock,
            reachabilityMock,
            providerInstallerHelperMock,
            identityMock,
            openMeasurementManagerMock,
        )

    @Before
    fun setup() {
        every { initConfigRequestMock.execute(any()) } just Runs
        every { initInstallRequestMock.execute() } just Runs
        every { providerInstallerHelperMock.installProviderIfPossible() } just Runs
        every { editorMock.putString(any(), any()) } returns editorMock
        every { sharedPreferencesMock.edit() } returns editorMock
        every { reachabilityMock.isNetworkAvailable } returns true
        every { videoRepositoryMock.initialize(contextMock) } just Runs
        every { openMeasurementManagerMock.initialize() } just Runs

        val identityBodyFields =
            IdentityBodyFields(
                TrackingState.TRACKING_ENABLED,
                "eyJnYWlkIjoiODI4YWFmZTgtYjU2NS00ODFjLThhMGEtMTBjYTFhZDFhYmUxIn0=",
                "a uuid",
                "a gaid",
                "a setId",
                1,
            )
        every { identityMock.toIdentityBodyFields() }.answers { identityBodyFields }
    }

    @Test
    fun `init sdk for the first time`() {
        sdkInitializer.isSDKInitialized = false
        val privacyCaptor = slot<String>()
        sdkInitializer.initSdk("000000000000000000000000", "0000000000000000000000000000000000000000", startCallbackMock)
        verify(exactly = 1) { providerInstallerHelperMock.installProviderIfPossible() }
        verify(exactly = 1) { downloaderMock.reduceCacheSize() }
        verify(exactly = 1) { initConfigRequestMock.execute(any()) }
        verify(exactly = 1) { privacyApiMock.getPrivacyStandard(capture(privacyCaptor)) }
        privacyCaptor.captured shouldBe COPPA.COPPA_STANDARD
    }

    @Test
    fun `init sdk with invalid ids empty`() {
        initWithInvalidCredentials("", "")
    }

    @Test
    fun `init sdk with invalid ids below expect length`() {
        initWithInvalidCredentials("below24chars", "below40chars")
    }

    @Test
    fun `init sdk with invalid ids below expect length but signature valid`() {
        initWithInvalidCredentials("below24chars", "0000000000000000000000000000000000000000")
    }

    @Test
    fun `init sdk with invalid signature below expect length but app id valid`() {
        initWithInvalidCredentials("000000000000000000000000", "below40chars")
    }

    @Test
    fun `init sdk with invalid ids above expect length`() {
        initWithInvalidCredentials(
            "this_is_length_above_24_chars_valid",
            "this_is_length_above_40_chars_valid_this_is_length_above_40_chars_valid",
        )
    }

    @Test
    fun `init sdk with invalid app id above expect length and valid signature`() {
        initWithInvalidCredentials("this_is_length_above_24_chars_valid", "0000000000000000000000000000000000000000")
    }

    @Test
    fun `init sdk with invalid signature above expect length and valid app id`() {
        initWithInvalidCredentials("000000000000000000000000", "this_is_length_above_40_chars_valid_this_is_length_above_40_chars_valid")
    }

    @Test
    fun `init sdk with invalid appid but valid length`() {
        initWithInvalidCredentials("00000000000000000000000@", "0000000000000000000000000000000000000000")
    }

    @Test
    fun `init sdk with invalid signature but valid length`() {
        initWithInvalidCredentials("000000000000000000000000", "000000000000000000000000000000000000000@")
    }

    @Test
    fun `init sdk with invalid letter appid but valid length`() {
        initWithInvalidCredentials("00000000000000000000000g", "0000000000000000000000000000000000000000")
    }

    @Test
    fun `init sdk with invalid letter signature but valid length`() {
        initWithInvalidCredentials("000000000000000000000000", "000000000000000000000000000000000000000g")
    }

    private fun initWithInvalidCredentials(
        appId: String,
        signature: String,
    ) {
        val sdkInitializerInvalidIds =
            SdkInitializer(
                contextMock,
                sharedPreferencesMock,
                uiPosterMock,
                privacyApiMock,
                sdkConfigRef,
                prefetcherMock,
                downloaderMock,
                sessionMock,
                videoCachePolicyMock,
                lazyOf(videoRepositoryMock),
                initInstallRequestMock,
                initConfigRequestMock,
                reachabilityMock,
                providerInstallerHelperMock,
                identityMock,
                openMeasurementManagerMock,
            )

        sdkInitializerInvalidIds.isSDKInitialized = false
        val privacyCaptor = slot<String>()
        sdkInitializerInvalidIds.initSdk(appId, signature, startCallbackMock)
        verify(inverse = true) { providerInstallerHelperMock.installProviderIfPossible() }
        verify(inverse = true) { downloaderMock.reduceCacheSize() }
        verify(inverse = true) { initConfigRequestMock.execute(any()) }
        verify(exactly = 1) { privacyApiMock.getPrivacyStandard(capture(privacyCaptor)) }
        privacyCaptor.captured shouldBe COPPA.COPPA_STANDARD
    }

    @Test
    fun `init sdk already initialised`() {
        sdkInitializer.isSDKInitialized = true
        val lambdaCaptor = slot<() -> Unit>()
        val privacyCaptor = slot<String>()
        sdkInitializer.initSdk("test", "test", startCallbackMock)
        verify(inverse = true) { providerInstallerHelperMock.installProviderIfPossible() }
        verify(inverse = true) { downloaderMock.reduceCacheSize() }
        verify(exactly = 1) { initConfigRequestMock.execute(any()) }
        verify(exactly = 1) { privacyApiMock.getPrivacyStandard(capture(privacyCaptor)) }
        privacyCaptor.captured shouldBe COPPA.COPPA_STANDARD
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.shouldNotBeNull()
        lambdaCaptor.captured()
        verify(exactly = 1) { startCallbackMock.onStartCompleted(null) }
    }

    @Ignore("HB-8129")
    @Test
    fun `on config request success`() {
        val stringCaptor1 = slot<String>()
        val stringCaptor2 = slot<String>()
        val lambdaCaptor = slot<() -> Unit>()

        val configMock = spyk(JSONObject("{}"))
        every { configMock.toString() } returns "{}"

        sdkInitializer.initSdk("000000000000000000000000", "0000000000000000000000000000000000000000", startCallbackMock)
        sdkInitializer.onConfigRequestSuccess(configMock)

        verify(exactly = 1) { sharedPreferencesMock.edit() }
        verify(exactly = 1) { editorMock.putString(capture(stringCaptor1), capture(stringCaptor2)) }

        stringCaptor1.captured shouldBe "config"
        stringCaptor2.captured shouldBe "{}"
        videoCachePolicyMock.bufferSize shouldBe 3

        verify(exactly = 1) { videoRepositoryMock.initialize(contextMock) }
        verify(exactly = 1) { privacyApiMock.setPrivacyConfig(any()) }
        verify(exactly = 1) { initInstallRequestMock.execute() }
        verify(exactly = 1) { prefetcherMock.prefetch() }
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.shouldNotBeNull()
        lambdaCaptor.captured()
        verify(exactly = 1) { startCallbackMock.onStartCompleted(null) }
        verify(exactly = 1) { openMeasurementManagerMock.initialize() }
    }

    @Test
    fun `on config request success don't call startCompleted if sdk is already init`() {
        mockkObject(SandboxBridgeSettings)
        every { SandboxBridgeSettings.isSandboxMode } returns false
        val lambdaCaptor = slot<() -> Unit>()
        val configMock = mockk<JSONObject>()
        every { configMock.toString() } returns "{}"
        sdkInitializer.initSdk("000000000000000000000000", "0000000000000000000000000000000000000000", startCallbackMock)
        sdkInitializer.onConfigRequestSuccess(configMock)
        // second successful config request
        sdkInitializer.onConfigRequestSuccess(configMock)
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured.shouldNotBeNull()
        lambdaCaptor.captured()
        // onStartCompleted is called only once
        verify(exactly = 1) { startCallbackMock.onStartCompleted(null) }
        verify(exactly = 2) { openMeasurementManagerMock.initialize() }
    }

    @Test
    fun `on config request failure after first session`() {
        sdkInitializer.isFirstSession = false
        val configErrorMsg = "test error"
        sdkConfigMock.privacyStandardsConfig = SdkConfiguration.PrivacyStandardsConfig()
        sdkInitializer.initSdk("0000", "0000") { }
        sdkInitializer.onConfigRequestFailure(configErrorMsg)
        verify(inverse = true) { sharedPreferencesMock.edit() }
        verify(inverse = true) { editorMock.putString(any(), any()) }
        verify(exactly = 1) { videoRepositoryMock.initialize(contextMock) }
        verify(exactly = 1) { privacyApiMock.setPrivacyConfig(any()) }
        verify(exactly = 1) { initInstallRequestMock.execute() }
        verify(exactly = 1) { prefetcherMock.prefetch() }
        sdkInitializer.isFirstSession = true
        verify(exactly = 1) { openMeasurementManagerMock.initialize() }
    }

    @Test
    fun `on config request failure on first session`() {
        val lambdaCaptor = slot<() -> Unit>()
        val startError = slot<StartError>()

        every { sessionMock.sessionCounter } returns 0

        val configErrorMsg = "test error"
        val privacyStandardsConfig = SdkConfiguration.PrivacyStandardsConfig()
        sdkConfigMock.privacyStandardsConfig = privacyStandardsConfig
        sdkInitializer.initSdk("000000000000000000000000", "0000000000000000000000000000000000000000", startCallbackMock)
        sdkInitializer.onConfigRequestFailure(configErrorMsg)
        verify(inverse = true) { sharedPreferencesMock.edit() }
        verify(inverse = true) { editorMock.putString(any(), any()) }
        verify(inverse = true) { videoRepositoryMock.initialize(contextMock) }
        verify(inverse = true) { privacyApiMock.setPrivacyConfig(any()) }
        verify(inverse = true) { initInstallRequestMock.execute() }
        verify(inverse = true) { backgroundExecutorMock.execute(any()) }
        verify(inverse = true) { prefetcherMock.prefetch() }
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured()
        verify(exactly = 1) { startCallbackMock.onStartCompleted(capture(startError)) }
        startError.captured.code shouldBe StartError.Code.SERVER_ERROR
        verify(exactly = 0) { openMeasurementManagerMock.initialize() }
    }

    @Test
    fun `on config request failure on first session and no network connection`() {
        val lambdaCaptor = slot<() -> Unit>()
        val startError = slot<StartError>()

        every { sessionMock.sessionCounter } returns 0
        every { reachabilityMock.isNetworkAvailable } returns false

        val configErrorMsg = "test error"
        val privacyStandardsConfig = SdkConfiguration.PrivacyStandardsConfig()
        sdkConfigMock.privacyStandardsConfig = privacyStandardsConfig
        sdkInitializer.initSdk("000000000000000000000000", "0000000000000000000000000000000000000000", startCallbackMock)
        sdkInitializer.onConfigRequestFailure(configErrorMsg)
        verify(inverse = true) { sharedPreferencesMock.edit() }
        verify(inverse = true) { editorMock.putString(any(), any()) }
        verify(inverse = true) { videoRepositoryMock.initialize(contextMock) }
        verify(inverse = true) { privacyApiMock.setPrivacyConfig(any()) }
        verify(inverse = true) { initInstallRequestMock.execute() }
        verify(inverse = true) { backgroundExecutorMock.execute(any()) }
        verify(inverse = true) { prefetcherMock.prefetch() }
        verify(exactly = 1) { uiPosterMock(capture(lambdaCaptor)) }
        lambdaCaptor.captured()
        verify(exactly = 1) { startCallbackMock.onStartCompleted(capture(startError)) }
        startError.captured.code shouldBe StartError.Code.NETWORK_FAILURE
        verify(exactly = 0) { openMeasurementManagerMock.initialize() }
    }

    @Test
    fun `async init with coroutines`() {
        runBlocking {
            withContext(Dispatchers.Default) {
                runAsync {
                    val startCallbackLocalMock = relaxedMockk<StartCallback>()
                    val lambdaCaptor = mutableListOf<() -> Unit>()
                    sdkInitializer.isSDKInitialized = true
                    sdkInitializer.initSdk("test", "test", startCallbackLocalMock)
                    verify(atLeast = 1) { uiPosterMock(capture(lambdaCaptor)) }
                    lambdaCaptor.forEach { it() }
                    verify(atLeast = 1) { startCallbackLocalMock.onStartCompleted(null) }
                    coroutineRunCounter.incrementAndGet()
                }
            }
            println("async init with coroutines ran for $coroutineRunCounter times")
        }
    }

    // I run 10 coroutines with 100 actions each. More than that could lead to out of memory issues
    // for example: 100 x 1000 was causing problems
    suspend fun runAsync(action: suspend () -> Unit) {
        val n = 10 // number of coroutines to launch
        val k = 100 // times an action is repeated by each coroutine
        val time =
            measureTimeMillis {
                coroutineScope { // scope for coroutines
                    repeat(n) {
                        launch {
                            repeat(k) { action() }
                        }
                    }
                }
            }
        println("Completed ${n * k} actions in $time ms")
    }
}
