/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import com.chartboost.chartboostmediationsdk.domain.Keywords

/**
 * The Chartboost Mediation fullscreen ad load request.
 *
 * @property placement The Chartboost Mediation placement for the ad.
 * @property keywords The keywords targeted for the ad.
 * @property partnerSettings An optional map of String to Any that a publisher would like to send to all partners.
 */
class ChartboostMediationFullscreenAdLoadRequest(
    placement: String,
    val keywords: Keywords,
    val partnerSettings: Map<String, Any> = mapOf(),
) : ChartboostMediationAdLoadRequest(placement)
