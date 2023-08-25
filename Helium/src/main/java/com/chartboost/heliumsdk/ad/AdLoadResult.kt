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
 * @suppress
 *
 * The Chartboost Mediation ad load result.
 *
 * @property loadId The identifier for this load call.
 * @property metrics Metrics data as JSON for the ad load event.
 * @property error The error that occurred during the ad load event, if any.
 */
open class AdLoadResult(
    val loadId: String,
    val metrics: JSONObject,
    var error: ChartboostMediationError?
)
