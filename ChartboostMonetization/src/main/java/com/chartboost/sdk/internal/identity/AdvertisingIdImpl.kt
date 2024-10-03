package com.chartboost.sdk.internal.identity

import android.content.Context
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.privacy.model.COPPA

internal abstract class AdvertisingIdImpl(
    private val context: Context,
) : AdvertisingId {
    fun isChildDirected(): Boolean {
        // coppa not set should be treated as isChildDirected = false
        return try {
            Chartboost.getDataUseConsent(context, COPPA.COPPA_STANDARD)?.consent as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
