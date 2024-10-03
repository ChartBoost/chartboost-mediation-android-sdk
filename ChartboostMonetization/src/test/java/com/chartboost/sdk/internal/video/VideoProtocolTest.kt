package com.chartboost.sdk.internal.video

import android.content.Context
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.measurement.Quartile
import com.chartboost.sdk.internal.video.player.mediaplayer.AdsMediaPlayer
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent
import com.iab.omid.library.chartboost.adsession.media.PlayerState
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class VideoProtocolTest {
    private val contextMock: Context = mockk()
    private val uiPosterMock: UiPoster = mockk(relaxed = true)
    private val fileCacheMock: FileCache = mockk()
    private val videoRepositoryMock: VideoRepository = mockk()
    private val templateProxyMock: CBTemplateProxy = mockk()
    private val adsMediaPlayerMock: AdsMediaPlayer = mockk()
    private val openMeasurementImpressionCallbackMock = mockk<OpenMeasurementImpressionCallback>()
    private val adUnitRendererImpressionCallbackMock = mockk<AdUnitRendererImpressionCallback>()
    private val impressionInterfaceMock = mockk<ImpressionInterface>()
    private val webViewTimeoutInterface = mockk<WebViewTimeoutInterface>()
    private val nativeBridgeCommandMock = mockk<NativeBridgeCommand>()

    private val mediation =
        Mediation(
            "mediation",
            "1.2.3",
            "3.2.1",
        )
    private val networkService: CBNetworkService = mockk()
    private val eventTrackerMock =
        relaxedMockk<EventTrackerExtensions>().apply {
            every { any<TrackingEvent>().track() } answers { firstArg() }
        }

    private lateinit var protocol: VideoProtocol
    private var localFileMock: File? = null

    @Before
    fun setup() {
        val asset = getVideoAsset("http://server.com/test.mp4")
        every { videoRepositoryMock.getVideoAsset(any()) } returns asset
        every { videoRepositoryMock.startDownloadIfPossible(any(), any(), any()) } just Runs
        every { videoRepositoryMock.getVideoDownloadState(any()) } returns 0
        every { videoRepositoryMock.removeAsset(any()) } returns true

        every { templateProxyMock.callOnBackgroundJSFunction(any(), any(), any()) } just Runs
        every { templateProxyMock.callOnForegroundJSFunction(any(), any(), any()) } just Runs
        every { templateProxyMock.callOnPlaybackTimeJSFunction(any(), any(), any(), any()) } just Runs
        every { templateProxyMock.callOnVideoFailedJSFunction(any(), any(), any()) } just Runs
        every { templateProxyMock.callOnVideoStartedJSFunction(any(), any(), any(), any()) } just Runs
        every { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) } just Runs

        every { adsMediaPlayerMock.asset(any()) } just Runs
        every { adsMediaPlayerMock.volume() } returns 1f

        every { nativeBridgeCommandMock.setImpressionInterface(any()) } just Runs

        every { impressionInterfaceMock.onWebViewError(any()) } returns CBError.Impression.INTERNAL

        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVolumeChanged(any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoStarted(any(), any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoResumed() } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVolumeChanged(any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoComplete() } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoBuffer(any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyStateChanged(any()) } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoPaused() } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionDestroyWebview() } just Runs
        every { nativeBridgeCommandMock.hideViewCallback = any() } just Runs
        protocol = getProtocolByOMtype(MediaTypeOM.VIDEO)
    }

    private fun getProtocolByOMtype(omTypeOM: MediaTypeOM): VideoProtocol {
        return VideoProtocol(
            context = contextMock,
            location = "default",
            mtype = omTypeOM,
            adUnitParameters = "interstitial",
            uiPoster = uiPosterMock,
            fileCache = fileCacheMock,
            templateProxy = templateProxyMock,
            videoRepository = videoRepositoryMock,
            videoFilename = "sniper3d_v_202010_104_en_portrait.mp4",
            mediation = mediation,
            adsVideoPlayerFactory = { _, _, _, _, _ -> adsMediaPlayerMock },
            networkService = networkService,
            templateHtml = "{ templateHTML }",
            openMeasurementImpressionCallback = openMeasurementImpressionCallbackMock,
            adUnitRendererImpressionCallback = adUnitRendererImpressionCallbackMock,
            impressionInterface = impressionInterfaceMock,
            webViewTimeoutInterface = webViewTimeoutInterface,
            nativeBridgeCommand = nativeBridgeCommandMock,
            eventTracker = eventTrackerMock,
            cbWebViewFactory = { relaxedMockk() },
        )
    }

    @After
    fun teardown() {
        localFileMock?.delete()
    }

    @Test
    fun createViewObjectSuccessTest() {
        val assetCaptor = CapturingSlot<VideoAsset>()

        val view = createBaseVideoView()
        verify(exactly = 1) { adsMediaPlayerMock.asset(capture(assetCaptor)) }
        verify(exactly = 1) { nativeBridgeCommandMock.setImpressionInterface(any()) }

        view.shouldNotBeNull()
        val assetArg = assetCaptor.captured
        assetArg.shouldNotBeNull()
        verify(exactly = 0) {
            templateProxyMock.callOnVideoFailedJSFunction(any(), any(), any())
            adsMediaPlayerMock.stop()
        }
    }

    @Test
    fun destroyVideoBaseTest() {
        createBaseVideoView()
        protocol.destroy()
        verify(exactly = 0) { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) }
        verify(exactly = 1) { adsMediaPlayerMock.stop() }
    }

    @Test
    fun destroyViewTest() {
        createBaseVideoView()
        protocol.destroy()
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionDestroyWebview() }
        verify(exactly = 0) { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) }
    }

    @Test
    fun onResumeTest() {
        createBaseVideoView()
        every { adsMediaPlayerMock.onComingFromBackground() } just runs
        protocol.onResume()
        verify(exactly = 1) {
            videoRepositoryMock.startDownloadIfPossible(null, eq(1), eq(false))
            adsMediaPlayerMock.onComingFromBackground()
            adsMediaPlayerMock.play()
        }
    }

    @Test
    fun onPauseTest() {
        createBaseVideoView()
        protocol.onPause()
        verify(exactly = 1) { adsMediaPlayerMock.pause() }
    }

    @Test
    fun onConfigurationChangeTest() {
        createBaseVideoView()
        protocol.onConfigurationChange()
        verify(exactly = 1) { adsMediaPlayerMock.onScreenOrientationChange(eq(0), eq(0)) }
    }

    @Test
    fun startVideoFirstTimeTest() {
        createBaseVideoView()
        every { adsMediaPlayerMock.wasMediaStartedForTheFirstTime() } returns false
        protocol.playVideo()
        verify(exactly = 1) { adsMediaPlayerMock.play() }
        verify(exactly = 1) { adsMediaPlayerMock.volume() }
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyStateChanged(PlayerState.FULLSCREEN) }
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoStarted(0f, 1f) }
        verify(exactly = 0) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoResumed() }
    }

    @Test
    fun onVideoDisplayStartedTest() {
        protocol.onVideoDisplayStarted()
        verify(exactly = 1) { templateProxyMock.callOnVideoStartedJSFunction(any(), any(), any(), any()) }
    }

    @Test
    fun pauseVideoTest() {
        createBaseVideoView()
        protocol.pauseVideo()
        verify(exactly = 1) { adsMediaPlayerMock.pause() }
    }

    @Test
    fun closeVideoTest() {
        createBaseVideoView()
        protocol.closeVideo()
        verify(exactly = 0) { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) }
        verify(exactly = 1) { adsMediaPlayerMock.stop() }
    }

    @Test
    fun muteVideoTest() {
        createBaseVideoView()
        protocol.muteVideo()
        verify(exactly = 1) { adsMediaPlayerMock.mute() }
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVolumeChanged(0f) }
    }

    @Test
    fun unmuteVideoTest() {
        createBaseVideoView()
        protocol.unmuteVideo()
        verify(exactly = 1) { adsMediaPlayerMock.unmute() }
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVolumeChanged(1f) }
    }

    @Test
    fun onVideoDisplayProgressTest() {
        protocol.onVideoDisplayProgress(1)
        verify(exactly = 1) { templateProxyMock.callOnPlaybackTimeJSFunction(any(), eq(0.001f), any(), any()) }
    }

    @Test
    fun onVideoDisplayProgressSendQuartilesTest() {
        protocol.onVideoDisplayPrepared(12000)
        protocol.onVideoDisplayProgress(3000)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.FIRST) }
        protocol.onVideoDisplayProgress(6000)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.MIDDLE) }
        protocol.onVideoDisplayProgress(9000)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.THIRD) }
    }

    @Test
    fun onVideoDisplayProgressSendQuartilesForNonVideoADTest() {
        val protocol = getProtocolByOMtype(MediaTypeOM.NATIVE)
        protocol.onVideoDisplayPrepared(12000)
        protocol.onVideoDisplayProgress(3000)
        verify(exactly = 0) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.FIRST) }
        protocol.onVideoDisplayProgress(6000)
        verify(exactly = 0) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.MIDDLE) }
        protocol.onVideoDisplayProgress(9000)
        verify(exactly = 0) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.THIRD) }
    }

    @Test
    fun onVideoDisplayCompletedTest() {
        createBaseVideoView()
        protocol.onVideoDisplayCompleted()
        verify(exactly = 1) { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) }
        verify(exactly = 0) { adsMediaPlayerMock.stop() }
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoComplete() }
    }

    @Test
    fun onVideoDisplayErrorTest() {
        createBaseVideoView()
        protocol.onVideoDisplayError("test error")
        verify(exactly = 1) { templateProxyMock.callOnVideoFailedJSFunction(any(), any(), any()) }
        verify(exactly = 1) { adsMediaPlayerMock.stop() }
    }

    @Test
    fun removeVideoAssetOnErrorSuccessTest() {
        val assetCaptor = CapturingSlot<VideoAsset>()
        val asset = VideoAsset("testurl", "test", null, null)
        every { videoRepositoryMock.getVideoAsset(any()) } returns asset
        every { videoRepositoryMock.removeAsset(any()) } returns true
        protocol.removeAssetOnError()
        verify(exactly = 1) { videoRepositoryMock.removeAsset(capture(assetCaptor)) }
        assetCaptor.captured.run {
            shouldNotBeNull()
            filename shouldBe "test"
            url shouldBe "testurl"
        }
    }

    @Test
    fun removeVideoAssetOnErrorFailureTest() {
        every { videoRepositoryMock.removeAsset(any()) } returns false
        every { videoRepositoryMock.getVideoAsset(any()) } returns null
        protocol.removeAssetOnError()
        verify(exactly = 1) { videoRepositoryMock.removeAsset(null) }
    }

    @Test
    fun getAssetDownloadStateNowNoAssetTest() {
        val videoCacheState = protocol.getAssetDownloadStateNow()
        videoCacheState shouldBe 0
    }

    @Test
    fun getAssetDownloadStateNowWithAssetTest() {
        val asset = VideoAsset("testurl", "test", null, null)
        every { videoRepositoryMock.getVideoAsset(any()) } returns asset
        every { videoRepositoryMock.getVideoDownloadState(asset) } returns 1
        val videoCacheState = protocol.getAssetDownloadStateNow()
        videoCacheState shouldBe 1
    }

    @Test
    fun onVideoBufferStartTest() {
        protocol.onVideoBufferStart()
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoBuffer(true) }
    }

    @Test
    fun onVideoBufferFinishTest() {
        protocol.onVideoBufferFinish()
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoBuffer(false) }
    }

    @Test
    fun sendQuartileEventTest() {
        protocol.sendQuartileEvent(10f, 1f)
        verify(exactly = 0) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.FIRST) }
        protocol.sendQuartileEvent(10f, 3f)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.FIRST) }
        protocol.sendQuartileEvent(10f, 7f)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.MIDDLE) }
        protocol.sendQuartileEvent(10f, 9f)
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyVideoProgress(Quartile.THIRD) }
    }

    private fun getVideoAsset(url: String): VideoAsset {
        val localFileMock = mockk<File>()
        val directoryMock = mockk<File>()
        return VideoAsset(url, "test.mp4", localFileMock, directoryMock)
    }

    private fun createBaseVideoView(): ViewBase? {
        with(adsMediaPlayerMock) {
            every { asset(any()) } just Runs
            every { play() } just Runs
            every { mute() } just Runs
            every { unmute() } just Runs
            every { pause() } just Runs
            every { stop() } just Runs
            every { onScreenOrientationChange(any(), any()) } just Runs
        }

        val asset = getVideoAsset("http://server.com/test.mp4")
        every { videoRepositoryMock.getVideoAsset(any()) } returns asset
        return protocol.createViewObject(contextMock)
    }
}
