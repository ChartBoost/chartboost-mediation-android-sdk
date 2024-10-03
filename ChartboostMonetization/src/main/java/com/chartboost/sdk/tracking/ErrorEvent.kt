package com.chartboost.sdk.tracking

import com.chartboost.sdk.Mediation

internal class ErrorEvent(
    name: TrackingEventName,
    message: String,
    adType: String = "",
    location: String = "",
    mediation: Mediation? = null,
) : TrackingEvent(
        name,
        message,
        adType,
        location,
        mediation,
        Type.ERROR,
        priority = Priority.HIGH,
    ) {
    // Constructor functions for no-args Java inter-op
    companion object {
        @JvmStatic
        fun instance(
            name: TrackingEventName,
            message: String,
        ): ErrorEvent = ErrorEvent(name, message)
    }
}
