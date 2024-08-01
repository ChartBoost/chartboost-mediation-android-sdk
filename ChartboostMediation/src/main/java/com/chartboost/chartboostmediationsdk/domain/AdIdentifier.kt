/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * @suppress
 *
 * The ad identifier containing the ad type and placement name for the ad.
 *
 * @property adType The ad type for the ad.
 * @property placement The Chartboost Mediation placement for the ad.
 */
class AdIdentifier(
    @field:Ad.AdType @param:Ad.AdType val adType: Int,
    val placement: String,
) {
    override fun hashCode(): Int = "$adType:$placement".hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val adIdentifier = other as AdIdentifier
        return adIdentifier.adType == adType && adIdentifier.placement == placement
    }

    val placementType: String
        get() =
            when (adType) {
                Ad.AdType.INTERSTITIAL -> "interstitial"
                Ad.AdType.REWARDED -> "rewarded"
                Ad.AdType.BANNER -> "banner"
                Ad.AdType.ADAPTIVE_BANNER -> "adaptive_banner"
                Ad.AdType.REWARDED_INTERSTITIAL -> "rewarded_interstitial"
                else -> "unknown"
            }

    override fun toString(): String =
        when (adType) {
            Ad.AdType.REWARDED_INTERSTITIAL -> "$placement (Rewarded Interstitial)"
            Ad.AdType.INTERSTITIAL -> "$placement (Interstitial)"
            Ad.AdType.REWARDED -> "$placement (Rewarded)"
            Ad.AdType.BANNER -> "$placement (Banner)"
            Ad.AdType.ADAPTIVE_BANNER -> "$placement (Adaptive Banner)"
            else -> ""
        }
}
