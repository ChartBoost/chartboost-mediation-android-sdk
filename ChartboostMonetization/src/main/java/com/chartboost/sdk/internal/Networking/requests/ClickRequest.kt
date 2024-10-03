package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.cbRequestHost
import com.chartboost.sdk.internal.Networking.requests.models.ClickParams
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EventTracker
import org.json.JSONObject

internal class ClickRequest(
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTracker,
    private val endpointRepository: EndpointRepository,
) : CBRequest.CBAPINetworkResponseCallback {
    private var callback: ClickRequestCallback? = null

    internal fun execute(
        callback: ClickRequestCallback?,
        params: ClickParams,
    ) {
        this.callback = callback
        val url = endpointRepository.getEndPointUrl(EndpointRepository.EndPoint.CLICK)
        val request =
            CBRequest(
                url.cbRequestHost,
                url.path,
                requestBodyBuilder.build(),
                Priority.NORMAL,
                this,
                eventTracker,
            )
        request.checkStatusInResponseBody = true
        appendApiClickRequestData(request, params)
        networkService.submit(request)
    }

    /**
     * Create click API request
     *
     * @param request
     * @return
     */
    private fun appendApiClickRequestData(
        request: CBRequest,
        params: ClickParams,
    ) {
        request.apply {
            appendBodyArgument(CBConstants.REQUEST_PARAM_AD_ID, params.adId)
            appendBodyArgument("to", params.to)
            appendBodyArgument("cgn", params.cgn)
            appendBodyArgument("creative", params.creative)
            appendBodyArgument(CBConstants.REQUEST_PARAM_LOCATION, params.location)

            if (params.impressionMediaType == ImpressionMediaType.BANNER) {
                request.appendBodyArgument("creative", "")
            } else {
                if (params.videoPosition != null && params.videoDuration != null) {
                    request.appendBodyArgument("total_time", params.videoDuration / 1000)
                    request.appendBodyArgument("playback_time", params.videoPosition / 1000)
                    Logger.d("TotalDuration: ${params.videoDuration} PlaybackTime: ${params.videoPosition}")
                }
            }

            params.retargetReinstall?.let { retarget ->
                request.appendBodyArgument(
                    CBConstants.REQUEST_PARAM_RETARGET_REINSTALL,
                    retarget,
                )
            }
        }
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        val json = CBJSON.walk(response, "response")
        callback?.onClickRequestSuccess(json)
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        val errorMsg = error?.message ?: "Click failure"
        callback?.onClickRequestFailure(errorMsg)
    }
}

interface ClickRequestCallback {
    fun onClickRequestSuccess(clickJson: JSONObject?)

    fun onClickRequestFailure(errorMsg: String?)
}
