package com.chartboost.sdk.internal.AdUnitManager.data

internal data class AppRequest(
    val id: Int,
    val location: String,
    var bidResponse: String?,
    var bannerData: AdUnitBannerData? = null,
    var adUnit: AdUnit? = null,
    var isTrackedCache: Boolean = false,
    var isTrackedShow: Boolean = false,
)
