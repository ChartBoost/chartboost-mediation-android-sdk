package com.chartboost.sdk.internal.impression

import android.content.Context
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.parsers.RenderingEngine
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.WebView.CBHtmlWebViewProtocol
import com.chartboost.sdk.internal.WebView.CBMraidWebViewProtocol
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.video.VideoProtocol
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.tracking.EventTrackerExtensions

internal class ImpressionViewProtocolBuilder(
    val context: Context,
    private val uiPoster: UiPoster,
    private val fileCache: FileCache,
    private val templateProxy: CBTemplateProxy,
    private val videoRepository: VideoRepository,
    private val mediation: Mediation?,
    private val networkService: CBNetworkService,
    private val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    private val eventTracker: EventTrackerExtensions,
) {
    fun prepareViewProtocol(
        location: String,
        adUnit: AdUnit,
        adTypeTraitsName: String,
        html: String,
        adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
        impressionInterface: ImpressionInterface,
        webViewTimeoutInterface: WebViewTimeoutInterface,
        nativeBridgeCommand: NativeBridgeCommand,
    ): CBViewProtocol {
        return when {
            adUnit.videoUrl.isNotEmpty() ->
                VideoProtocol(
                    context = context,
                    location = location,
                    mtype = adUnit.mtype,
                    adUnitParameters = adTypeTraitsName,
                    uiPoster = uiPoster,
                    fileCache = fileCache,
                    templateProxy = templateProxy,
                    videoRepository = videoRepository,
                    videoFilename = adUnit.videoFilename,
                    mediation = mediation,
                    adsVideoPlayerFactory = ChartboostDependencyContainer.applicationComponent.adsVideoPlayerFactory,
                    networkService = networkService,
                    templateHtml = html,
                    openMeasurementImpressionCallback = openMeasurementImpressionCallback,
                    adUnitRendererImpressionCallback = adUnitRendererImpressionCallback,
                    impressionInterface = impressionInterface,
                    webViewTimeoutInterface = webViewTimeoutInterface,
                    nativeBridgeCommand = nativeBridgeCommand,
                    eventTracker = eventTracker,
                )

            adUnit.renderingEngine == RenderingEngine.HTML ->
                CBHtmlWebViewProtocol(
                    context,
                    location,
                    adUnit.mtype,
                    adTypeTraitsName,
                    fileCache,
                    networkService,
                    uiPoster,
                    templateProxy,
                    mediation,
                    adUnit.baseUrl,
                    adUnit.decodedAdm,
                    adUnit.infoIcon,
                    openMeasurementImpressionCallback,
                    adUnitRendererImpressionCallback,
                    impressionInterface,
                    webViewTimeoutInterface,
                    adUnit.scripts,
                    eventTracker,
                )

            else ->
                CBMraidWebViewProtocol(
                    context,
                    location,
                    adUnit.mtype,
                    adTypeTraitsName,
                    fileCache,
                    networkService,
                    uiPoster,
                    templateProxy,
                    mediation,
                    html,
                    openMeasurementImpressionCallback,
                    adUnitRendererImpressionCallback,
                    impressionInterface,
                    webViewTimeoutInterface,
                    nativeBridgeCommand,
                    eventTracker,
                )
        }
    }
}
