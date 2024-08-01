/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import com.chartboost.chartboostmediationsdk.domain.Keywords

/**
 * The Chartboost Mediation banner ad load request.
 *
 * @property placement The Chartboost Mediation placement for the ad.
 * @property keywords The keywords targeted for the ad.
 * @property size The desired size for the banner ad.
 */
class ChartboostMediationBannerAdLoadRequest(
    placement: String,
    val keywords: Keywords,
    val size: ChartboostMediationBannerAdView.ChartboostMediationBannerSize,
) : ChartboostMediationAdLoadRequest(placement)
