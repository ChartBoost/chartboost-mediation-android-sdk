/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.util.Size
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import org.json.JSONObject

/**
 * The Chartboost Mediation banner ad load result.
 *
 * @param loadId The identifier for this load call.
 * @param metrics Metrics data as JSON for the ad load event.
 * @param winningBidInfo A [Map] of information regarding the winning bid.
 * @param error The error that occurred during the ad load event, if any.
 * @property placement Indicates which placement cached the ad.
 * @property bannerSize The [Size] for the loaded banner.
 */
class ChartboostMediationBannerAdLoadResult(
    loadId: String,
    metrics: JSONObject,
    winningBidInfo: Map<String, String>,
    error: ChartboostMediationError?,
    val placement: String,
    val bannerSize: Size,
) : AdLoadResult(loadId, metrics, error, winningBidInfo)
