/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import com.chartboost.heliumsdk.domain.ChartboostMediationAdException

/**
 * Listener for Helium fullscreen ad events.
 */
@Deprecated("Use ChartboostMediationFullscreenAdListener for the most comprehensive fullscreen ad experience.")
interface HeliumFullscreenAdListener {
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
     * Called when an ad is shown.
     *
     * @param placementName Indicates which placement was shown.
     * @param error null if the show was successful, an error if the show failed.
     */
    fun onAdShown(
        placementName: String,
        error: ChartboostMediationAdException?,
    )

    /**
     * Called when the ad executes its clickthrough. This may happen multiple times for the same ad.
     *
     * @param placementName Indicates which placement was clicked.
     */
    fun onAdClicked(placementName: String)

    /**
     * Called when the ad is closed.
     *
     * @param placementName Indicates which placement was closed.
     * @param error If there was an error in the lifecycle of the ad, it will be presented here.
     */
    fun onAdClosed(
        placementName: String,
        error: ChartboostMediationAdException?,
    )

    /**
     * Called when the user should receive the reward associated with this rewarded ad.
     *
     * @param placementName Indicates which placement the reward originated from.
     */
    fun onAdRewarded(placementName: String)

    /**
     * Called when an ad impression occurs. This signal is when Helium fires an impression and
     * is independent of any partner impression.
     *
     * @param placementName Indicates which placement recorded an impression.
     */
    fun onAdImpressionRecorded(placementName: String)
}
