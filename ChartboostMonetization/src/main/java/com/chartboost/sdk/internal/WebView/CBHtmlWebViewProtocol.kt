package com.chartboost.sdk.internal.WebView

import android.content.Context
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.HtmlWebViewBase
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.tracking.EventTrackerExtensions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class CBHtmlWebViewProtocol(
    context: Context,
    location: String,
    mtype: MediaTypeOM,
    adUnitParameters: String,
    fileCache: FileCache,
    networkRequestService: CBNetworkService?,
    uiPoster: UiPoster,
    templateProxy: CBTemplateProxy?,
    mediation: Mediation?,
    private val baseUrl: String,
    private val html: String?,
    private val infoIcon: InfoIcon,
    openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    adUnitRendererCallback: AdUnitRendererImpressionCallback,
    private val impressionInterface: ImpressionInterface,
    webViewTimeoutInterface: WebViewTimeoutInterface,
    private val scripts: List<String>,
    private val eventTracker: EventTrackerExtensions,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val cbWebViewFactory: (Context) -> CBWebView = { CBHtmlWebView(it) },
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
        html,
        openMeasurementImpressionCallback,
        adUnitRendererCallback,
        webViewTimeoutInterface,
        eventTracker = eventTracker,
    ) {
    override fun createViewObject(context: Context): ViewBase? {
        if (html.isNullOrBlank()) {
            Logger.e("html must not be null or blank")
            return null
        }

        return try {
            HtmlWebViewBase(
                context,
                baseUrl,
                html,
                infoIcon,
                eventTracker,
                customWebViewInterface,
                impressionInterface,
                dispatcher,
                cbWebViewFactory,
            ).apply {
                webViewContainer?.let {
                    makeInfoIcon(it)
                } ?: Logger.e("webViewContainer null when creating HtmlWebViewBase")
            }
        } catch (e: Exception) {
            onWebViewError("Can't instantiate WebViewBase: $e")
            null
        }
    }

    override fun onConfigurationChange() {}

    override fun onPageFinishedWebView() {
        super.onPageFinishedWebView()
        impressionInterface.onShowImpression()
        view?.webView?.let { webView ->
            scripts.forEach { webView.evaluateJavascript(it, null) }
        }
    }
}
