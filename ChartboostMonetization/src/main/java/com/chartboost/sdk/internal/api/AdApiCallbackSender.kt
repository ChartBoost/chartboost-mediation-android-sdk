package com.chartboost.sdk.internal.api

import com.chartboost.sdk.ads.Ad
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.ads.Interstitial
import com.chartboost.sdk.ads.Rewarded
import com.chartboost.sdk.callbacks.AdCallback
import com.chartboost.sdk.callbacks.DismissibleAdCallback
import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.DismissEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.RewardEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger

internal class AdApiCallbackSender(private val uiPoster: UiPoster) {
    fun sendShowCallbackInMainThread(
        adId: String?,
        error: ShowError?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            ad?.let {
                callback?.onAdShown(ShowEvent(adId, it), error) ?: Logger.i(
                    "Callback missing for ${getAdTypeName(it)} on onAdShown",
                )
            } ?: Logger.e("Ad is missing on onAdShown")
        }
    }

    fun sendRewardCallbackOnMainThread(
        adId: String?,
        ad: Ad?,
        callback: AdCallback?,
        reward: Int,
    ) {
        uiPoster {
            callback?.let { adCallback ->
                if (adCallback is RewardedCallback) {
                    ad?.let {
                        adCallback.onRewardEarned(RewardEvent(adId, it, reward))
                    } ?: Logger.e("Ad is missing on didEarnReward")
                } else {
                    Logger.e("Invalid ad type to send a reward")
                }
            } ?: Logger.e("Missing callback on sendRewardCallbackOnMainThread")
        }
    }

    fun sendDismissCallbackOnMainThread(
        adId: String?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            callback?.let { adCallback ->
                if (adCallback is DismissibleAdCallback) {
                    ad?.let {
                        adCallback.onAdDismiss(DismissEvent(adId, it))
                    } ?: Logger.e("Ad is missing on onAdDismiss")
                } else {
                    Logger.e("Invalid ad type to send onAdDismiss")
                }
            } ?: Logger.e("Missing callback on sendDismissCallbackOnMainThread")
        }
    }

    fun sendImpressionRecordedCallbackOnMainThread(
        adId: String?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            ad?.let {
                callback?.onImpressionRecorded(ImpressionEvent(adId, it)) ?: Logger.i(
                    "Callback missing for ${getAdTypeName(it)} on onImpressionRecorded",
                )
            } ?: Logger.e("Ad is missing on onImpressionRecorded")
        }
    }

    fun sendCacheCallbackInMainThread(
        adId: String?,
        error: CacheError?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            ad?.let {
                callback?.onAdLoaded(CacheEvent(adId, it), error) ?: Logger.i(
                    "Callback missing for ${getAdTypeName(it)} on onAdLoaded",
                )
            } ?: Logger.e("Ad is missing on onAdLoaded")
        }
    }

    fun sendClickCallbackInMainThread(
        adId: String?,
        error: ClickError?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            ad?.let {
                callback?.onAdClicked(ClickEvent(adId, it), error) ?: Logger.i(
                    "Callback missing for ${getAdTypeName(it)} on onAdClicked",
                )
            } ?: Logger.e("Ad is missing on onAdClicked")
        }
    }

    fun sendOnAdRequestedToShowInMainThread(
        adId: String?,
        ad: Ad?,
        callback: AdCallback?,
    ) {
        uiPoster {
            ad?.let {
                callback?.onAdRequestedToShow(ShowEvent(adId, it)) ?: Logger.i(
                    "Callback missing for ${getAdTypeName(it)} on onAdRequestedToShow",
                )
            } ?: Logger.e("Ad is missing on onAdRequestedToShow")
        }
    }

    private fun getAdTypeName(ad: Ad): String {
        return when (ad) {
            is Interstitial -> AdType.Interstitial.name
            is Rewarded -> AdType.Rewarded.name
            is Banner -> AdType.Banner.name
        }
    }
}
