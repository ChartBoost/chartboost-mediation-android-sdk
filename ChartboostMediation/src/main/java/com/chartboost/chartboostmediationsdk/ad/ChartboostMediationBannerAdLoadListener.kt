/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

/**
 * Banner ad load listener for Java callers.
 */
interface ChartboostMediationBannerAdLoadListener {
    fun onAdLoaded(result: ChartboostMediationBannerAdLoadResult)
}
