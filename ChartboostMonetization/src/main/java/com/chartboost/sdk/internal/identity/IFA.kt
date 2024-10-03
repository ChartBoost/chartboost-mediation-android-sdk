package com.chartboost.sdk.internal.identity

import android.content.Context
import android.os.Build
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.DeviceInfo

/**
 * Identifier for Advertising
 */
internal class IFA(
    private val googleAdvertisingId: GoogleAdvertisingId,
    private val amazonAdvertisingId: AmazonAdvertisingId,
    private val manufacturer: String = Build.MANUFACTURER,
) {
    fun getAdvertisingIdHolder(): AdvertisingIDHolder {
        return try {
            if (isAmazonDevice()) {
                amazonAdvertisingId.getAdvertisingIdHolder()
            } else {
                googleAdvertisingId.getAdvertisingIdHolder()
            }
        } catch (e: Exception) {
            Logger.e("getAdvertisingId error", e)
            return AdvertisingIDHolder(TrackingState.TRACKING_UNKNOWN, "")
        }
    }

    fun getLocalAdvertisingId(
        context: Context,
        isTrackingLimited: Boolean,
    ): String {
        return DeviceInfo.getUniqueId(context, isTrackingLimited)
    }

    private fun isAmazonDevice(): Boolean {
        return "Amazon".equals(manufacturer, ignoreCase = true)
    }
}
