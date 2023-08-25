/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context
import com.chartboost.heliumsdk.domain.Ad
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.utils.LogController

/**
 * The Helium Interstitial Ad.
 *
 * @param context The current [Context].
 * @param placementName The placement name of the ad.
 * @property heliumFullscreenAdListener The listener to notify of Helium interstitial ad events.
 */
 @Deprecated("Use HeliumSdk.loadFullscreenAd(Context, ChartboostMediationAdLoadRequest, ChartboostMediationFullscreenAdListener) and ChartboostMediationFullscreenAd.show(Context) for the most comprehensive fullscreen ad experience.")
class HeliumInterstitialAd(
    context: Context,
    placementName: String,
    var heliumFullscreenAdListener: HeliumFullscreenAdListener?
) : HeliumFullscreenAd(context, placementName) {

    init {
        listener = object : HeliumFullscreenAdListener {
            override fun onAdCached(
                placementName: String,
                loadId: String,
                winningBidInfo: Map<String, String>,
                error: ChartboostMediationAdException?
            ) {
                heliumFullscreenAdListener?.onAdCached(placementName, loadId, winningBidInfo, error)
            }

            override fun onAdShown(placementName: String, error: ChartboostMediationAdException?) {
                heliumFullscreenAdListener?.onAdShown(placementName, error)
            }

            override fun onAdClicked(placementName: String) {
                heliumFullscreenAdListener?.onAdClicked(placementName)
            }

            override fun onAdClosed(placementName: String, error: ChartboostMediationAdException?) {
                heliumFullscreenAdListener?.onAdClosed(placementName, error)
            }

            override fun onAdRewarded(placementName: String) {
                LogController.w("$placementName Interstitial received rewarded event?")
            }

            override fun onAdImpressionRecorded(placementName: String) {
                heliumFullscreenAdListener?.onAdImpressionRecorded(placementName)
            }
        }
    }

    /**
     * @suppress
     *
     * Get the [Ad.AdType] for this ad.
     */
    override fun getAdType() = Ad.AdType.INTERSTITIAL

    /**
     * Destroys the ad and does the necessary clean up and clears listeners.
     */
    override fun destroy() {
        super.destroy()
        heliumFullscreenAdListener = null
    }
}
