package com.chartboost.sdk.internal.identity

internal interface AdvertisingId {
    fun getAdvertisingIdHolder(): AdvertisingIDHolder
}

/**
 * advertisingIDState - stores whether we've retrieved the ad ID
 * advertisingID - stores the ad ID once we've retrieved it
 */
internal data class AdvertisingIDHolder(
    val advertisingIDState: TrackingState,
    val advertisingID: String?,
)
