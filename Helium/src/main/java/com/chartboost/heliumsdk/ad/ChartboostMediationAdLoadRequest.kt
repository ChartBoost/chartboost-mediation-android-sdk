/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import com.chartboost.heliumsdk.domain.Keywords

/**
 * The Chartboost Mediation ad load request.
 *
 * @property placementName The placement name for the ad.
 * @property keywords The keywords targeted for the ad.
 */
class ChartboostMediationAdLoadRequest(
    val placementName: String,
    var keywords: Keywords,
)
