package com.chartboost.sdk.internal

import android.os.Build
import com.chartboost.sdk.ads.Rewarded
import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoader
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRenderer
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.api.AdApi
import com.chartboost.sdk.internal.api.AdApiCallbackSender
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.Session
import com.chartboost.sdk.tracking.TrackingEventName
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

/**
 * This is obfuscated and internal and calls to the actual code
 * Implementation of the Rewarded object
 */
internal class RewardedApi(
    adUnitLoader: AdUnitLoader,
    adUnitRenderer: AdUnitRenderer,
    private val uiPoster: UiPoster,
    sdkConfig: AtomicReference<SdkConfiguration>,
    backgroundExecutorService: ScheduledExecutorService,
    adApiCallbackSender: AdApiCallbackSender,
    session: Session,
    base64Wrapper: Base64Wrapper,
    eventTracker: EventTrackerExtensions,
    androidVersion: () -> Int = { Build.VERSION.SDK_INT },
) : AdApi(
        adUnitLoader,
        adUnitRenderer,
        sdkConfig,
        backgroundExecutorService,
        adApiCallbackSender,
        session,
        base64Wrapper,
        eventTracker,
        androidVersion,
    ) {
    fun cache(
        ad: Rewarded,
        callback: RewardedCallback,
    ) {
        cache(ad, callback, null)
    }

    fun cache(
        ad: Rewarded,
        callback: RewardedCallback,
        bidResponse: String?,
    ) {
        if (shouldStopCache(ad.location)) {
            uiPoster {
                callback.onAdLoaded(
                    CacheEvent(null, ad),
                    CacheError(CacheError.Code.SESSION_NOT_STARTED),
                )
            }
            trackEventWithAdTypeAndLocation(
                TrackingEventName.Cache.FINISH_FAILURE,
                "Invalid configuration. Check logs for more details.",
                AdType.Rewarded,
                ad.location,
            )
            return
        }
        cacheAdUnit(ad.location, ad, callback, bidResponse)
    }

    fun show(
        ad: Rewarded,
        callback: RewardedCallback,
    ) {
        if (shouldStopCache(ad.location)) {
            uiPoster {
                callback.onAdShown(
                    ShowEvent(null, ad),
                    ShowError(ShowError.Code.SESSION_NOT_STARTED),
                )
            }
            trackEventWithAdTypeAndLocation(
                TrackingEventName.Show.FINISH_FAILURE,
                "Invalid configuration. Check logs for more details.",
                AdType.Rewarded,
                ad.location,
            )
            return
        }

        if (!isCached()) {
            uiPoster {
                callback.onAdShown(
                    ShowEvent(null, ad),
                    ShowError(ShowError.Code.NO_CACHED_AD),
                )
            }
            return
        }

        showAdUnit(ad, callback)
    }
}
