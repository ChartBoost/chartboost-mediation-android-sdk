package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBUtility
import com.chartboost.sdk.internal.Libraries.toByteArray
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkRequestInfo
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.NetworkHelper
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EventTracker
import com.chartboost.sdk.tracking.TrackingEventCache
import org.json.JSONArray
import org.json.JSONObject

private const val PROTOCOL_CONTENT_TYPE = "application/json"

internal class TrackingRequest(
    private val networkService: CBNetworkService,
    private val trackingEventCache: TrackingEventCache,
    private val jsonFactory: (collection: Collection<JSONObject>) -> JSONArray = ::JSONArray,
    private val eventTracker: EventTracker,
) {
    fun execute(
        url: String,
        events: List<JSONObject>,
    ) {
        TrackingRequestExtension(
            url,
            trackingEventCache,
            eventTracker = eventTracker,
        ).apply {
            bodyArray = jsonFactory(events)
        }.let(networkService::submit)
    }
}

/**
 * Special case of [CBRequest] that creates custom body just for the tracking call.
 */
internal class TrackingRequestExtension(
    url: String,
    private val trackingEventCache: TrackingEventCache,
    callback: CBAPINetworkResponseCallback =
        object : CBAPINetworkResponseCallback {
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
                Logger.d("Request ${request?.uri} failed!")
                request?.bodyArray?.let { trackingEventCache.cacheEventJsonBodyAfterRequestFailure(it) }
            }
        },
    eventTracker: EventTracker,
) : CBRequest(
        NetworkHelper.getEndpointFromUrl(url),
        NetworkHelper.getPathFromUrl(url),
        null,
        Priority.NORMAL,
        callback,
        eventTracker,
    ) {
    init {
        checkStatusInResponseBody = false
    }

    override fun buildRequestInfo(): CBNetworkRequestInfo {
        return CBNetworkRequestInfo(
            getHeaders(),
            bodyArray.toByteArray(),
            PROTOCOL_CONTENT_TYPE,
        )
    }

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            CBConstants.REQUEST_PARAM_ACCEPT_HEADER_KEY to CBConstants.REQUEST_PARAM_HEADER_VALUE,
            CBConstants.REQUEST_PARAM_CLIENT_HEADER_KEY to CBUtility.getUserAgent(),
            CBConstants.REQUEST_PARAM_HEADER_KEY to CBConstants.API_VERSION,
        )
    }
}
