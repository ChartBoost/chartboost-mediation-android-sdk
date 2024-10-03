package com.chartboost.sdk.internal.AdUnitManager.data

import android.view.ViewGroup
import com.chartboost.sdk.tracking.TrackAd

data class AdUnitBannerData(
    val bannerView: ViewGroup,
    val bannerWidth: Int,
    val bannerHeight: Int,
)

internal fun AdUnitBannerData?.toAdSize(): TrackAd.AdSize? =
    this?.run {
        TrackAd.AdSize(bannerHeight, bannerWidth)
    }
