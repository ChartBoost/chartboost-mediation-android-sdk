/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView

data class PartnerAdPreBidRequest(
    val mediationPlacement: String,
    val format: PartnerAdFormat,
    val loadId: String,
    val bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
    val keywords: Map<String, String>,
    val partnerSettings: Map<String, Any>,
)
