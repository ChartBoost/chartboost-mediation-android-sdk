package com.chartboost.sdk.internal

import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.callbacks.BannerCallback
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
internal class BannerApi constructor(
    private val adUnitLoader: AdUnitLoader,
    private val adUnitRenderer: AdUnitRenderer,
    private val uiPoster: UiPoster,
    private val sdkConfig: AtomicReference<SdkConfiguration>,
    backgroundExecutor: ScheduledExecutorService,
    adApiCallbackSender: AdApiCallbackSender,
    session: Session,
    base64Wrapper: Base64Wrapper,
    eventTracker: EventTrackerExtensions,
    androidVersion: () -> Int = { Build.VERSION.SDK_INT },
) : AdApi(
        adUnitLoader,
        adUnitRenderer,
        sdkConfig,
        backgroundExecutor,
        adApiCallbackSender,
        session,
        base64Wrapper,
        eventTracker,
        androidVersion,
    ) {
    fun cache(
        ad: Banner,
        callback: BannerCallback,
    ) {
        cache(ad, callback, null)
    }

    fun cache(
        ad: Banner,
        callback: BannerCallback,
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
                AdType.Banner,
                ad.location,
            )
            return
        }

        if (!isBannerEnabled()) {
            uiPoster {
                callback.onAdLoaded(
                    CacheEvent(null, ad),
                    CacheError(CacheError.Code.BANNER_DISABLED),
                )
            }
            return
        }

        cacheAdUnit(ad.location, ad, callback, bidResponse)
    }

    fun show(
        ad: Banner,
        callback: BannerCallback,
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
                AdType.Banner,
                ad.location,
            )
            return
        }

        if (!isBannerEnabled()) {
            uiPoster {
                callback.onAdShown(
                    ShowEvent(null, ad),
                    ShowError(ShowError.Code.BANNER_DISABLED),
                )
            }
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

    fun detach() {
        adUnitRenderer.detachBannerImpression()
        adUnitLoader.removeAppRequest()
    }

    override fun onImpressionDismissed(impressionId: String?) {
        // Ignore - banner doesn't dismiss
    }

    fun fillSize(banner: Banner) {
        if (banner.layoutParams == null) {
            banner.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
        }

        val metrics = banner.resources.displayMetrics
        banner.layoutParams.width = pxToDp(banner.getBannerWidth(), metrics).toInt()
        banner.layoutParams.height = pxToDp(banner.getBannerHeight(), metrics).toInt()
    }

    private fun isBannerEnabled(): Boolean {
        return sdkConfig.get()?.bannerConfig?.isBannerEnabled ?: true
    }

    private fun pxToDp(
        px: Int,
        display: DisplayMetrics,
    ): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), display)
    }
}
