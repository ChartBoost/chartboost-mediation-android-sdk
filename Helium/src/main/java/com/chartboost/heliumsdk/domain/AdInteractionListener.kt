/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

/**
 * @suppress
 */
interface AdInteractionListener {
    fun onImpressionTracked(partnerAd: PartnerAd)

    fun onClicked(partnerAd: PartnerAd)

    fun onRewarded(partnerAd: PartnerAd)

    fun onDismissed(
        partnerAd: PartnerAd,
        error: ChartboostMediationAdException?,
    )

    fun onExpired(partnerAd: PartnerAd)
}
