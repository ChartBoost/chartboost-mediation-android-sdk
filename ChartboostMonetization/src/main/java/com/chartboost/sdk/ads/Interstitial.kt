package com.chartboost.sdk.ads

import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.InterstitialCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.InterstitialApi
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer.androidComponent
import com.chartboost.sdk.internal.di.createInterstitialApi
import com.chartboost.sdk.internal.logging.Logger

/**
 * Interstitial is a full-screen ad.
 * To show an interstitial, it first needs to be cached. Trying to show a not-cached interstitial will
 * always fail, so it is recommended to always check if the ad is cached first.
 * You can create and cache as many interstitial ads as you want,
 * but only one can be presented at a time.
 *
 * A basic implementation would look like this:
 * ```
 * fun createAndCacheInterstitial() {
 *     this.interstitial = Interstitial("location", callback)
 *     this.interstitial.cache()
 * }
 *
 * fun showInterstitial() {
 *     if (this.interstitial.isCached()) {
 *         this.interstitial.show()
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
 * ```
 *
 * For more information on integrating and using the Chartboost SDK, please visit our help site
 * documentation at https://help.chartboost.com.
 */
class Interstitial(
    override val location: String,
    private val callback: InterstitialCallback,
    private val mediation: Mediation? = null,
) : Ad {
    private val api: InterstitialApi by lazy { createInterstitialApi(mediation) }

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
            Logger.e("Interstitial ad cannot post session not started callback $e")
        }
    }
}
