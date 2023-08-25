/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import com.chartboost.heliumsdk.domain.ChartboostMediationError
import org.json.JSONObject

/**
 * The Chartboost Mediation fullscreen ad show result.
 *
 * @property metrics Metrics data as JSON for the ad show event.
 * @property error The error that occurred during the ad show event, if any.
 */
class ChartboostMediationAdShowResult(
    val metrics: JSONObject,
    var error: ChartboostMediationError?
)
