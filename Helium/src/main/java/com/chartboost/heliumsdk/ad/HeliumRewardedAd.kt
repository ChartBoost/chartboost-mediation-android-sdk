/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context
import com.chartboost.heliumsdk.ad.HeliumRewardedAd.Constants.MAX_CUSTOM_DATA_LENGTH
import com.chartboost.heliumsdk.domain.Ad
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.utils.LogController.w

/**
 * The Helium Rewarded Ad.
 *
 * @param context The current [Context].
 * @param placementName The placement name of the ad.
 * @property heliumFullscreenAdListener The listener to notify of Helium rewarded ad events.
 */
@Deprecated(
    "Use HeliumSdk.loadFullscreenAd(Context, ChartboostMediationAdLoadRequest, ChartboostMediationFullscreenAdListener) and ChartboostMediationFullscreenAd.show(Context) for the most comprehensive fullscreen ad experience.",
)
class HeliumRewardedAd(
    context: Context,
    placementName: String,
    var heliumFullscreenAdListener: HeliumFullscreenAdListener?,
) : HeliumFullscreenAd(context, placementName) {
    /**
     * @suppress
     */
    object Constants {
        /**
         * The maximum number of characters allowed for custom data.
         */
        const val MAX_CUSTOM_DATA_LENGTH = 1000
    }

    init {
        listener =
            object : HeliumFullscreenAdListener {
                override fun onAdCached(
                    placementName: String,
                    loadId: String,
                    winningBidInfo: Map<String, String>,
                    error: ChartboostMediationAdException?,
                ) {
                    heliumFullscreenAdListener?.onAdCached(placementName, loadId, winningBidInfo, error)
                    cachedAd?.customData = customData ?: ""
                }

                override fun onAdShown(
                    placementName: String,
                    error: ChartboostMediationAdException?,
                ) {
                    heliumFullscreenAdListener?.onAdShown(placementName, error)
                }

                override fun onAdClicked(placementName: String) {
                    heliumFullscreenAdListener?.onAdClicked(placementName)
                }

                override fun onAdClosed(
                    placementName: String,
                    error: ChartboostMediationAdException?,
                ) {
                    heliumFullscreenAdListener?.onAdClosed(placementName, error)
                }

                override fun onAdRewarded(placementName: String) {
                    heliumFullscreenAdListener?.onAdRewarded(placementName)
                }

                override fun onAdImpressionRecorded(placementName: String) {
                    heliumFullscreenAdListener?.onAdImpressionRecorded(placementName)
                }
            }
    }

    /**
     * Set an arbitrary String to send to the rewarded callback for the %%CUSTOM_DATA%% macro.
     * This is recommended to be a base64 String.
     */
    var customData: String? = null
        set(customData) {
            field =
                customData?.let {
                    if (it.length > MAX_CUSTOM_DATA_LENGTH) {
                        w("Failed to set custom data. It is longer than the maximum limit of $MAX_CUSTOM_DATA_LENGTH characters.")
                    }

                    it.takeIf { it.length <= MAX_CUSTOM_DATA_LENGTH }.also { maxCharCustomData ->
                        cachedAd?.customData = maxCharCustomData ?: ""
                    }
                }
        }

    /**
     * @suppress
     *
     * Get the [Ad.AdType] for this ad.
     */
    override fun getAdType() = Ad.AdType.REWARDED

    /**
     * Destroys the ad and does the necessary clean up and clears listeners.
     */
    override fun destroy() {
        super.destroy()
        heliumFullscreenAdListener = null
    }
}
