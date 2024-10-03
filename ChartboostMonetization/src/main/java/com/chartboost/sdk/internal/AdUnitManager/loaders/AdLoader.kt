package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.loaders.interceptors.AdUnitLoaderInterceptorChain
import com.chartboost.sdk.internal.AdUnitManager.parsers.AdUnitParser
import com.chartboost.sdk.internal.AdUnitManager.parsers.OpenRTBAdUnitParser
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.NetworkParameters
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyFields
import com.chartboost.sdk.internal.Networking.AdParameters
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.cbRequestHost
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest
import com.chartboost.sdk.internal.Networking.requests.OpenRTBRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject

internal interface AdLoader {
    fun loadAd(
        params: LoadParams,
        callback: LoadResult.() -> Unit,
    )

    fun JSONObject.getTrackingErrorWithResponseJson(
        error: String,
        response: String,
    ): String =
        apply {
            try {
                put("error", error)
                put("response", response)
            } catch (e: Exception) {
                Logger.e("Cannot create error json for the event", e)
            }
        }.toString()
}

internal data class LoadParams(
    val appRequest: AppRequest,
    val isCacheRequest: Boolean,
    val bannerHeight: Int?,
    val bannerWidth: Int?,
) {
    val interceptor = AdUnitLoaderInterceptorChain()
}

internal data class LoadResult(
    val appRequest: AppRequest,
    val adUnit: AdUnit? = null,
    val error: CBError? = null,
    val requestResponseCodeNs: Long = 0L,
    val readDataNs: Long = 0L,
)

internal fun LoadResult.fold(
    isSuccess: LoadResult.() -> Unit,
    isError: LoadResult.(CBError) -> Unit,
) {
    if (error == null) {
        isSuccess()
    } else {
        isError(error)
    }
}

internal class AdLoaderImpl(
    private val adTraits: AdType,
    val fileCache: FileCache,
    val requestBodyBuilder: RequestBodyBuilder,
    val networkService: CBNetworkService,
    private val adUnitParser: AdUnitParser,
    private val openRTBAdUnitParser: OpenRTBAdUnitParser,
    private val openMeasurementManager: OpenMeasurementManager,
    private val eventTracker: EventTrackerExtensions,
    private val endpointRepository: EndpointRepository,
) : AdLoader, CBRequest.CBAPINetworkResponseCallback, EventTrackerExtensions by eventTracker {
    private lateinit var requestBodyFields: RequestBodyFields
    private lateinit var params: LoadParams
    private lateinit var callback: (result: LoadResult) -> Unit

    override fun loadAd(
        params: LoadParams,
        callback: (result: LoadResult) -> Unit,
    ) {
        this.params = params
        this.callback = callback
        requestBodyFields = requestBodyBuilder.build()

        val req: CBRequest =
            buildRequest(
                params.appRequest.location,
                params.bannerHeight ?: 0,
                params.bannerWidth ?: 0,
                params.isCacheRequest,
                requestBodyFields,
                this,
                openMeasurementManager,
            )
        req.dispatch = CBNetworkRequest.Dispatch.ASYNC
        networkService.submit(req)
    }

    private fun parseResponse(
        requestBodyFields: RequestBodyFields,
        response: JSONObject,
        location: String,
    ): AdUnit? {
        return try {
            when {
                adTraits == AdType.Banner ->
                    openRTBAdUnitParser.parse(
                        AdType.Banner,
                        response,
                    )

                requestBodyFields.configurationFields.webViewEnabled -> adUnitParser.parse(response)
                else -> null
            }
        } catch (e: Exception) {
            CriticalEvent(
                name = TrackingEventName.Cache.GET_RESPONSE_PARSING_ERROR,
                message =
                    JSONObject().getTrackingErrorWithResponseJson(
                        error = e.message ?: "no message",
                        response = response.toString(),
                    ),
                adType = adTraits.name,
                location = location,
            ).track()
            null
        }
    }

    private fun buildRequest(
        location: String,
        height: Int,
        width: Int,
        isCacheRequest: Boolean,
        requestBodyFields: RequestBodyFields,
        callback: CBRequest.CBAPINetworkResponseCallback,
        openMeasurementManager: OpenMeasurementManager,
    ): CBRequest {
        val impressionCounter =
            when (adTraits) {
                AdType.Rewarded -> requestBodyFields.session.rewardedImpressionCounter
                AdType.Interstitial -> requestBodyFields.session.interstitialImpressionCounter
                else -> requestBodyFields.session.bannerImpressionCounter
            }

        return if (adTraits == AdType.Banner) {
            buildOpenRTBRequest(
                callback,
                height,
                width,
                location,
                impressionCounter,
                requestBodyFields,
                openMeasurementManager,
            )
        } else {
            buildWebViewRequest(
                callback,
                location,
                impressionCounter,
                isCacheRequest,
                requestBodyFields,
                openMeasurementManager,
            )
        }
    }

    private fun buildWebViewRequest(
        callback: CBRequest.CBAPINetworkResponseCallback,
        location: String,
        impressionCounter: Int,
        isCacheRequest: Boolean,
        requestBodyFields: RequestBodyFields,
        openMeasurementManager: OpenMeasurementManager,
    ): CBWebViewRequest {
        val url = endpointRepository.getEndPointUrl(adTraits.getEndPoint)
        val wvReq =
            CBWebViewRequest(
                CBNetworkRequest.Method.POST,
                url.cbRequestHost,
                url.path,
                requestBodyFields,
                Priority.NORMAL,
                null,
                callback,
                eventTracker,
            )

        val webAssetList: JSONObject = fileCache.webViewCacheAssets
        wvReq.appendWebViewBodyArgument(CBConstants.REQUEST_PARAM_ASSET_LIST, webAssetList)
        wvReq.appendWebViewBodyArgument(CBConstants.REQUEST_PARAM_LOCATION, location)
        wvReq.appendWebViewBodyArgument(CBConstants.REQUEST_PARAM_IMP_DEPTH, impressionCounter)

        if (openMeasurementManager.isOmSdkEnabled()) {
            openMeasurementManager.getOmidPartner()?.let {
                wvReq.appendWebViewBodySdkArgument("omidpn", it.name)
                wvReq.appendWebViewBodySdkArgument("omidpv", it.version)
            }
        }

        wvReq.appendWebViewBodyArgument("cache", isCacheRequest)
        wvReq.checkStatusInResponseBody = true
        return wvReq
    }

    private fun buildOpenRTBRequest(
        callback: CBRequest.CBAPINetworkResponseCallback,
        height: Int,
        width: Int,
        location: String,
        impressionCounter: Int,
        requestBodyFields: RequestBodyFields,
        openMeasurementManager: OpenMeasurementManager,
    ): OpenRTBRequest {
        val url = endpointRepository.getEndPointUrl(adTraits.getEndPoint)
        val networkParameters =
            NetworkParameters(
                url.cbRequestHost,
                url.path,
                requestBodyFields,
                Priority.NORMAL,
                callback,
            )

        val adParameters =
            AdParameters(
                adTraits,
                height,
                width,
                location,
                impressionCounter,
            )
        return OpenRTBRequest(networkParameters, adParameters, openMeasurementManager, eventTracker)
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        if (request == null || response == null) {
            callbackResultsWithError("Unexpected response")
        } else {
            parseResponse(
                requestBodyFields,
                params.interceptor.intercept(response),
                params.appRequest.location,
            )?.let {
                callbackResultsSuccess(it, request)
            } ?: callbackResultsWithError("Error parsing response")
        }
    }

    private fun callbackResultsSuccess(
        adUnit: AdUnit,
        request: CBRequest,
    ) {
        callback(
            LoadResult(
                params.appRequest,
                adUnit,
                null,
                request.readDataNs,
                request.getResponseCodeNs,
            ),
        )
    }

    private fun callbackResultsWithError(errorMsg: String) {
        callback(
            LoadResult(
                params.appRequest,
                error =
                    CBError(
                        type = CBError.Internal.UNEXPECTED_RESPONSE,
                        errorDesc = errorMsg,
                    ),
            ),
        )
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        callback(
            LoadResult(
                params.appRequest,
                error =
                    error ?: CBError(
                        CBError.Internal.INVALID_RESPONSE,
                        "Error parsing response",
                    ),
            ),
        )
    }
}
