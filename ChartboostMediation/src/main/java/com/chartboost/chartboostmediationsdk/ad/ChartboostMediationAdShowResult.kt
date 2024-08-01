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
 * The Chartboost Mediation fullscreen ad show result.
 *
 * @property metrics Metrics data as JSON for the ad show event.
 * @property error The error that occurred during the ad show event, if any.
 */
class ChartboostMediationAdShowResult(
    val metrics: JSONObject,
    var error: ChartboostMediationError?,
)
