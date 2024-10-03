package com.chartboost.sdk.tracking

import com.chartboost.sdk.Mediation

internal class InfoEvent(
    name: TrackingEventName,
    message: String,
    adType: String = "",
    location: String = "",
    mediation: Mediation? = null,
    trackAd: TrackAd = TrackAd(),
) : TrackingEvent(
        name,
        message,
        adType,
        location,
        mediation,
        Type.INFO,
        trackAd = trackAd,
        priority = Priority.LOW,
    ) {
    init {
        if (isHighPriority()) {
            priority = Priority.HIGH
            isLatencyEvent = true
        }
    }

    companion object {
        @JvmStatic
        fun instance(
            name: TrackingEventName,
            message: String,
            adType: String,
            location: String,
            mediation: Mediation?,
        ): InfoEvent = InfoEvent(name, message, adType, location, mediation)
    }

    private fun isHighPriority(): Boolean {
        return when (name) {
            TrackingEventName.Cache.FINISH_SUCCESS,
            TrackingEventName.Cache.FINISH_FAILURE,
            TrackingEventName.Show.FINISH_SUCCESS,
            TrackingEventName.Show.FINISH_FAILURE,
            -> true
            else -> false
        }
    }
}
