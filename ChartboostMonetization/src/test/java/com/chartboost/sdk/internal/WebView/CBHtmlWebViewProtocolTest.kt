package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.CBConstants.API_ENDPOINT
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

class CBHtmlWebViewProtocolTest {
    private val contextMock: Context = mockk()
    private val uiPosterMock: UiPoster = mockk(relaxed = true)
    private val fileCacheMock: FileCache = mockk()
    private val templateProxyMock: CBTemplateProxy = mockk()
    private val networkService: CBNetworkService = mockk()
    private val infoIconMock: InfoIcon = mockk()
    private val openMeasurementImpressionCallbackMock = mockk<OpenMeasurementImpressionCallback>()
    private val adUnitRendererCallbackMock = mockk<AdUnitRendererImpressionCallback>()
    private val impressionInterfaceMock = mockk<ImpressionInterface>()
    private val webViewTimeoutInterface = mockk<WebViewTimeoutInterface>()

    private val dispatcher = StandardTestDispatcher()

    private val mediation =
        Mediation(
            "mediation",
            "1.2.3",
            "3.2.1",
        )
    private val eventTrackerMock =
        relaxedMockk<EventTrackerExtensions>().apply {
            every { any<TrackingEvent>().track() } answers { firstArg() }
        }

    private lateinit var protocol: CBHtmlWebViewProtocol

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        val resourcesMock = mockk<Resources>()
        val displayMetricsMock = mockk<DisplayMetrics>()

        displayMetricsMock.density = 2.0f
        every { resourcesMock.displayMetrics } returns displayMetricsMock
        every { contextMock.resources } returns resourcesMock

        every { impressionInterfaceMock.onWebViewError(any()) } returns CBError.Impression.INTERNAL

        every {
            openMeasurementImpressionCallbackMock.onImpressionNotifyFriendlyObstructionCreated(
                any(),
            )
        } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionDestroyWebview() } just Runs

        protocol = getProtocolByOMtype(MediaTypeOM.HTML)
    }

    private fun getProtocolByOMtype(omTypeOM: MediaTypeOM): CBHtmlWebViewProtocol {
        return CBHtmlWebViewProtocol(
            context = contextMock,
            location = "default",
            mtype = omTypeOM,
            adUnitParameters = "banner",
            uiPoster = uiPosterMock,
            fileCache = fileCacheMock,
            templateProxy = templateProxyMock,
            mediation = mediation,
            networkRequestService = networkService,
            baseUrl = API_ENDPOINT,
            html = "<html></html>",
            infoIcon = infoIconMock,
            openMeasurementImpressionCallback = openMeasurementImpressionCallbackMock,
            adUnitRendererCallback = adUnitRendererCallbackMock,
            impressionInterface = impressionInterfaceMock,
            webViewTimeoutInterface = webViewTimeoutInterface,
            scripts = emptyList(),
            eventTracker = eventTrackerMock,
            dispatcher = dispatcher,
            cbWebViewFactory = { relaxedMockk() },
        )
    }

    @Test
    fun createViewObjectSuccessTest() {
        val view = createWebViewBase()
        view.shouldNotBeNull()
    }

    @Test
    fun destroyViewBaseTest() {
        createWebViewBase()
        protocol.destroy()
    }

    @Test
    fun destroyViewTest() {
        createWebViewBase()
        protocol.destroy()
        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionDestroyWebview() }
    }

    @Test
    fun onConfigurationChangeTest() {
        createWebViewBase()
        protocol.onConfigurationChange()
    }

    private fun createWebViewBase(): ViewBase? {
        infoIconMock.apply {
            every { position } returns relaxedMockk<InfoIcon.Position>()
            every { margin } returns relaxedMockk<InfoIcon.DoubleSize>()
            every { padding } returns relaxedMockk<InfoIcon.DoubleSize>()
            every { size } returns relaxedMockk<InfoIcon.DoubleSize>()
        }
        return protocol.createViewObject(contextMock)
    }
}
