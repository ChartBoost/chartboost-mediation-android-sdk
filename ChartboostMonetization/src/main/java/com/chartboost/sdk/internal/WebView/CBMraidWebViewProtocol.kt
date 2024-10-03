package com.chartboost.sdk.internal.WebView

import android.content.Context
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent

internal class CBMraidWebViewProtocol(
    context: Context,
    location: String,
    mtype: MediaTypeOM,
    adUnitParameters: String,
    fileCache: FileCache,
    networkRequestService: CBNetworkService?,
    uiPoster: UiPoster,
    templateProxy: CBTemplateProxy?,
    mediation: Mediation?,
    private val templateHtml: String?,
    openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    adUnitRendererCallback: AdUnitRendererImpressionCallback,
    private val impressionInterface: ImpressionInterface,
    webViewTimeoutInterface: WebViewTimeoutInterface,
    private val nativeBridgeCommand: NativeBridgeCommand,
    private val eventTracker: EventTrackerExtensions,
) : CBViewProtocol(
        context,
        location,
        mtype,
        adUnitParameters,
        uiPoster,
        fileCache,
        networkRequestService,
        templateProxy,
        mediation,
        templateHtml,
        openMeasurementImpressionCallback,
        adUnitRendererCallback,
        webViewTimeoutInterface,
        eventTracker = eventTracker,
    ) {
    override fun createViewObject(context: Context): ViewBase? {
        nativeBridgeCommand.setImpressionInterface(impressionInterface)
        if (templateHtml.isNullOrBlank()) {
            Logger.e("templateHtml must not be null or blank")
            return null
        }

        return try {
            MraidWebViewBase(
                context,
                templateHtml,
                customWebViewInterface,
                baseExternalPathURL,
                nativeBridgeCommand,
                eventTracker = eventTracker,
            )
        } catch (e: Exception) {
            onWebViewError("Can't instantiate MraidWebViewBase: $e")
            null
        }
    }

    override fun onConfigurationChange() {}

    // This shouldn't be necessary, but defaultOrientationProperties_Check fails to pass without it.
    // The failure indicates that track() has not been implemented.
    override fun track(event: TrackingEvent) {
        super.track(event)
    }
}
