/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

interface PartnerAdListener {
    fun onPartnerAdImpression(partnerAd: PartnerAd)
    fun onPartnerAdClicked(partnerAd: PartnerAd)
    fun onPartnerAdRewarded(partnerAd: PartnerAd)
    fun onPartnerAdDismissed(partnerAd: PartnerAd, error: ChartboostMediationAdException?)
    fun onPartnerAdExpired(partnerAd: PartnerAd)
}
