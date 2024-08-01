/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import org.json.JSONObject

/**
 * The Chartboost Mediation fullscreen ad load result.
 *
 * @property ad The [ChartboostMediationFullscreenAd] that was loaded, if any.
 * @property loadId The identifier for this load call.
 * @property metrics Metrics data as JSON for the ad load event.
 * @property error The error that occurred during the ad load event, if any.
 */
class ChartboostMediationFullscreenAdLoadResult(
    val ad: ChartboostMediationFullscreenAd?,
    loadId: String,
    metrics: JSONObject,
    error: ChartboostMediationError?,
    winningBidInfo: Map<String, String> = mapOf(),
) : AdLoadResult(loadId, metrics, error, winningBidInfo)
