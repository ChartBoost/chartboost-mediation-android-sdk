/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import com.chartboost.heliumsdk.ad.HeliumBannerAd

/**
 * @suppress
 */
data class AdLoadParams(
    val adIdentifier: AdIdentifier,
    val keywords: Keywords,
    val loadId: String,
    val bannerSize: HeliumBannerAd.HeliumBannerSize?,
    val adInteractionListener: AdInteractionListener
)
