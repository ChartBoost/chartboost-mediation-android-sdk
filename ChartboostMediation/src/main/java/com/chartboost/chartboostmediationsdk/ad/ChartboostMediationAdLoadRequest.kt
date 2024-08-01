/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

/**
 * The Chartboost Mediation base ad load request.
 *
 * @property placement The Chartboost Mediation placement for the ad.
 */
open class ChartboostMediationAdLoadRequest(
    val placement: String,
)
