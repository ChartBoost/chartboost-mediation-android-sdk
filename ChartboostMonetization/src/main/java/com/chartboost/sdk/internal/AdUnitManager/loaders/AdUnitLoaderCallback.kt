package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.tracking.TrackingEventName

internal interface AdUnitLoaderCallback {
    fun onAdUnitCacheSuccess(
        appRequest: AppRequest,
        trackingEventName: TrackingEventName,
    )
}
