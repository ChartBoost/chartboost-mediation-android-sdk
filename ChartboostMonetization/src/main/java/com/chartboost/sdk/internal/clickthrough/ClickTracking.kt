package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.InfoEvent
import com.chartboost.sdk.tracking.TrackingEventName

internal interface ClickTracking {
    fun trackNavigationSuccess(message: String)

    fun trackNavigationFailure(message: String)
}

internal class ClickTrackingImpl(
    private val adType: String = "missing ad type",
    private val location: String = "missing location",
    private val mediation: Mediation? = null,
    eventTracker: EventTrackerExtensions,
) : ClickTracking, EventTrackerExtensions by eventTracker {
    override fun trackNavigationSuccess(message: String) {
        InfoEvent(
            TrackingEventName.Navigation.SUCCESS,
            message,
            adType,
            location,
            mediation,
        ).track()
    }

    override fun trackNavigationFailure(message: String) {
        ErrorEvent(
            TrackingEventName.Navigation.FAILURE,
            message,
            adType,
            location,
            mediation,
        ).track()
    }
}
