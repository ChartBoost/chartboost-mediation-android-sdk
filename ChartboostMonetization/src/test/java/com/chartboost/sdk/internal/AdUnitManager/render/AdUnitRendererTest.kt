package com.chartboost.sdk.internal.AdUnitManager.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.impression.ImpressionBuilder
import com.chartboost.sdk.internal.AdUnitManager.impression.ImpressionHolder
import com.chartboost.sdk.internal.AssetLoader.TemplateLoader
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.*
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.defaultUrl
import com.chartboost.sdk.internal.Networking.requests.models.ShowParamsModel
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.internal.impression.ImpressionViewProtocolBuilder
import com.chartboost.sdk.internal.measurement.OpenMeasurementController
import com.chartboost.sdk.internal.measurement.VisibilityTracker
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.view.CBImpressionActivity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AdUnitRendererTest {
    private val testScope: TestScope = TestScope(UnconfinedTestDispatcher())
    private val interstitialTraits = AdType.Interstitial
    private val bannerTraits = AdType.Banner
    private val reachabilityMock = mockk<CBReachability>()
    private val mediationMock = mockk<Mediation>()
    private val impressionMock = mockk<CBImpression>()
    private val viewBaseMock = mockk<ViewBase>()
    private val viewProtocolMock = mockk<CBViewProtocol>()
    private val adUnitRendererAdCallback = mockk<AdUnitRendererAdCallback>()
    private val impressionBuilderMock = mockk<ImpressionBuilder>()
    private val videoRepositoryMock = mockk<VideoRepository>()
    private val fileCacheMock = mockk<FileCache>()
    private val adUnitRendererShowRequestMock = mockk<AdUnitRendererShowRequest>()
    private val openMeasurementControllerMock = mockk<OpenMeasurementController>()
    private val impressionViewProtocolBuilderMock = mockk<ImpressionViewProtocolBuilder>()
    private val rendererActivityBridgeMock = mockk<RendererActivityBridge>()
    private val trackingEventMock = mockk<TrackingEvent>()
    private val nativeBridgeCommandMock = mockk<NativeBridgeCommand>()
    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()
    private val templateLoader = relaxedMockk<TemplateLoader>()
    private val endpointRepositoryMock =
        mockk<EndpointRepository> {
            every { getEndPointUrl(any()) } answers { firstArg<EndpointRepository.EndPoint>().defaultUrl }
        }

    private val impressionHolder = ImpressionHolder(impressionMock, null)
    private val impressionId = "1"
    private val adUnitNoVideo =
        AdUnit(
            videoFilename = "",
            videoUrl = "",
            name = "test",
            impressionId = "id",
            adId = "adid",
        )
    private val appRequestNoVideo =
        AppRequest(
            id = 1,
            location = "test location",
            bidResponse = null,
            adUnit = adUnitNoVideo,
        )

    private val adUnitWithVideo = AdUnit(videoFilename = "test", videoUrl = "test", name = "test")
    private val appRequestWithVideo =
        AppRequest(
            id = 1,
            location = "test location",
            bidResponse = null,
            adUnit = adUnitWithVideo,
        )

    private val adUnitRenderer =
        AdUnitRenderer(
            interstitialTraits,
            reachabilityMock,
            fileCacheMock,
            videoRepositoryMock,
            impressionBuilderMock,
            adUnitRendererShowRequestMock,
            openMeasurementControllerMock,
            impressionViewProtocolBuilderMock,
            rendererActivityBridgeMock,
            nativeBridgeCommandMock,
            templateLoader,
            mediationMock,
            testScope,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    private val adUnitRendererBanner =
        AdUnitRenderer(
            bannerTraits,
            reachabilityMock,
            fileCacheMock,
            videoRepositoryMock,
            impressionBuilderMock,
            adUnitRendererShowRequestMock,
            openMeasurementControllerMock,
            impressionViewProtocolBuilderMock,
            rendererActivityBridgeMock,
            nativeBridgeCommandMock,
            templateLoader,
            mediationMock,
            testScope,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    @Before
    fun setup() {
        every {
            impressionViewProtocolBuilderMock.prepareViewProtocol(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns viewProtocolMock

        every { reachabilityMock.isNetworkAvailable } returns true

        every {
            impressionBuilderMock.createImpressionHolderFromAppRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }.returns(impressionHolder)
        every { videoRepositoryMock.downloadVideoFile(any(), any(), any(), any()) } just Runs
        every { adUnitRendererShowRequestMock.execute(any(), any()) } just Runs
        every { adUnitRendererAdCallback.onRequestedToShow(any()) } just Runs
        every { adUnitRendererAdCallback.onShowFailure(any(), any()) } just Runs
        every { adUnitRendererAdCallback.onShowSuccess(any()) } just Runs
        every { adUnitRendererAdCallback.onImpressionClicked(any()) } just Runs
        every { adUnitRendererAdCallback.onImpressionClickedFailed(any(), any(), any()) } just Runs
        every { adUnitRendererAdCallback.onReward(any(), any()) } just Runs
        every { adUnitRendererAdCallback.onImpressionDismissed(any()) } just Runs
        every { adUnitRendererAdCallback.onImpressionSuccess(any()) } just Runs
        every { openMeasurementControllerMock.signalImpressionEvent() } just Runs
        every { openMeasurementControllerMock.onImpressionDestroyWebview() } just Runs
        every {
            openMeasurementControllerMock.onImpressionNotifyVideoStarted(
                any(),
                any(),
            )
        } just Runs
        every {
            openMeasurementControllerMock.onImpressionOnWebviewPageStarted(
                any(),
                any(),
                any(),
            )
        } just Runs
        every { openMeasurementControllerMock.destroyVisibilityTracker() } just Runs
        every {
            openMeasurementControllerMock.createVisibilityTracker(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        every { impressionMock.wasImpressionSignaled = any() } just Runs
        every { impressionMock.wasImpressionSignaled } returns false
        every { impressionMock.setIsShowProcessed(any()) } just Runs
        every { impressionMock.getIsShowProcessed() } returns true
        every { impressionMock.setIsVisible(any()) } just Runs
        every { impressionMock.getIsVisible() } returns true
        every { impressionMock.getLocation() } returns "loc"
        every { impressionMock.getViewProtocolAssetDownloadStateNow() } returns -1
        every { impressionMock.prepareAndDisplay() } just Runs
        every { impressionMock.getImpressionState() } returns ImpressionState.NONE
        every { impressionMock.setImpressionState(any()) } just Runs
        every { impressionMock.getViewProtocolView() } returns viewBaseMock
        every { impressionMock.viewProtocolDestroy() } just Runs
        every { impressionMock.isViewProtocolViewNotCreated() } returns false
        every { impressionMock.getAdUnitImpressionId() } returns impressionId
        every { rendererActivityBridgeMock.finishActivity() } just Runs
        every { fileCacheMock.isAssetsAvailable(any()) } returns true
        every { trackingEventMock.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
        every { trackingEventMock.location } returns "location"
        every { trackingEventMock.impressionAdType } returns "interstitial"
        every { trackingEventMock.trackAd = any() } just Runs
        every { trackingEventMock.isLatencyEvent } returns false
        every { trackingEventMock.latency = any() } just Runs
        every { trackingEventMock.shouldCalculateLatency } returns true
        every { nativeBridgeCommandMock.hideViewCallback = null } just Runs
        every { nativeBridgeCommandMock.clearImpressionInterface() } just Runs
        every { nativeBridgeCommandMock.setImpressionInterface(any()) } just Runs
    }

    @Ignore("HB-8129")
    @Test
    fun `render success non precache banner detach banner impression`() {
        val viewBaseMock =
            mockk<ViewBase>().apply {
                every { webView = any() } just Runs
                every { webView } returns mockk()
            }
        viewBaseMock.webView = mockk()
        val bannerViewMock = mockk<ViewGroup>()
        every { bannerViewMock.removeAllViews() } just Runs
        every { bannerViewMock.invalidate() } just Runs
        every { impressionMock.getViewProtocolView() } returns viewBaseMock
        every { impressionMock.getHostView() } returns bannerViewMock
        adUnitRendererBanner.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRendererBanner.onImpressionShownFully(appRequestNoVideo)
        verify(exactly = 1) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 1) { impressionMock.prepareAndDisplay() }
        assertFalse(appRequestNoVideo.isTrackedShow)
        adUnitRendererBanner.detachBannerImpression()
        verify(exactly = 1) { openMeasurementControllerMock.onImpressionDestroyWebview() }
        verify(exactly = 1) { bannerViewMock.removeAllViews() }
        verify(exactly = 1) { bannerViewMock.invalidate() }
        verify(exactly = 1) { impressionMock.viewProtocolDestroy() }
    }

    @Test
    fun `render no connection`() {
        every { reachabilityMock.isNetworkAvailable } returns false
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                any(),
                CBError.Impression.INTERNET_UNAVAILABLE_AT_SHOW,
            )
        }
        verify(exactly = 0) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 0) { adUnitRendererAdCallback.onShowSuccess(any()) }
    }

    @Test
    fun `render asset not ready`() {
        every { fileCacheMock.isAssetsAvailable(any()) } returns false
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                any(),
                CBError.Impression.ASSET_MISSING,
            )
        }
        verify(exactly = 0) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 0) { adUnitRendererAdCallback.onShowSuccess(any()) }
    }

    @Test
    fun `render impression holder with error`() {
        val impressionHolder =
            ImpressionHolder(null, CBError.Impression.ERROR_LOADING_WEB_VIEW)
        every {
            impressionBuilderMock.createImpressionHolderFromAppRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }.returns(impressionHolder)
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        verify(exactly = 1) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                any(),
                CBError.Impression.ERROR_LOADING_WEB_VIEW,
            )
        }
        verify(exactly = 0) { adUnitRendererAdCallback.onShowSuccess(any()) }
    }

    @Test
    fun `render success non precache`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        verify(exactly = 1) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 1) { impressionMock.prepareAndDisplay() }
        assertTrue(appRequestNoVideo.isTrackedShow)
    }

    @Test
    fun `render success precache`() {
        val callbackSlot = CapturingSlot<AdUnitVideoPrecacheTemp>()
        adUnitRenderer.render(appRequestWithVideo, adUnitRendererAdCallback)
        verify(exactly = 1) {
            videoRepositoryMock.downloadVideoFile(
                "test",
                "test",
                true,
                capture(callbackSlot),
            )
        }
        callbackSlot.captured.tempVideoFileIsReady("test")
        verify(exactly = 1) { adUnitRendererAdCallback.onRequestedToShow(any()) }
        verify(exactly = 1) { impressionMock.prepareAndDisplay() }
        assertTrue(appRequestWithVideo.isTrackedShow)
    }

    @Test
    fun `impression ready to be displayed on the host view`() {
        every { impressionMock.shouldDisplayOnHostView() } returns true
        every { impressionMock.getHostView() } returns viewBaseMock
        every { impressionMock.displayOnHostView(any()) } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionReadyToBeDisplayed()
        verify(exactly = 1) { impressionMock.setImpressionState(ImpressionState.LOADED) }
        verify(exactly = 1) { impressionMock.getHostView() }
        verify(exactly = 1) { impressionMock.displayOnHostView(viewBaseMock) }
    }

    @Test
    fun `impression ready to be displayed on the activity`() {
        every { impressionMock.shouldDisplayOnHostView() } returns false
        every { rendererActivityBridgeMock.startActivity(any()) } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionReadyToBeDisplayed()
        verify(exactly = 1) { impressionMock.setImpressionState(ImpressionState.LOADED) }
        verify(exactly = 1) { rendererActivityBridgeMock.startActivity(adUnitRenderer) }
    }

    @Test
    fun `impression close triggered`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionCloseTriggered(appRequestNoVideo)
    }

    @Test
    fun `impression clicked`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionClicked("id")
        verify(exactly = 1) { adUnitRendererAdCallback.onImpressionClicked("id") }
    }

    @Test
    fun `impression clicked failed`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionClickedFailed("id", "url", CBError.Click.INTERNAL)
        verify(exactly = 1) {
            adUnitRendererAdCallback.onImpressionClickedFailed(
                "id",
                "url",
                CBError.Click.INTERNAL,
            )
        }
    }

    @Test
    fun `impression rewarded`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionRewarded("id", 1)
        verify(exactly = 1) { adUnitRendererAdCallback.onReward("id", 1) }
    }

    @Test
    fun `impression dismissed`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionDismissed("id")
        verify(exactly = 1) { adUnitRendererAdCallback.onImpressionDismissed("id") }
    }

    @Ignore("HB-8129")
    @Test
    fun `impression shown fully and ad impression is visible`() {
        val paramSlot = CapturingSlot<ShowParamsModel>()

        val viewBaseMock =
            mockk<ViewBase>().apply {
                every { webView = any() } just Runs
                every { webView } returns mockk()
            }
        viewBaseMock.webView = mockk<CBWebView>()
        impressionMock.setIsVisible(true)
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionShownFully(appRequestNoVideo)
        assertTrue(impressionMock.getIsShowProcessed())

        val url = EndpointRepository.EndPoint.INTERSTITIAL_SHOW.defaultUrl
        verify(exactly = 1) { adUnitRendererAdCallback.onShowSuccess("id") }
        verify(exactly = 1) { adUnitRendererAdCallback.onImpressionSuccess("id") }
        verify(exactly = 1) {
            adUnitRendererShowRequestMock.execute(url, capture(paramSlot))
        }
        verify(exactly = 1) { openMeasurementControllerMock.signalImpressionEvent() }
        assertEquals("adid", paramSlot.captured.adId)
        assertEquals("test location", paramSlot.captured.location)
        assertEquals(AdType.Interstitial.name, paramSlot.captured.adTypeName)
        assertEquals(-1, paramSlot.captured.videoCached)
        assertEquals(mediationMock, paramSlot.captured.mediation)
        assertNull(appRequestNoVideo.adUnit)
    }

    @Ignore("HB-8129")
    @Test
    fun `impression shown fully and ad impression is not visible`() {
        val paramSlot = CapturingSlot<ShowParamsModel>()

        val url = EndpointRepository.EndPoint.INTERSTITIAL_SHOW.defaultUrl
        val viewBaseMock =
            mockk<ViewBase>().apply {
                every { webView = any() } just Runs
                every { webView } returns mockk()
            }
        viewBaseMock.webView = mockk<CBWebView>()
        every { impressionMock.getIsVisible() } returns false
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionShownFully(appRequestNoVideo)
        verify(exactly = 1) { adUnitRendererAdCallback.onShowSuccess("id") }
        verify(exactly = 0) { adUnitRendererAdCallback.onImpressionSuccess("id") }
        verify(exactly = 1) {
            adUnitRendererShowRequestMock.execute(url, capture(paramSlot))
        }
        verify(exactly = 0) { openMeasurementControllerMock.signalImpressionEvent() }
        assertEquals("adid", paramSlot.captured.adId)
        assertEquals("test location", paramSlot.captured.location)
        assertEquals(AdType.Interstitial.name, paramSlot.captured.adTypeName)
        assertEquals(-1, paramSlot.captured.videoCached)
        assertEquals(mediationMock, paramSlot.captured.mediation)
        assertNull(appRequestNoVideo.adUnit)
    }

    @Ignore("HB-8129")
    @Test
    fun `impression shown fully banner`() {
        val paramSlot = CapturingSlot<ShowParamsModel>()

        val url = EndpointRepository.EndPoint.INTERSTITIAL_SHOW.defaultUrl
        val viewBaseMock =
            mockk<ViewBase>().apply {
                every { webView = any() } just Runs
                every { webView } returns mockk()
            }
        viewBaseMock.webView = mockk<CBWebView>()
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionShownFully(appRequestNoVideo)
        verify(exactly = 1) { impressionMock.setIsShowProcessed(true) }
        verify(exactly = 1) { adUnitRendererAdCallback.onShowSuccess("id") }
        verify(exactly = 1) { adUnitRendererAdCallback.onImpressionSuccess("id") }
        verify(exactly = 1) { openMeasurementControllerMock.signalImpressionEvent() }

        verify(exactly = 1) {
            adUnitRendererShowRequestMock.execute(url, capture(paramSlot))
        }

        assertEquals("adid", paramSlot.captured.adId)
        assertEquals("test location", paramSlot.captured.location)
        assertEquals(AdType.Interstitial.name, paramSlot.captured.adTypeName)
        assertEquals(-1, paramSlot.captured.videoCached)
        assertEquals(mediationMock, paramSlot.captured.mediation)
        assertNull(appRequestNoVideo.adUnit)
    }

    @Test
    fun `impression error internal`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionError(appRequestNoVideo, CBError.Impression.INTERNAL)
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                "id",
                CBError.Impression.INTERNAL,
            )
        }
    }

    @Test
    fun `impression error impression already visible`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionError(
            appRequestNoVideo,
            CBError.Impression.IMPRESSION_ALREADY_VISIBLE,
        )
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                "id",
                CBError.Impression.IMPRESSION_ALREADY_VISIBLE,
            )
        }
    }

    @Test
    fun `impression error pending impression error`() {
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionError(
            appRequestNoVideo,
            CBError.Impression.PENDING_IMPRESSION_ERROR,
        )
        verify(exactly = 1) {
            adUnitRendererAdCallback.onShowFailure(
                "id",
                CBError.Impression.PENDING_IMPRESSION_ERROR,
            )
        }
    }

    @Test
    fun `on impression view created before show was processed`() {
        val slot = CapturingSlot<VisibilityTracker.VisibilityTrackerListener>()
        val contextMock: Context = mockk()
        val viewBaseMock = mockk<ViewBase>()
        val rootViewMock = mockk<View>()
        every { impressionMock.getIsShowProcessed() } returns false
        every { viewBaseMock.rootView } returns rootViewMock
        every { impressionMock.getViewProtocolView() } returns viewBaseMock
        every { openMeasurementControllerMock.isOmSdkEnabled() } returns true
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionViewCreated(contextMock)
        verify(exactly = 1) { openMeasurementControllerMock.isOmSdkEnabled() }
        verify(exactly = 1) {
            openMeasurementControllerMock.createVisibilityTracker(
                contextMock,
                viewBaseMock,
                rootViewMock,
                capture(slot),
            )
        }
        val listener = slot.captured
        assertNotNull(listener)
        listener.onVisibilityThresholdMet()
        verify(exactly = 0) { adUnitRendererAdCallback.onImpressionSuccess(any()) }
        verify(exactly = 0) { openMeasurementControllerMock.signalImpressionEvent() }
        assertTrue(impressionMock.getIsVisible())
        assertFalse(impressionMock.getIsShowProcessed())
    }

    @Test
    fun `on impression view created after show was processed`() {
        val slot = CapturingSlot<VisibilityTracker.VisibilityTrackerListener>()
        val contextMock: Context = mockk()
        val viewBaseMock = mockk<ViewBase>()
        val rootViewMock = mockk<View>()
        every { viewBaseMock.rootView } returns rootViewMock
        every { impressionMock.getViewProtocolView() } returns viewBaseMock
        every { openMeasurementControllerMock.isOmSdkEnabled() } returns true
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionViewCreated(contextMock)
        verify(exactly = 1) { openMeasurementControllerMock.isOmSdkEnabled() }
        verify(exactly = 1) {
            openMeasurementControllerMock.createVisibilityTracker(
                contextMock,
                viewBaseMock,
                rootViewMock,
                capture(slot),
            )
        }
        val listener = slot.captured
        assertNotNull(listener)
        listener.onVisibilityThresholdMet()
        verify(exactly = 1) { impressionMock.isViewProtocolViewNotCreated() }
        verify(exactly = 1) { impressionMock.getViewProtocolView() }
        verify(exactly = 1) { adUnitRendererAdCallback.onImpressionSuccess(impressionId) }
        verify(exactly = 1) { openMeasurementControllerMock.signalImpressionEvent() }
        verify(exactly = 1) {
            openMeasurementControllerMock.createVisibilityTracker(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `on impression view created after show was processed but impression was already signaled`() {
        every { impressionMock.wasImpressionSignaled } returns true
        val slot = CapturingSlot<VisibilityTracker.VisibilityTrackerListener>()
        val contextMock: Context = mockk()
        val viewBaseMock = mockk<ViewBase>()
        val rootViewMock = mockk<View>()
        every { viewBaseMock.rootView } returns rootViewMock
        every { impressionMock.getViewProtocolView() } returns viewBaseMock
        every { openMeasurementControllerMock.isOmSdkEnabled() } returns true
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onImpressionViewCreated(contextMock)
        verify(exactly = 1) { openMeasurementControllerMock.isOmSdkEnabled() }
        verify(exactly = 1) {
            openMeasurementControllerMock.createVisibilityTracker(
                contextMock,
                viewBaseMock,
                rootViewMock,
                capture(slot),
            )
        }
        val listener = slot.captured
        assertNotNull(listener)
        listener.onVisibilityThresholdMet()
        verify(exactly = 1) { impressionMock.isViewProtocolViewNotCreated() }
        verify(exactly = 1) { impressionMock.getViewProtocolView() }
        verify(exactly = 0) { adUnitRendererAdCallback.onImpressionSuccess(impressionId) }
        verify(exactly = 0) { openMeasurementControllerMock.signalImpressionEvent() }
        verify(exactly = 1) {
            openMeasurementControllerMock.createVisibilityTracker(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `impression on start`() {
        every { impressionMock.onStart() } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.impressionOnStart()
        verify(exactly = 1) { impressionMock.onStart() }
    }

    @Test
    fun `impression on resume`() {
        every { impressionMock.onResume() } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.impressionOnResume()
        verify(exactly = 1) { impressionMock.onResume() }
    }

    @Test
    fun `impression on pause`() {
        every { impressionMock.onPause() } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.impressionOnPause()
        verify(exactly = 1) { impressionMock.onPause() }
    }

    @Test
    fun `impression on destroy`() {
        every { impressionMock.closeImpression() } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.impressionOnDestroy()
        verify(exactly = 1) { impressionMock.closeImpression() }
    }

    @Test
    fun `activity is ready to display`() {
        every { impressionMock.getImpressionState() } returns ImpressionState.LOADED
        every { impressionMock.displayOnActivity(any(), any()) } just Runs
        every { rendererActivityBridgeMock.displayViewOnActivity(any()) } just Runs
        val activityMock = mockk<CBImpressionActivity>()
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onActivityIsReadyToDisplay(activityMock)
        verify(exactly = 1) {
            impressionMock.displayOnActivity(
                ImpressionState.LOADED,
                activityMock,
            )
        }
        verify(exactly = 1) { impressionMock.getViewProtocolView() }
        verify(exactly = 1) { rendererActivityBridgeMock.displayViewOnActivity(viewBaseMock) }
    }

    @Test
    fun `on configuration changed`() {
        every { impressionMock.viewProtocolConfigurationChange() } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.onConfigurationChange()
        verify(exactly = 1) { impressionMock.viewProtocolConfigurationChange() }
    }

    @Test
    fun `impression failure`() {
        every { impressionMock.onFailure(any()) } just Runs
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        adUnitRenderer.failure(CBError.Impression.PENDING_IMPRESSION_ERROR)
        verify(exactly = 1) { impressionMock.onFailure(CBError.Impression.PENDING_IMPRESSION_ERROR) }
    }

    @Test
    fun `on back pressed`() {
        every { rendererActivityBridgeMock.finishActivity() } just Runs
        every { impressionMock.isPlayerPlaying() } answers { false }
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        val backPressed = adUnitRenderer.onBackPressed()
        verify(exactly = 1) { rendererActivityBridgeMock.finishActivity() }
        assertTrue(backPressed)
    }

    @Test
    fun `on back pressed interstitial playing`() {
        every { rendererActivityBridgeMock.finishActivity() } just Runs
        every { impressionMock.isPlayerPlaying() } answers { true }
        every { impressionMock.canBeClosed() } answers { true }
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        val backPressed = adUnitRenderer.onBackPressed()
        verify(exactly = 1) { rendererActivityBridgeMock.finishActivity() }
        assertTrue(backPressed)
    }

    @Test
    fun `on back pressed rewarded playing`() {
        every { rendererActivityBridgeMock.finishActivity() } just Runs
        every { impressionMock.isPlayerPlaying() } answers { true }
        every { impressionMock.canBeClosed() } answers { false }
        adUnitRenderer.render(appRequestNoVideo, adUnitRendererAdCallback)
        val backPressed = adUnitRenderer.onBackPressed()
        verify(exactly = 0) { rendererActivityBridgeMock.finishActivity() }
        assertTrue(backPressed)
    }
}
