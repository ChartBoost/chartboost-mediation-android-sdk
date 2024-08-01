/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAdException

/**
 * The Chartboost Mediation Fullscreen Ad Queue Ad Expired Listener extends from the
 * [ChartboostMediationFullscreenAdListener] to make use of the onAdExpired callback.
 *
 * While the onAdExpired callback is not implemented here, this approach is intentional as this will force
 * the onAdExpired callback to be implemented where it will be used. In our case, in the
 * [ChartboostMediationFullscreenAdQueue] class, in which we make use of it for expiration.
 *
 * @suppress
 */
internal interface ChartboostMediationFullscreenAdQueueAdExpiredListener : ChartboostMediationFullscreenAdListener {
    override fun onAdClicked(ad: ChartboostMediationFullscreenAd) {}

    override fun onAdClosed(
        ad: ChartboostMediationFullscreenAd,
        error: ChartboostMediationAdException?,
    ) {}

    override fun onAdImpressionRecorded(ad: ChartboostMediationFullscreenAd) {}

    override fun onAdRewarded(ad: ChartboostMediationFullscreenAd) {}
}
