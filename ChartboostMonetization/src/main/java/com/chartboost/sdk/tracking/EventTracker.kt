package com.chartboost.sdk.tracking

import com.chartboost.sdk.internal.Model.TrackingConfig

/**
 * EventTracker performs all the logic around tracking the device information and
 * sending this information to the server
 */
interface EventTracker {
    fun track(event: TrackingEvent)

    fun clear(
        type: String,
        location: String,
    )

    fun store(ad: TrackAd)

    fun refresh(config: TrackingConfig)

    fun persist(event: TrackingEvent)

    fun clearFromStorage(event: TrackingEvent)
}

// Syntactic sugar for interface delegation
interface EventTrackerExtensions : EventTracker {
    fun TrackingEvent.track(): TrackingEvent

    fun TrackAd.store(): TrackAd

    fun TrackingConfig.refresh(): TrackingConfig

    fun TrackingEvent.persist(): TrackingEvent

    fun TrackingEvent.clearFromStorage(): TrackingEvent
}
