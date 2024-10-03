package com.chartboost.sdk.internal.initialization

import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.cbRequestHost
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject

internal class InitConfigRequest(
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTrackerExtensions,
    private val endpointRepository: EndpointRepository,
) : CBRequest.CBAPINetworkResponseCallback, EventTrackerExtensions by eventTracker {
    private var callback: ConfigRequestCallback? = null

    fun execute(callback: ConfigRequestCallback) {
        this.callback = callback
        val url = endpointRepository.getEndPointUrl(EndpointRepository.EndPoint.CONFIG)
        // TODO Refactor CBRequest to take URL as destination directly
        val request =
            CBRequest(
                url.cbRequestHost,
                url.path,
                requestBodyBuilder.build(),
                Priority.HIGH,
                this,
                eventTracker,
            )
        request.checkStatusInResponseBody = true
        networkService.submit(request)
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        val configJson = CBJSON.walk(response, "response")
        callback?.onConfigRequestSuccess(configJson)
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        val errorMsg = error?.errorDesc ?: "Config failure"
        ErrorEvent(
            TrackingEventName.Misc.CONFIG_REQUEST_ERROR,
            errorMsg,
        ).track()
        callback?.onConfigRequestFailure(errorMsg)
    }
}
