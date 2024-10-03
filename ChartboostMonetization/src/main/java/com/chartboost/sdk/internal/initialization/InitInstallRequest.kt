package com.chartboost.sdk.internal.initialization

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

internal class InitInstallRequest(
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTrackerExtensions,
    private val endpointRepository: EndpointRepository,
) : CBRequest.CBAPINetworkResponseCallback, EventTrackerExtensions by eventTracker {
    fun execute() {
        val url = endpointRepository.getEndPointUrl(EndpointRepository.EndPoint.INSTALL)
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
        networkService.submit(request)
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        // Ignore
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        val errorMsg = error?.errorDesc ?: "Install failure"
        ErrorEvent(
            TrackingEventName.Misc.INSTALL_REQUEST_ERROR,
            errorMsg,
        ).track()
    }
}
