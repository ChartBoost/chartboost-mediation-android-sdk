package com.chartboost.sdk.test

import android.view.ViewGroup
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Networking.requests.ClickRequest
import com.chartboost.sdk.internal.Networking.requests.CompleteRequest
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ImpressionClick
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.internal.impression.ImpressionComplete
import com.chartboost.sdk.internal.impression.ImpressionCounter
import com.chartboost.sdk.internal.impression.ImpressionDependency
import com.chartboost.sdk.internal.impression.ImpressionDismiss
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.ImpressionView
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.mockk

internal fun impressionFactory(
    impressionDependency: ImpressionDependency,
    viewGroup: ViewGroup?,
): CBImpression =
    with(impressionDependency) {
        CBImpression(
            impressionDependency,
            ImpressionClick(
                adUnit,
                urlResolver,
                intentResolver,
                clickRequest,
                clickTracking,
                mediaType,
                impressionClickCallback,
                openMeasurementImpressionCallback,
                adUnitRendererImpressionCallback,
            ),
            ImpressionDismiss(
                adUnit,
                location,
                adTypeTraits,
                adUnitRendererImpressionCallback,
                impressionCallback,
                appRequest,
                downloader,
                openMeasurementImpressionCallback,
                eventTracker = eventTracker,
            ),
            ImpressionComplete(
                adUnit,
                adTypeTraits,
                completeRequest,
                adUnitRendererImpressionCallback,
            ),
            ImpressionView(
                appRequest,
                viewProtocol,
                downloader,
                viewGroup,
                adUnitRendererImpressionCallback,
                impressionCallback,
                impressionClickCallback,
            ),
        )
    }

internal class ImpressionBuilder(tc: TestContainer, adType: AdType?) {
    private val tc: TestContainer
    private val adType: AdType?
    private var location = TestUtils.randomString("the location")
    private var templateHtml: String?
    private var adUnit: AdUnit?
    private var callback = mockk<AdUnitRendererImpressionCallback>()

    init {
        val webview = tc.sdkConfig.get().isWebviewEnabled
        this.tc = tc
        this.adType = adType
        templateHtml = if (webview) TestUtils.randomString("the template html") else null
        adUnit = tc.minimalAdUnit()
    }

    fun build(): CBImpression {
        val adUnitRendererImpressionCallbackMock = mockk<AdUnitRendererImpressionCallback>()

        val impressionInterfaceMock = mockk<ImpressionInterface>()

        val webViewTimeoutInterfaceMock = mockk<WebViewTimeoutInterface>()

        val nativeBridgeCommandMock = mockk<NativeBridgeCommand>()

        val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()

        val dependency =
            ImpressionDependency(
                tc.urlResolver,
                tc.intentResolver,
                mockk<ClickRequest>(),
                mockk(),
                mockk<CompleteRequest>(),
                mockk<ImpressionMediaType>(),
                tc.openMeasurementImpressionCallback,
                AppRequest(1, location, null, null, adUnit, false, false),
                tc.downloader,
                tc.newCBWebViewProtocol(
                    adUnitRendererImpressionCallbackMock,
                    impressionInterfaceMock,
                    webViewTimeoutInterfaceMock,
                    nativeBridgeCommandMock,
                    eventTracker = eventTrackerMock,
                ),
                impressionCounter = ImpressionCounter(),
                adUnit!!,
                mockk<AdType>(),
                "default",
                mockk(),
                mockk(),
                adUnitRendererImpressionCallbackMock,
                eventTracker = eventTrackerMock,
            )
        return impressionFactory(dependency, null)
    }

    fun buildMock(): CBImpression {
        val impression = mockk<CBImpression>()
        TestUtils.setFieldWithReflection(impression, CBImpression::class.java, "adUnit", adUnit)
        TestUtils.setFieldWithReflection(impression, CBImpression::class.java, "callback", callback)
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "fileCache",
            tc.fileCache,
        )
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "networkService",
            tc.networkService,
        )
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "requestBodyBuilder",
            tc.requestBodyBuilder,
        )
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "sharedPreferences",
            tc.sharedPreferences,
        )
        TestUtils.setFieldWithReflection(impression, CBImpression::class.java, "adType", adType)
        TestUtils.setFieldWithReflection(impression, CBImpression::class.java, "location", location)
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "templateHtml",
            templateHtml,
        )
        TestUtils.setFieldWithReflection(
            impression,
            CBImpression::class.java,
            "mediation",
            tc.mediation,
        )
        return impression
    }

    fun withLocation(location: String?): ImpressionBuilder {
        this.location = location
        return this
    }

    fun withCallback(callback: AdUnitRendererImpressionCallback): ImpressionBuilder {
        this.callback = callback
        return this
    }
}
