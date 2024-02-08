/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import com.chartboost.heliumsdk.domain.ChartboostMediationAdException

interface ChartboostMediationFullscreenAdListener {
    /**
     * Called when the ad executes its clickthrough. This may happen multiple times for the same ad.
     *
     * @param ad The current ChartboostMediationFullscreenAd instance.
     */
    fun onAdClicked(ad: ChartboostMediationFullscreenAd)

    /**
     * Called when the ad is closed.
     *
     * @param ad The current ChartboostMediationFullscreenAd instance.
     * @param error If there was an error in the lifecycle of the ad, it will be presented here.
     */
    fun onAdClosed(
        ad: ChartboostMediationFullscreenAd,
        error: ChartboostMediationAdException?,
    )

    /**
     * Called when the user should receive the reward associated with this rewarded ad.
     *
     * @param ad The current ChartboostMediationFullscreenAd instance.
     */
    fun onAdRewarded(ad: ChartboostMediationFullscreenAd)

    /**
     * Called when an ad impression occurs. This signal is when Helium fires an impression and
     * is independent of any partner impression.
     *
     * @param ad The current ChartboostMediationFullscreenAd instance.
     */
    fun onAdImpressionRecorded(ad: ChartboostMediationFullscreenAd)

    /**
     * Called when the ad is expired by the partner SDK/adapter.
     *
     * @param ad The current ChartboostMediationFullscreenAd instance.
     */
    fun onAdExpired(ad: ChartboostMediationFullscreenAd)
}
