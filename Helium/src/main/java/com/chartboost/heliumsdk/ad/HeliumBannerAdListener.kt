/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.util.Size
import android.view.View
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException

/**
 * Use to listen to Helium banner ad events.
 */
@JvmDefaultWithCompatibility
interface HeliumBannerAdListener {
    /**
     * Called when an ad is cached or fails to cache.
     *
     * @param placementName Indicates which placement cached the ad.
     * @param loadId A unique identifier for this load request.
     * @param winningBidInfo Map of winning bid information such as price.
     * @param error null if the cache was successful, an error if it failed to cache.
     */
    fun onAdCached(
        placementName: String,
        loadId: String,
        winningBidInfo: Map<String, String>,
        error: ChartboostMediationAdException?,
    )

    /**
     * Called when an ad is cached or fails to cache. By default this overload calls the function
     * without the bannerSize parameter.
     *
     * @param placementName Indicates which placement cached the ad.
     * @param loadId A unique identifier for this load request.
     * @param winningBidInfo Map of winning bid information such as price.
     * @param error null if the cache was successful, an error if it failed to cache.
     * @param bannerSize the size of banner in dp as provided by the partner. The requested size
     *                   will be used as a fallback when a size is not available from the partner.
     */
    fun onAdCached(
        placementName: String,
        loadId: String,
        winningBidInfo: Map<String, String>,
        error: ChartboostMediationAdException?,
        bannerSize: Size
    ) {
        onAdCached(
            placementName = placementName,
            loadId = loadId,
            winningBidInfo = winningBidInfo,
            error = error,
        )
    }

    /**
     * Called when the ad executes its clickthrough. This may happen multiple times for the same ad.
     *
     * @param placementName Indicates which placement was clicked.
     */
    fun onAdClicked(placementName: String)

    /**
     * Called when an ad impression occurs. This signal is when Helium fires an impression and
     * is independent of any partner impression.
     *
     * @param placementName Indicates which placement recorded an impression.
     */
    fun onAdImpressionRecorded(placementName: String)

    /**
     * Called when an ad View is added to this HeliumBannerAd.
     *
     * @param placementName Indicates which placement had a View added.
     * @param child View being added
     */
    fun onAdViewAdded(
        placementName: String,
        child: View?,
    ) { }
}
