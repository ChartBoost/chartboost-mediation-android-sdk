/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.util.Size
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.asSize

object PartnerAdUtils {
    fun getCreativeSizeFromPartnerAdDetails(partnerAd: PartnerAd): Size =
        partnerAd.partnerBannerSize?.size
            ?: partnerAd.request.bannerSize?.asSize()
            ?: ChartboostMediationBannerAdView.ChartboostMediationBannerSize.STANDARD.asSize()
}
