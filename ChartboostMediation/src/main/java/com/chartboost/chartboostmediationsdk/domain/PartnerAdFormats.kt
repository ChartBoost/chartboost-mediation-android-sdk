/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * Known partner ad formats.
 */
object PartnerAdFormats {
    val INTERSTITIAL = AdFormat.INTERSTITIAL.key
    val REWARDED = AdFormat.REWARDED.key
    val BANNER = AdFormat.BANNER.key
    val REWARDED_INTERSTITIAL = AdFormat.REWARDED_INTERSTITIAL.key
}

typealias PartnerAdFormat = String
