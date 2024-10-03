package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName

internal typealias ImpressionTrackerRequestFactory = (String, EventTrackerExtensions) -> CBNetworkRequest<String>

internal class ImpressionTrackerRequest(
    url: String,
    eventTracker: EventTrackerExtensions,
) : CBNetworkRequest<String>(
        method = Method.GET,
        uri = url,
        priority = Priority.NORMAL,
        outputFile = null,
    ),
    EventTrackerExtensions by eventTracker {
    override fun deliverError(
        error: CBError?,
        serverResponse: CBNetworkServerResponse?,
    ) {
        Logger.e("Impression tracking request failed", error ?: Exception("Null CBError"))
        val trackingMessage = serverResponse?.asErrorMessage() ?: error?.asErrorMessage() ?: ""
        ErrorEvent(
            name = TrackingEventName.Impression.IMPRESSION_TRACKER_FAILURE,
            message = trackingMessage,
        ).track()
    }

    private fun CBNetworkServerResponse.asErrorMessage(): String? =
        if (isStatusOk()) {
            null
        } else {
            "Server error $statusCode for URL $uri"
        }

    private fun CBError.asErrorMessage(): String = "Error $type: $message for URL $uri"
}
