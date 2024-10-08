/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

/**
 * Fullscreen ad load listener for Java callers.
 */
interface ChartboostMediationFullscreenAdLoadListener {
    fun onAdLoaded(result: ChartboostMediationFullscreenAdLoadResult)
}
