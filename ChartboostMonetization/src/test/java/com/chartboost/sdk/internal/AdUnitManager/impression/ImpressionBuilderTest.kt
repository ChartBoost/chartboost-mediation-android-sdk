package com.chartboost.sdk.internal.AdUnitManager.impression

import android.view.ViewGroup
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.parsers.SDKBiddingTemplateParser
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.TemplateLoader
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Libraries.FileCacheLocations
import com.chartboost.sdk.internal.Model.Asset
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepositoryBase
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.ImpressionIntermediateCallback
import com.chartboost.sdk.internal.impression.ImpressionViewProtocolBuilder
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.test.impressionFactory
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ImpressionBuilderTest {
    private val downloaderMock = mockk<Downloader>()
    private val adTypeMock = AdType.Interstitial
    private val fileCacheMock = mockk<FileCache>()
    private val urlResolverMock = mockk<UrlResolver>()
    private val networkServiceMock = mockk<CBNetworkService>(relaxed = true)
    private val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    private val viewProtocolMock = mockk<CBViewProtocol>()
    private val openMeasurementManagerMock = mockk<OpenMeasurementManager>()
    private val sdkBiddingTemplateParser = mockk<SDKBiddingTemplateParser>()
    private val mediationMock = mockk<Mediation>()
    private val fileCacheLocationsMock = mockk<FileCacheLocations>()
    private val baseDirMock = File("test_directory") // mockk<File>()
    private val adUnitMock = mockk<AdUnit>()
    private val adUnitBodyMock = mockk<Asset>(relaxed = true)
    private val htmlFileMock = File("body")
    private val adUnitParameters = HashMap<String, String>()
    private val adUnitAssets = HashMap<String, Asset>()
    private val callbackMock = mockk<AdUnitRendererImpressionCallback>()
    private val openMeasurementImpressionCallbackMock = mockk<OpenMeasurementImpressionCallback>()
    private val impressionIntermediateCallback = mockk<ImpressionIntermediateCallback>()
    private val impressionClickCallback = mockk<ImpressionClickCallback>()
    private val impressionViewProtocolBuilder = mockk<ImpressionViewProtocolBuilder>()
    private val impressionInterfaceMock = mockk<ImpressionInterface>()
    private val webViewTimeoutInterfaceMock = mockk<WebViewTimeoutInterface>()
    private val nativeBridgeCommandMock = mockk<NativeBridgeCommand>()
    private val intentResolverMock = mockk<IntentResolver>()
    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()
    private val templateLoaderMock = mockk<TemplateLoader>()
    private val endpointRepositoryMock = mockk<EndpointRepositoryBase>()

    private val bannerView = mockk<ViewGroup>()
    private val location = "test"

    private val appRequest = AppRequest(1, location, null, null, adUnitMock)

    private val impressionBuilder =
        ImpressionBuilder(
            fileCacheMock,
            downloaderMock,
            urlResolverMock,
            intentResolverMock,
            adTypeMock,
            networkServiceMock,
            requestBodyBuilderMock,
            mediationMock,
            openMeasurementManagerMock,
            sdkBiddingTemplateParser,
            openMeasurementImpressionCallbackMock,
            ::impressionFactory,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    @Before
    fun setup() {
        htmlFileMock.writeText("<HTML><BODY>test content</BODY></HTML>")
        every { adUnitBodyMock.getFile(any()) } returns htmlFileMock
        every { adUnitMock.assets } returns adUnitAssets
        every { adUnitMock.body } returns adUnitBodyMock
        every { adUnitMock.parameters } returns adUnitParameters
        every { adUnitMock.videoUrl } returns ""
        every { adUnitMock.templateParams } returns ""
        every { adUnitMock.adm } returns ""
        every { sdkBiddingTemplateParser.parse(any(), any(), any()) } returns ""
        every { openMeasurementManagerMock.initialize() } just Runs
        every { openMeasurementManagerMock.getOmidPartner() } returns mockk()
        every { openMeasurementManagerMock.injectOmidJsIntoHtml(any()) } returns ""
        every {
            impressionViewProtocolBuilder.prepareViewProtocol(
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
        every { fileCacheLocationsMock.getBaseDir() } returns baseDirMock
        every { fileCacheMock.currentLocations() } returns fileCacheLocationsMock
        every { templateLoaderMock.formatTemplateHtml(any(), any(), any(), any()) } returns "template html"
    }

    @Test
    fun `create impression holder from app request valid`() {
        every { adUnitMock.videoUrl } returns "url.com"
        every { adUnitMock.mtype } returns MediaTypeOM.UNKNOWN
        every { adUnitMock.mediaType } returns "Interstitial"
        every { adUnitMock.videoFilename } returns "url"
        every { adUnitBodyMock.getUrl() } returns "url.com"
        every { templateLoaderMock.formatTemplateHtml(any(), any(), any(), any()) } returns "<HTML><BODY>test content</BODY></HTML>"

        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        verify(exactly = 1) {
            openMeasurementManagerMock.injectOmidJsIntoHtml("<HTML><BODY>test content</BODY></HTML>")
        }
        assertNotNull(impressionHolder)
        assertNotNull(impressionHolder.impression)
        assertNull(impressionHolder.error)
    }

    @Test
    fun `create impression holder from app request valid with sdk bidding`() {
        val expectedTemplate = "{ template: params: true }"
        every { adUnitMock.templateParams } returns "{ params: true }"
        every { adUnitMock.adm } returns "admadmadm"
        every { adUnitMock.mtype } returns MediaTypeOM.UNKNOWN
        every { adUnitMock.mediaType } returns "Interstitial"
        every { adUnitMock.videoUrl } returns "https://video.mp4"
        every { adUnitMock.videoFilename } returns "video.mp4"
        every { adUnitMock.template } returns expectedTemplate
        every { sdkBiddingTemplateParser.parse(any(), any(), any()) } returns expectedTemplate
        every { openMeasurementManagerMock.injectOmidJsIntoHtml(any()) } returns expectedTemplate
        every { adUnitBodyMock.getUrl() } returns "url.com"
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        verify(exactly = 1) { sdkBiddingTemplateParser.parse(any(), any(), any()) }
        verify(exactly = 1) { openMeasurementManagerMock.injectOmidJsIntoHtml(any()) }

        assertNotNull(impressionHolder)
        assertNotNull(impressionHolder.impression)
        assertNull(impressionHolder.error)
        assertEquals(expectedTemplate, impressionHolder.impression?.getAdUnitTemplate())
    }

    @Test
    fun `create impression holder missing ad unit`() {
        val appRequest = AppRequest(1, location, null, null, null)
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        assertNotNull(impressionHolder)
        assertNull(impressionHolder.impression)
        assertEquals(CBError.Impression.PENDING_IMPRESSION_ERROR, impressionHolder.error)
    }

    @Test
    fun `create impression holder missing assets`() {
        every { adUnitMock.mediaType } returns "Interstitial"
        adUnitAssets["test asset"] = Asset("test_directory", "test_asset", "http://test.com")
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        assertNotNull(impressionHolder)
        assertNull(impressionHolder.impression)
        assertEquals(CBError.Impression.ASSET_MISSING, impressionHolder.error)
    }

    @Test
    fun `create impression holder load template html error missing ad unit body`() {
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        assertNotNull(impressionHolder)
        assertNull(impressionHolder.impression)
        assertEquals(CBError.Impression.ERROR_LOADING_WEB_VIEW, impressionHolder.error)
    }

    @Test
    fun `create impression holder load template html with invalid characters`() {
        htmlFileMock.writeText("{{<HTML><BODY>test content</BODY></HTML>")
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        assertNull(impressionHolder.impression)
        assertEquals(CBError.Impression.ERROR_LOADING_WEB_VIEW, impressionHolder.error)
    }

    @Test
    fun `create impression holder exception`() {
        every { fileCacheMock.currentLocations() } returns null
        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callbackMock,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                impressionViewProtocolBuilder,
                impressionInterfaceMock,
                webViewTimeoutInterfaceMock,
                nativeBridgeCommandMock,
                templateLoaderMock,
            )
        assertNull(impressionHolder.impression)
        assertEquals(CBError.Impression.INTERNAL, impressionHolder.error)
    }
}
