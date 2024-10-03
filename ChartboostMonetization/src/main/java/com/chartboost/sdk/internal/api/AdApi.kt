package com.chartboost.sdk.internal.api

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.ads.Ad
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.ads.Interstitial
import com.chartboost.sdk.ads.Rewarded
import com.chartboost.sdk.callbacks.AdCallback
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnitBannerData
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoader
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoaderAdCallback
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRenderer
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererAdCallback
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.i
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.InfoEvent
import com.chartboost.sdk.tracking.Session
import com.chartboost.sdk.tracking.TrackAd
import com.chartboost.sdk.tracking.TrackingEventName
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

@Suppress("LongParameterList")
internal abstract class AdApi(
    private val adUnitLoader: AdUnitLoader,
    private val adUnitRenderer: AdUnitRenderer,
    private val sdkConfig: AtomicReference<SdkConfiguration>,
    private val backgroundExecutorService: ScheduledExecutorService,
    private val adApiCallbackSender: AdApiCallbackSender,
    @get:VisibleForTesting private val session: Session,
    private val base64Wrapper: Base64Wrapper,
    eventTracker: EventTrackerExtensions,
    private val androidVersion: () -> Int,
) : AdUnitRendererAdCallback, AdUnitLoaderAdCallback, EventTrackerExtensions by eventTracker {
    protected var ad: Ad? = null
    protected var callback: AdCallback? = null

    protected fun cacheAdUnit(
        location: String,
        ad: Ad,
        callback: AdCallback,
        bidResponse: String?,
    ) {
        this.ad = ad
        this.callback = callback

        val decodedBidResponse =
            AdApiDecoderImpl.decodeBidResponse(bidResponse, base64Wrapper, ::onAdFailToLoad)

        decodedBidResponse.fold(
            onSuccess = {
                backgroundExecutorService.execute {
                    if (ad is Banner) {
                        adUnitLoader.load(
                            location,
                            this,
                            it,
                            AdUnitBannerData(
                                ad,
                                ad.getBannerWidth(),
                                ad.getBannerHeight(),
                            ),
                        )
                    } else {
                        adUnitLoader.load(location, this, it)
                    }
                }
            },
            onFailure = {
                // no distinguish by type error
                return
            },
        )
    }

    protected fun showAdUnit(
        ad: Ad,
        callback: AdCallback,
    ) {
        this.ad = ad
        this.callback = callback
        backgroundExecutorService.execute {
            adUnitLoader.getAppRequest()?.let {
                adUnitRenderer.render(it, this)
            } ?: Logger.e("Missing app request on render")
        }
    }

    fun clearCache() {
        if (isCached()) {
            adUnitLoader.removeAppRequest()
        }
    }

    fun isCached(): Boolean {
        return adUnitLoader.getAppRequest()?.adUnit != null
    }

    protected fun shouldStopCache(location: String): Boolean {
        // At this point we don't need to check if we are initialised as it is done before
        if (androidVersion() < Build.VERSION_CODES.LOLLIPOP) return true
        if (sdkConfig.get()?.getPublisherDisable() == true) {
            Logger.e(
                "Chartboost Integration Warning: your account has been disabled " +
                    "for this session. This app has no active publishing campaigns, " +
                    "please create a publishing campaign in the Chartboost dashboard and wait " +
                    "at least 30 minutes to re-enable. If you need assistance, please " +
                    "visit http://chartboo.st/publishing .",
            )
            return true
        }
        return location.isEmpty()
    }

    override fun onImpressionClicked(impressionId: String?) {
        adApiCallbackSender.sendClickCallbackInMainThread(
            impressionId,
            null,
            ad,
            callback,
        )
    }

    override fun onImpressionClickedFailed(
        impressionId: String?,
        url: String?,
        error: CBError.Click,
    ) {
        val message = "Click error: ${error.name} url: $url"
        trackEvent(TrackingEventName.Click.INVALID_URL_ERROR, message, impressionId)
        adApiCallbackSender.sendClickCallbackInMainThread(
            impressionId,
            parseCBImpressionClickErrorToClickError(error, message),
            ad,
            callback,
        )
    }

    override fun onShowSuccess(impressionId: String?) {
        trackEvent(TrackingEventName.Show.FINISH_SUCCESS, "", impressionId)
        updateSession()
        adApiCallbackSender.sendShowCallbackInMainThread(
            impressionId,
            null,
            ad,
            callback,
        )
    }

    override fun onShowFailure(
        impressionId: String?,
        error: CBError.Impression,
    ) {
        error.trackErrorEventByType(impressionId)
        adApiCallbackSender.sendShowCallbackInMainThread(
            impressionId,
            parseCBImpressionErrorToShowError(error),
            ad,
            callback,
        )
    }

    private fun CBError.Impression.trackErrorEventByType(impressionId: String?) {
        when (this) {
            CBError.Impression.ASSET_MISSING,
            CBError.Impression.ASSETS_DOWNLOAD_FAILURE,
            CBError.Impression.ASSET_PREFETCH_IN_PROGRESS,
            -> TrackingEventName.Show.UNAVAILABLE_ASSET_ERROR

            CBError.Impression.WEB_VIEW_CLIENT_RECEIVED_ERROR,
            CBError.Impression.WEB_VIEW_PAGE_LOAD_TIMEOUT,
            CBError.Impression.ERROR_LOADING_WEB_VIEW,
            -> TrackingEventName.Show.WEBVIEW_ERROR

            else -> TrackingEventName.Show.FINISH_FAILURE
        }.let { eventName ->
            trackEvent(eventName, name, impressionId)
        }
    }

    private fun String?.asTrackAd(): TrackAd =
        TrackAd(
            adImpressionId = this ?: "",
        )

    override fun onCacheSuccess(
        impressionId: String?,
        trackingEventName: TrackingEventName,
    ) {
        trackEvent(trackingEventName, "", impressionId)
        adApiCallbackSender.sendCacheCallbackInMainThread(
            impressionId,
            null,
            ad,
            callback,
        )
    }

    override fun onAdFailToLoad(
        impressionId: String?,
        error: CBError.Type,
    ) {
        trackEvent(TrackingEventName.Cache.FINISH_FAILURE, error.name, impressionId)
        adApiCallbackSender.sendCacheCallbackInMainThread(
            impressionId,
            parseCBImpressionErrorToCacheError(error),
            ad,
            callback,
        )
    }

    override fun onReward(
        impressionId: String?,
        reward: Int,
    ) {
        adApiCallbackSender.sendRewardCallbackOnMainThread(
            impressionId,
            ad,
            callback,
            reward,
        )
    }

    override fun onImpressionDismissed(impressionId: String?) {
        adApiCallbackSender.sendDismissCallbackOnMainThread(
            impressionId,
            ad,
            callback,
        )
    }

    override fun onRequestedToShow(impressionId: String?) {
        adApiCallbackSender.sendOnAdRequestedToShowInMainThread(
            impressionId,
            ad,
            callback,
        )
    }

    override fun onImpressionSuccess(impressionId: String?) {
        trackEvent(TrackingEventName.Misc.IMPRESSION_RECORDED, "", impressionId)
        adApiCallbackSender.sendImpressionRecordedCallbackOnMainThread(
            impressionId,
            ad,
            callback,
        )
    }

    protected fun trackEventWithAdTypeAndLocation(
        eventName: TrackingEventName,
        message: String,
        adType: AdType,
        location: String,
    ) {
        InfoEvent(eventName, message, adType.name, location, adUnitRenderer.mediation).track()
    }

    private fun Ad.toAdType(): AdType {
        return when (this) {
            is Interstitial -> AdType.Interstitial
            is Rewarded -> AdType.Rewarded
            is Banner -> AdType.Banner
        }
    }

    private fun trackEvent(
        eventName: TrackingEventName,
        message: String,
        impressionId: String?,
    ) {
        val adTypeEncodedName = ad?.toAdType()?.name ?: "Unknown"
        val loc = ad?.location ?: ""
        when (eventName) {
            TrackingEventName.Click.INVALID_URL_ERROR ->
                CriticalEvent(
                    eventName,
                    message,
                    adTypeEncodedName,
                    loc,
                    adUnitRenderer.mediation,
                    impressionId.asTrackAd(),
                )
            else ->
                InfoEvent(
                    eventName,
                    message,
                    adTypeEncodedName,
                    loc,
                    adUnitRenderer.mediation,
                    impressionId.asTrackAd(),
                )
        }.track()
    }

    private fun updateSession() {
        ad?.toAdType()?.let {
            session.addSessionImpression(it)
            i(
                "Current session impression count: " +
                    session.getSessionImpressionsCounter(it) +
                    " in session: " +
                    session.sessionCounter,
            )
        }
    }
}
