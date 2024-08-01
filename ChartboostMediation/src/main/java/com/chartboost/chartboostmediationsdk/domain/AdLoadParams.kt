/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView

/**
 * @suppress
 */
data class AdLoadParams(
    val adIdentifier: AdIdentifier,
    val keywords: Keywords,
    val loadId: String,
    val queueId: String? = null,
    val bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
    val adInteractionListener: AdInteractionListener,
    val partnerSettings: Map<String, Any>,
)
