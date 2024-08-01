/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView

data class PartnerAdLoadRequest(
    /**
     * The server-keyed identifier for a particular partner.
     */
    val partnerId: String,
    /**
     * The Chartboost Mediation placement associated with this ad.
     */
    val mediationPlacement: String,
    /**
     * The partner placement or ad unit id or zone id or equivalent associated with this ad.
     */
    val partnerPlacement: String,
    /**
     * Size in density-independent pixels of the banner ad and the banner type. Can be null if not a banner.
     */
    val bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
    /**
     * The partner ad format.
     */
    val format: PartnerAdFormat,
    /**
     * Ad markup for bidding ads.
     */
    val adm: String?,
    /**
     *  Unique request identifier for this load request.
     */
    val identifier: String,
    /**
     * Publisher-supplied and server-provided parameters settings to be passed to the active adapter.
     */
    val partnerSettings: Map<String, Any>,
    /**
     * Forwards events from the partner ad to this listener after an ad is loaded.
     */
    val adInteractionListener: AdInteractionListener,
    /**
     * The keywords to be passed to the partner.
     */
    val keywords: Map<String, String>,
)
