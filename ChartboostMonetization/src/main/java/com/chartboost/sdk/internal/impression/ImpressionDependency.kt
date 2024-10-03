package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Networking.requests.ClickRequest
import com.chartboost.sdk.internal.Networking.requests.CompleteRequest
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ClickTracking
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.tracking.EventTrackerExtensions

internal data class ImpressionDependency(
    val urlResolver: UrlResolver,
    val intentResolver: IntentResolver,
    val clickRequest: ClickRequest,
    val clickTracking: ClickTracking,
    val completeRequest: CompleteRequest,
    val mediaType: ImpressionMediaType,
    val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    val appRequest: AppRequest,
    val downloader: Downloader,
    val viewProtocol: CBViewProtocol,
    val impressionCounter: ImpressionCounter,
    val adUnit: AdUnit, // AdUnit is still used in multiple places
    val adTypeTraits: AdType, // AdType is still used in multiple places
    val location: String, // location is still used in multiple places
    val impressionCallback: ImpressionIntermediateCallback, // ImpressionIntermediateCallback is still used in multiple places UIManager
    val impressionClickCallback: ImpressionClickCallback,
    val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
    val eventTracker: EventTrackerExtensions,
)
