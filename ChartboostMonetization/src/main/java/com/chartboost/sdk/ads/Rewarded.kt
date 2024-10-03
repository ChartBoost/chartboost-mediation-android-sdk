package com.chartboost.sdk.ads

import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.RewardedApi
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer.androidComponent
import com.chartboost.sdk.internal.di.createRewardedApi
import com.chartboost.sdk.internal.logging.Logger

/**
 * Rewarded is a full-screen ad that provides a reward to the user.
 * To show a rewarded ad, it first needs to be cached. Trying to show an uncached rewarded ad will
 * always fail, so it is recommended to always check if the ad is cached first.
 * You can create and cache as many rewarded ads as you want,
 * but only one can be presented at a time.
 *
 * A basic implementation would look like this:
 * ```
 * fun createAndCacheRewarded() {
 *     this.rewarded = Rewarded("location", callback)
 *     this.rewarded.cache()
 * }
 *
 * fun showRewarded() {
 *     if (this.rewarded.isCached()) {
 *         this.rewarded.show()
 *     }
 * }
 *
 * // Callback implementation
 *
 * fun didCacheAd(event: CacheEvent, error: CacheError?) {
 *     if (error != null) {
 *         // Handle error, possibly scheduling a retry
 *     }
 * }
 *
 * fun willShowAd(event: ShowEvent) {
 *     // Pause ongoing processes
 * }
 *
 * fun didShowAd(event: ShowEvent, error: ShowError?) {
 *     if (error != null) {
 *         // Resume paused processes
 *     }
 * }
 *
 * fun didDismissAd(event: DismissEvent) {
 *     // Resume paused processes
 * }
 *
 * fun didEarnReward(event: RewardEvent) {
 *     // Update app state with event.reward
 * }
 * ```
 *
 * For more information on integrating and using the Chartboost SDK, please visit our help site
 * documentation at [Chartboost Help](https://help.chartboost.com).
 */
class Rewarded(
    override val location: String,
    private val callback: RewardedCallback,
    private val mediation: Mediation? = null,
) : Ad {
    private val api: RewardedApi by lazy { createRewardedApi(mediation) }

    override fun cache() {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(true)
            return
        }
        api.cache(this, callback)
    }

    override fun cache(bidResponse: String?) {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(true)
            return
        }

        if (bidResponse.isNullOrEmpty()) {
            api.onAdFailToLoad("", CBError.Impression.INVALID_RESPONSE)
        } else {
            api.cache(this, callback, bidResponse)
        }
    }

    override fun show() {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(false)
            return
        }
        api.show(this, callback)
    }

    override fun clearCache() {
        if (Chartboost.isSdkStarted()) {
            api.clearCache()
        }
    }

    override fun isCached(): Boolean {
        return if (Chartboost.isSdkStarted()) {
            api.isCached()
        } else {
            false
        }
    }

    private fun postSessionNotStartedInMainThread(isCache: Boolean) {
        try {
            androidComponent.uiPoster {
                if (isCache) {
                    callback.onAdLoaded(
                        CacheEvent(null, this),
                        CacheError(CacheError.Code.SESSION_NOT_STARTED),
                    )
                } else {
                    callback.onAdShown(
                        ShowEvent(null, this),
                        ShowError(ShowError.Code.SESSION_NOT_STARTED),
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("Rewarded ad cannot post session not started callback $e")
        }
    }
}
