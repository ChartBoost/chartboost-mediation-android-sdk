package com.chartboost.sdk.internal.AdUnitManager.impression

import android.view.ViewGroup
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.parsers.SDKBiddingTemplateParser
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.TemplateLoader
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.requests.ClickRequest
import com.chartboost.sdk.internal.Networking.requests.CompleteRequest
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.di.clickTracking
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.internal.impression.ImpressionCounter
import com.chartboost.sdk.internal.impression.ImpressionDependency
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.ImpressionIntermediateCallback
import com.chartboost.sdk.internal.impression.ImpressionViewProtocolBuilder
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import java.io.File

/**
 * Parameter needed by webview to work out if media player is gonna display video
 */
private const val PARAM_NATIVE_VIDEO_PLAYER = "{% native_video_player %}"
private const val MEDIA_TYPE_VIDEO = "video"

// TODO Big constructor, probably needs to be refactored further
internal class ImpressionBuilder(
    private val fileCache: FileCache,
    private val downloader: Downloader,
    private val urlResolver: UrlResolver,
    private val intentResolver: IntentResolver,
    private val adType: AdType,
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val mediation: Mediation?,
    private val measurementManager: OpenMeasurementManager,
    private val sdkBiddingTemplateParser: SDKBiddingTemplateParser,
    private val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    private val impressionFactory: (ImpressionDependency, ViewGroup?) -> CBImpression,
    private val eventTracker: EventTrackerExtensions,
    private val endpointRepository: EndpointRepository,
) : EventTrackerExtensions by eventTracker {
    /**
     * Prepare Impression and ImpressionHolder based on the asset files, template and
     * adunit data.
     */
    internal fun createImpressionHolderFromAppRequest(
        appRequest: AppRequest,
        callback: AdUnitRendererImpressionCallback,
        bannerView: ViewGroup?,
        impressionIntermediateCallback: ImpressionIntermediateCallback,
        impressionClickCallback: ImpressionClickCallback,
        viewProtocolBuilder: ImpressionViewProtocolBuilder,
        impressionInterface: ImpressionInterface,
        webViewTimeoutInterface: WebViewTimeoutInterface,
        nativeBridgeCommand: NativeBridgeCommand,
        templateLoader: TemplateLoader,
    ): ImpressionHolder {
        try {
            val baseDir = fileCache.currentLocations().getBaseDir()
            val adUnit = appRequest.adUnit
            val location = appRequest.location

            if (adUnit == null) {
                return ImpressionHolder(null, CBError.Impression.PENDING_IMPRESSION_ERROR)
            }

            // 0ms
            checkMissingAssets(adUnit, baseDir, location)?.let {
                return ImpressionHolder(null, it)
            }

            // 250ms
            val templateHtml =
                loadTemplateHtml(
                    templateLoader,
                    adUnit,
                    baseDir,
                    location,
                ) ?: return ImpressionHolder(null, CBError.Impression.ERROR_LOADING_WEB_VIEW)

            // 13ms
            val injectedHtml = measurementManager.injectOmidJsIntoHtml(templateHtml)

            // 3ms
            createImpressionObject(
                appRequest,
                adUnit,
                location,
                injectedHtml,
                callback,
                bannerView,
                impressionIntermediateCallback,
                impressionClickCallback,
                viewProtocolBuilder,
                impressionInterface,
                webViewTimeoutInterface,
                nativeBridgeCommand,
            ).let {
                return ImpressionHolder(it, null)
            }
        } catch (ex: Exception) {
            Logger.e("showReady exception:", ex)
            return ImpressionHolder(null, CBError.Impression.INTERNAL)
        }
    }

    private fun createImpressionObject(
        appRequest: AppRequest,
        adUnit: AdUnit,
        location: String,
        templateHtml: String,
        callback: AdUnitRendererImpressionCallback,
        bannerView: ViewGroup?,
        impressionIntermediateCallback: ImpressionIntermediateCallback,
        impressionClickCallback: ImpressionClickCallback,
        viewProtocolBuilder: ImpressionViewProtocolBuilder,
        impressionInterface: ImpressionInterface,
        webViewTimeoutInterface: WebViewTimeoutInterface,
        nativeBridgeCommand: NativeBridgeCommand,
    ): CBImpression {
        val impressionMediaType = calculateImpressionMediaType(adUnit.mediaType, adType)

        val clickRequest =
            ClickRequest(
                networkService = networkService,
                requestBodyBuilder = requestBodyBuilder,
                eventTracker = eventTracker,
                endpointRepository = endpointRepository,
            )

        val completeRequest =
            CompleteRequest(
                networkService = networkService,
                requestBodyBuilder = requestBodyBuilder,
                eventTracker = eventTracker,
                endpointRepository = endpointRepository,
            )

        val viewProtocol =
            viewProtocolBuilder.prepareViewProtocol(
                location,
                adUnit,
                adType.name,
                templateHtml,
                callback,
                impressionInterface,
                webViewTimeoutInterface,
                nativeBridgeCommand,
            )

        val impressionDependency =
            ImpressionDependency(
                urlResolver = urlResolver,
                intentResolver = intentResolver,
                clickRequest = clickRequest,
                clickTracking = clickTracking(adType.name, location, mediation, eventTracker),
                completeRequest = completeRequest,
                mediaType = impressionMediaType,
                openMeasurementImpressionCallback = openMeasurementImpressionCallback,
                appRequest = appRequest,
                downloader = downloader,
                viewProtocol = viewProtocol,
                adUnit = adUnit,
                adTypeTraits = adType,
                location = location,
                impressionCallback = impressionIntermediateCallback,
                impressionClickCallback = impressionClickCallback,
                adUnitRendererImpressionCallback = callback,
                eventTracker = eventTracker,
                impressionCounter = ImpressionCounter(),
            )

        return impressionFactory(impressionDependency, bannerView)
    }

    private fun calculateImpressionMediaType(
        adUnitMediaType: String,
        adType: AdType,
    ): ImpressionMediaType {
        return when (adType) {
            AdType.Interstitial -> prepareTypeInterstitial(adUnitMediaType)
            AdType.Rewarded -> ImpressionMediaType.INTERSTITIAL_REWARD_VIDEO
            AdType.Banner -> ImpressionMediaType.BANNER
        }
    }

    private fun prepareTypeInterstitial(adUnitMediaType: String): ImpressionMediaType {
        return if (adUnitMediaType == MEDIA_TYPE_VIDEO) {
            /** View for Interstitial Video  */
            ImpressionMediaType.INTERSTITIAL_VIDEO
        } else {
            /** View for Static Interstitial
             *  This is the default behavior for media types other than "video"
             *  such as "image", "gif", or "playable"
             */
            ImpressionMediaType.INTERSTITIAL
        }
    }

    private fun checkMissingAssets(
        adUnit: AdUnit,
        baseDir: File,
        location: String,
    ): CBError.Impression? {
        val assets = adUnit.assets
        if (assets.isEmpty()) {
            // empty asset is not an error
            return null
        }

        assets.values.forEach { asset ->
            asset.getFile(baseDir).apply {
                if (this == null || !exists()) {
                    Logger.e("Asset does not exist: " + asset.filename)
                    trackAssetError(location, asset.filename ?: "")
                    return CBError.Impression.ASSET_MISSING
                }
            }
        }
        return null
    }

    private fun loadTemplateHtml(
        templateLoader: TemplateLoader,
        adUnit: AdUnit,
        baseDir: File,
        location: String,
    ): String? {
        val body = adUnit.body
        if (body.getUrl().isNullOrEmpty()) {
            Logger.e("AdUnit does not have a template body")
            return null
        }

        val htmlFile = body.getFile(baseDir)
        val params = HashMap(adUnit.parameters)

        if (adUnit.templateParams.isNotEmpty() && adUnit.adm.isNotEmpty()) {
            sdkBiddingTemplateParser.parse(
                htmlFile,
                adUnit.templateParams,
                adUnit.adm,
            )?.let {
                // return only if not null otherwise fall through to the old parser
                return it
            }
        }

        // insert parameter needed by webview to work out if media player is gonna display video
        if (adUnit.videoUrl.isEmpty() || adUnit.videoFilename.isEmpty()) {
            params[PARAM_NATIVE_VIDEO_PLAYER] = "false"
        } else {
            params[PARAM_NATIVE_VIDEO_PLAYER] = "true"
        }

        adUnit.assets.entries.forEach {
            params[it.key] = it.value.filename
        }
        return templateLoader.formatTemplateHtml(
            htmlFile,
            params,
            adType.name,
            location,
        )
    }

    private fun trackAssetError(
        location: String,
        assetFilename: String,
    ) {
        CriticalEvent(
            TrackingEventName.Show.UNAVAILABLE_ASSET_ERROR,
            assetFilename,
            adType.name,
            location,
            mediation,
        ).track()
    }
}
