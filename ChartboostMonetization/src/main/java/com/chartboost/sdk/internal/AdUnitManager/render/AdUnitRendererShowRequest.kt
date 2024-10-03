package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.cbRequestHost
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Networking.requests.models.ShowParamsModel
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject
import java.net.URL

internal class AdUnitRendererShowRequest(
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTrackerExtensions,
) : CBRequest.CBAPINetworkResponseCallback, EventTrackerExtensions by eventTracker {
    private lateinit var showParams: ShowParamsModel

    fun execute(
        url: URL,
        showParams: ShowParamsModel,
    ) {
        this.showParams = showParams
        val request =
            CBRequest(
                url.cbRequestHost,
                url.path,
                requestBodyBuilder.build(),
                Priority.NORMAL,
                this,
                eventTracker,
            )
        request.dispatch = CBNetworkRequest.Dispatch.ASYNC
        attachArguments(request, showParams)
        networkService.submit(request)
    }

    private fun attachArguments(
        request: CBRequest,
        showParams: ShowParamsModel,
    ) {
        // CBImpressionManager conditionally sets this to 0 or 1, but always
        // sets the controlling boolean to false before doing so.
        request.appendBodyArgument(CBConstants.REQUEST_PARAM_CACHED, "0")
        request.appendBodyArgument(CBConstants.REQUEST_PARAM_LOCATION, showParams.location)
        val cachedVideoState = showParams.videoCached
        if (cachedVideoState >= 0) {
            request.appendBodyArgument(CBConstants.REQUEST_PARAM_VIDEO_CACHED, cachedVideoState)
        }
        val adId = showParams.adId
        if (!adId.isNullOrEmpty()) {
            request.appendBodyArgument(CBConstants.REQUEST_PARAM_AD_ID, adId)
        }
    }

    override fun onSuccess(
        request: CBRequest?,
        response: JSONObject?,
    ) {
        // ignore
    }

    override fun onFailure(
        request: CBRequest?,
        error: CBError?,
    ) {
        ErrorEvent(
            TrackingEventName.Show.REQUEST_ERROR,
            error?.message ?: "Show failure",
            showParams.adTypeName,
            showParams.location,
            showParams.mediation,
        ).track()
    }
}
