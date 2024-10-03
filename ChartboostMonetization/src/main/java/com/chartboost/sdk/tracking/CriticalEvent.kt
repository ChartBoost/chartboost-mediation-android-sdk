package com.chartboost.sdk.tracking

import com.chartboost.sdk.Mediation

internal class CriticalEvent(
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
        Type.CRITICAL,
        trackAd = trackAd,
        priority = Priority.HIGH,
    ) {
    // Constructor functions for default args Java inter-op
    companion object {
        @JvmStatic
        fun instance(
            name: TrackingEventName,
            message: String,
            adType: String,
            location: String,
        ): CriticalEvent = CriticalEvent(name, message, adType, location)

        @JvmStatic
        fun instance(
            name: TrackingEventName,
            message: String,
        ): CriticalEvent = CriticalEvent(name, message)
    }
}
