package com.chartboost.sdk.internal.identity

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure

private const val AMAZON_ZEROES = "00000000-0000-0000-0000-000000000000"
private const val AMAZON_AD_ID = "advertising_id"
private const val AMAZON_LIMIT_TRACKING = "limit_ad_tracking"

internal class AmazonAdvertisingId(
    context: Context,
    private val contentResolver: ContentResolver,
) : AdvertisingIdImpl(context) {
    /**
     * Retrieve the AD ID on Amazon devices
     * The amazon ad ID can be disabled or reset via Settings > Apps & Games > Advertising ID
     */
    override fun getAdvertisingIdHolder(): AdvertisingIDHolder {
        var advertisingIDState: TrackingState = TrackingState.TRACKING_UNKNOWN
        var advertisingID: String? = null
        try {
            val limitAdTracking = Secure.getInt(contentResolver, AMAZON_LIMIT_TRACKING) != 0
            val id = Secure.getString(contentResolver, AMAZON_AD_ID)

            if (limitAdTracking || id == AMAZON_ZEROES || isChildDirected()) {
                advertisingIDState = TrackingState.TRACKING_LIMITED
            } else {
                advertisingIDState = TrackingState.TRACKING_ENABLED
                advertisingID = id
            }
        } catch (ex: Settings.SettingNotFoundException) {
            // if there is no user visible ad setting uuid will still be sent
        }
        return AdvertisingIDHolder(advertisingIDState, advertisingID)
    }
}
