/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

enum class AdFormat(val key: String) {
    INTERSTITIAL("interstitial"),
    REWARDED("rewarded"),
    BANNER("banner"),
    REWARDED_INTERSTITIAL("rewarded_interstitial"),
    UNKNOWN("unknown");

    /**
     * @suppress
     */
    companion object {
        fun fromString(value: String?): AdFormat {
            return values().find { it.key.equals(value, true) } ?: UNKNOWN
        }

        fun toAdType(value: AdFormat?): Int {
            return when (value) {
                INTERSTITIAL -> Ad.AdType.INTERSTITIAL
                REWARDED -> Ad.AdType.REWARDED
                BANNER -> Ad.AdType.BANNER
                REWARDED_INTERSTITIAL -> Ad.AdType.REWARDED_INTERSTITIAL
                else -> Ad.AdType.UNKNOWN
            }
        }

        fun fromAdType(value: Int?): AdFormat {
            return when (value) {
                Ad.AdType.INTERSTITIAL -> INTERSTITIAL
                Ad.AdType.REWARDED -> REWARDED
                Ad.AdType.BANNER -> BANNER
                Ad.AdType.REWARDED_INTERSTITIAL -> REWARDED_INTERSTITIAL
                else -> UNKNOWN
            }
        }
    }
}
