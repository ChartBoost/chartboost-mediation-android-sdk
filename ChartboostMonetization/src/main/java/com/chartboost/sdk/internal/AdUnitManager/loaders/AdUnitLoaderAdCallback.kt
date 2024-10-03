package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.tracking.TrackingEventName

internal interface AdUnitLoaderAdCallback {
    fun onCacheSuccess(
        impressionId: String?,
        trackingEventName: TrackingEventName,
    )

    fun onAdFailToLoad(
        impressionId: String?,
        error: CBError.Type,
    )
}
