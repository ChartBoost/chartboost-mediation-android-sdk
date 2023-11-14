/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

/**
 * @suppress
 *
 * The ad identifier containing the ad type and placement name for the ad.
 *
 * @property adType The ad type for the ad.
 * @property placementName The placement name for the ad.
 */
class AdIdentifier(@field:Ad.AdType @param:Ad.AdType val adType: Int, val placementName: String) {
    override fun hashCode(): Int {
        return "$adType:$placementName".hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val adIdentifier = other as AdIdentifier
        return adIdentifier.adType == adType && adIdentifier.placementName == placementName
    }

    val placementType: String
        get() = when (adType) {
            Ad.AdType.INTERSTITIAL -> "interstitial"
            Ad.AdType.REWARDED -> "rewarded"
            Ad.AdType.BANNER -> "banner"
            Ad.AdType.ADAPTIVE_BANNER -> "adaptive_banner"
            Ad.AdType.REWARDED_INTERSTITIAL -> "rewarded_interstitial"
            else -> "unknown"
        }

    override fun toString(): String {
        return when (adType) {
            Ad.AdType.REWARDED_INTERSTITIAL -> "$placementName (Rewarded Interstitial)"
            Ad.AdType.INTERSTITIAL -> "$placementName (Interstitial)"
            Ad.AdType.REWARDED -> "$placementName (Rewarded)"
            Ad.AdType.BANNER -> "$placementName (Banner)"
            Ad.AdType.ADAPTIVE_BANNER -> "$placementName (Adaptive Banner)"
            else -> ""
        }
    }
}
