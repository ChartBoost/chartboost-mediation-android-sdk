package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.cbRequestHost
import com.chartboost.sdk.internal.Networking.requests.models.CompleteParamsModel
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EventTracker
import org.json.JSONObject

internal class CompleteRequest(
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTracker,
    private val endpointRepository: EndpointRepository,
) : CBRequest.CBAPINetworkResponseCallback {
    private var callback: CompleteRequestCallback? = null

    fun execute(
        callback: CompleteRequestCallback?,
        params: CompleteParamsModel,
    ) {
        this.callback = callback
        val url = endpointRepository.getEndPointUrl(EndpointRepository.EndPoint.VIDEO_COMPLETE)
        val request =
            CBRequest(
                url.cbRequestHost,
                url.path,
                requestBodyBuilder.build(),
                Priority.NORMAL,
                this,
                eventTracker,
            )
        appendApiClickRequestData(request, params)
        networkService.submit(request)
    }

    /**
     * Create Complete API request
     *
     * @param request
     * @return
     */
    private fun appendApiClickRequestData(
        request: CBRequest,
        params: CompleteParamsModel,
    ) {
        request.apply {
            appendBodyArgument(CBConstants.REQUEST_PARAM_LOCATION, params.location)
            appendBodyArgument("reward", params.rewardAmount)
            appendBodyArgument("currency-name", params.rewardCurrency)
            appendBodyArgument(CBConstants.REQUEST_PARAM_AD_ID, params.adId)
            appendBodyArgument("force_close", false)
            appendBodyArgument("cgn", params.cgn)
            if (params.videoPostion != null && params.videoDuration != null) {
                appendBodyArgument("total_time", params.videoDuration / 1000)
                appendBodyArgument("playback_time", params.videoPostion / 1000)
                Logger.d("TotalDuration: ${params.videoDuration} PlaybackTime: ${params.videoPostion}")
            }
        }
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        val json = CBJSON.walk(response, "response")
        callback?.onCompleteRequestSuccess(json)
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        val errorMsg = error?.message ?: "Click failure"
        callback?.onCompleteRequestFailure(errorMsg)
    }
}

interface CompleteRequestCallback {
    fun onCompleteRequestSuccess(completeJson: JSONObject?)

    fun onCompleteRequestFailure(errorMsg: String?)
}
