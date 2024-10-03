package com.chartboost.sdk.internal.identity

import android.content.Context
import com.chartboost.sdk.internal.logging.Logger
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import java.io.IOException

private const val ZEROES = "00000000-0000-0000-0000-000000000000"

internal class GoogleAdvertisingId(private val context: Context) : AdvertisingIdImpl(context) {
    /**
     * Retrieve the AD ID on devices that have Google Play Services.
     * Can be disabled via Google Settings (or Settings > Google) > Ads > Opt out
     */
    override fun getAdvertisingIdHolder(): AdvertisingIDHolder {
        // Check COPPA to determine if we should obtain adid to comply with Google policy April 1, 2022
        if (isChildDirected()) {
            return AdvertisingIDHolder(
                TrackingState.TRACKING_LIMITED,
                null,
            )
        }

        var googlePlayServicesAdvertisingIDState: TrackingState = TrackingState.TRACKING_UNKNOWN
        var googlePlayServicesAdvertisingID: String? = null

        try {
            AdvertisingIdClient.getAdvertisingIdInfo(context).let {
                if (it.isLimitAdTrackingEnabled) {
                    googlePlayServicesAdvertisingIDState = TrackingState.TRACKING_LIMITED
                    googlePlayServicesAdvertisingID = null
                } else {
                    googlePlayServicesAdvertisingIDState = TrackingState.TRACKING_ENABLED
                    googlePlayServicesAdvertisingID = it.id

                    // Add extra check for zeroed-out AdvertisingID applied from 2022(?)
                    if (ZEROES == googlePlayServicesAdvertisingID) {
                        googlePlayServicesAdvertisingIDState = TrackingState.TRACKING_LIMITED
                        googlePlayServicesAdvertisingID = null
                    }
                }
            }
        } catch (illegalStateException: IllegalStateException) {
            Logger.e(
                "This should have been called off the main thread.",
                illegalStateException,
            )
        } catch (ioException: IOException) {
            Logger.e(
                "The connection to Google Play Services failed.",
                ioException,
            )
        } catch (googlePlayServicesRepairableException: GooglePlayServicesRepairableException) {
            Logger.e(
                "There was a recoverable error connecting to Google Play Services.",
                googlePlayServicesRepairableException,
            )
        } catch (googlePlayServicesNotAvailableException: GooglePlayServicesNotAvailableException) {
            Logger.e(
                "Google play service is not available.",
                googlePlayServicesNotAvailableException,
            )
        }
        return AdvertisingIDHolder(
            googlePlayServicesAdvertisingIDState,
            googlePlayServicesAdvertisingID,
        )
    }
}
