/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */
package com.chartboost.heliumsdk.domain

import androidx.annotation.IntDef
import com.chartboost.heliumsdk.ad.HeliumAd

/**
 * @suppress
 */
class Ad(ad: HeliumAd) {
    val adIdentifier: AdIdentifier
    var heliumAd: HeliumAd
    var loadId: String? = null
    var bids: Bids? = null

    @State
    var state: Int

    @Deprecated("Use the AdFormat enumeration instead")
    @IntDef(
        AdType.UNKNOWN,
        AdType.INTERSTITIAL,
        AdType.REWARDED,
        AdType.BANNER,
        AdType.REWARDED_INTERSTITIAL,
        AdType.ADAPTIVE_BANNER
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class AdType {
        companion object {
            const val UNKNOWN = -1
            const val INTERSTITIAL = 0
            const val REWARDED = 1
            const val BANNER = 2
            const val REWARDED_INTERSTITIAL = 3
            const val ADAPTIVE_BANNER = 4
        }
    }

    @IntDef(
        State.NEW,
        State.BIDDING,
        State.LOADING,
        State.LOADED,
        State.SHOW_REQUESTED,
        State.SHOWING,
        State.FAILED
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class State {
        companion object {
            const val NEW = 0
            const val BIDDING = 1
            const val LOADING = 2
            const val LOADED = 3
            const val SHOW_REQUESTED = 4
            const val SHOWING = 5
            const val FAILED = 6
        }
    }

    init {
        adIdentifier = AdIdentifier(ad.getAdType(), ad.placementName)
        heliumAd = ad
        state = State.NEW
    }
}
