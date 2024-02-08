/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers.banners

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.Ilrd
import com.chartboost.heliumsdk.ad.HeliumBannerAd
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.STANDARD
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.asSize
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.domain.PartnerAdUtils.getCreativeSizeFromPartnerAdDetails
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking
import com.chartboost.heliumsdk.network.Endpoints
import com.chartboost.heliumsdk.network.model.BannerAdDimensions
import com.chartboost.heliumsdk.network.model.BannerSizeBody
import com.chartboost.heliumsdk.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.lang.ref.WeakReference

/**
 * @suppress
 *
 * Handles loading and refreshing banners.
 */
class BannerController(
    private val heliumBannerAdRef: WeakReference<HeliumBannerAd>,
    private val fullscreenAdShowingState: FullscreenAdShowingState? = HeliumSdk.chartboostMediationInternal.fullscreenAdShowingState,
    private val ilrd: Ilrd? = HeliumSdk.chartboostMediationInternal.ilrd,
) {
    /**
     * Whether or not auto refresh is enabled for this placement
     */
    val shouldAutoRefresh: Boolean
        get() = PlacementStorage.shouldRefresh(getBannerAdPlacementName())

    /**
     * How long to wait between each refresh.
     */
    private val refreshTimeMillis
        get() = PlacementStorage.getRefreshTime(getBannerAdPlacementName()) * 1000

    /**
     * How long to wait before verifying the ad size.
     */
    private val timeToVerifyAdSizeJobMillis
        get() = AppConfigStorage.bannerSizeEventDelayMs

    /**
     * The penalty time if we fail too many loads in a row.
     */
    private val maxRefreshTime = PlacementStorage.getMaxRefreshTime() * 1000

    /**
     * How many failed loads before we go into penalty time.
     */
    private val maxTriesUntilPenaltyTime = PlacementStorage.getMaxTriesUntilPenaltyTime()

    /**
     * Handler to send messages.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Observer for keeping track of any showing fullscreen ads.
     */
    private val fullscreenAdShowingStateObserver =
        object : FullscreenAdShowingState.FullscreenAdShowingStateObserver {
            override fun onFullscreenAdShown() {
                pauseRefresh()
            }

            override fun onFullscreenAdDismissed() {
                checkAndResumeRefresh()
            }
        }

    /**
     * Whether or not the HeliumBannerAd is attached to the screen and is ready to refresh ads.
     */
    private var isHeliumBannerAdReadyToRefresh = false

    /**
     * How many refreshes have been failed in a row.
     */
    private var refreshesFailed = 0

    /**
     * The visibility tracker to check the visibility of an ad before we are notified of an impression event.
     */
    private var visibilityTracker: VisibilityTracker? = null

    /**
     * The next cached ad.
     */
    private var nextAd: CachedAd? = null

    /**
     * The currently showing ad
     */
    private var currentlyShowingAd: CachedAd? = null

    /**
     * Whether or not this is a publisher triggered load.
     */
    private var isPublisherTriggeredLoad = false

    /**
     * The timestamp of when the banner was last shown. This is also the timestamp of the last time
     * the auto refresh was resumed. This is also used for failed loads and is reset when an ad
     * fails its load.
     */
    private var bannerShownUptimeMillis: Long = SystemClock.uptimeMillis()

    /**
     * The total time the banner has already been on the screen not counting the current
     * time difference between now and auto refresh having been resumed.
     */
    private var shownDurationMillis: Long = 0

    /**
     * If there is an ad being shown.
     */
    private var isShowingAd = false

    /**
     * We need this to only ever have one of these running
     */
    private var swapAdJob: Job? = null

    /**
     * This is the [Job] representing the AdController load.
     */
    private var fetchAdJob: Job? = null

    /**
     * We need this to only ever have one of these running. This is the [Job] to wait for the next refresh.
     */
    private var nextAdJob: Job? = null

    /**
     * The current auto refresh state.
     */
    private var isAutoRefreshResumed = true

    /**
     * Convenience function to get the placement name.
     */
    private fun getBannerAdPlacementName(): String {
        heliumBannerAdRef.get()?.let {
            return it.placementName
        } ?: return ""
    }

    internal fun renewCachedAd() {
        // all jobs are also cancelled by pauseRefresh()
        pauseRefresh()
        isPublisherTriggeredLoad = true
        getNextAd(forceRefresh = true)
    }

    /**
     * Starts a load. If a load is in progress, do nothing. Since this is considered a publisher
     * initiated action, set it so that callbacks do happen if there happens to be a load request
     * in flight. If there is already an ad loaded, immediately show the ad.
     *
     * @return The load request ID associated with this load.
     */
    fun load() {
        if (isPublisherTriggeredLoad) {
            LogController.w(ChartboostMediationError.CM_LOAD_FAILURE_LOAD_IN_PROGRESS.message)
            return
        }
        isPublisherTriggeredLoad = true
        if (fetchAdJob != null) {
            LogController.w(
                "${ChartboostMediationError.CM_LOAD_FAILURE_LOAD_IN_PROGRESS.message} Treating the next load as a publisher initiated load.",
            )
            return
        }
        nextAd?.let { heliumAd ->
            heliumAd.partnerAd?.let { partnerAd ->
                LogController.d("Returning cached ad.")
                if (partnerAd.inlineView != null) {
                    mainHandler.post {
                        heliumBannerAdRef.get()?.heliumBannerAdListener?.onAdCached(
                            partnerAd.request.chartboostPlacement,
                            heliumAd.loadId,
                            heliumAd.winningBidInfo,
                            null,
                            getCreativeSizeFromPartnerAdDetails(
                                partnerAd,
                                partnerAd.request.size ?: STANDARD.asSize()
                            ),
                        )
                            ?: LogController.e("The Helium SDK Banner listener is detached on onHeliumAdLoaded for onAdCached.")
                    }
                    isPublisherTriggeredLoad = false
                    return
                }
            }
        }

        fullscreenAdShowingState?.subscribe(fullscreenAdShowingStateObserver)
        getNextAd()
        return
    }

    /**
     * Clears the showing ad and the cached ad. Set it to ignore the inflight load request if there
     * is one.
     */
    fun clearAd() {
        resetState()
    }

    private fun invalidateAd(cachedAd: CachedAd?) {
        cachedAd?.let {
            HeliumSdk.chartboostMediationInternal.adController?.invalidate(cachedAd)
                ?: LogController.e("Failed to invalidate ad due to no ad controller.")
        }
    }

    private fun cancelAllJobs() {
        fetchAdJob?.cancel()
        nextAdJob?.cancel()
        swapAdJob?.cancel()
        fetchAdJob = null
        nextAdJob = null
        swapAdJob = null
    }

    /**
     * This is to notify the controller that the [HeliumBannerAd] is back on the screen.
     */
    fun onHeliumBannerAdResumeRefresh() {
        isHeliumBannerAdReadyToRefresh = true
        checkAndResumeRefresh()
    }

    /**
     * Check to see if the [HeliumBannerAd] is on the screen and that a fullscreen ad is not showing.
     * If those conditions are true, then schedule either an ad swap if an ad is loaded or an
     * ad refresh if there is no loaded ad.
     */
    private fun checkAndResumeRefresh() {
        if (!isHeliumBannerAdReadyToRefresh || fullscreenAdShowingState?.isFullscreenAdShowing == true) {
            return
        }
        isAutoRefreshResumed = true
        bannerShownUptimeMillis = SystemClock.uptimeMillis()
        val nextAd = nextAd
        if (nextAd != null) {
            scheduleAdSwap()
            return
        }
        scheduleNextRefresh()
    }

    /**
     * This is to notify the controller that the [HeliumBannerAd] is no longer on the screen. This
     * can be because the screen is off, the app is paused, or the banner has been removed from
     * the view hierarchy.
     */
    fun onHeliumBannerAdPauseRefresh() {
        isHeliumBannerAdReadyToRefresh = false
        pauseRefresh()
    }

    /**
     * Pauses auto refresh. Saves how long the ad has been shown for.
     */
    private fun pauseRefresh() {
        // If auto refresh was already paused due to another event, then don't do this again.
        if (!isAutoRefreshResumed) {
            return
        }
        isAutoRefreshResumed = false
        val bannerShownUptimeMillis = bannerShownUptimeMillis
        shownDurationMillis += SystemClock.uptimeMillis() - bannerShownUptimeMillis
        LogController.d("Auto refresh paused. Already shown for $shownDurationMillis millis")
        cancelAllJobs()
    }

    /**
     * Cleans up state. BannerController is no longer usable after this call.
     */
    fun destroy() {
        resetState()
        heliumBannerAdRef.clear()
    }

    internal fun getCreativeSizeDips(bannerSize: HeliumBannerAd.HeliumBannerSize?): Size {
        var adapterProvidedSize: Size? = null

        try {
            if (bannerSize?.isAdaptive == true) {
                adapterProvidedSize =
                    currentlyShowingAd?.partnerAd?.let {
                        getCreativeSizeFromPartnerAdDetails(
                            it,
                            bannerSize.asSize()
                        )
                    }
            }
        } catch (e: Exception) {
            LogController.e("Encountered a problem getting the creative size: ${e.message}")
        }

        return adapterProvidedSize ?: Size(
            bannerSize?.width ?: STANDARD.width,
            bannerSize?.height ?: STANDARD.height,
        )
    }

    private fun resetState() {
        isPublisherTriggeredLoad = false
        isShowingAd = false
        visibilityTracker?.destroy()
        visibilityTracker = null
        cancelAllJobs()
        invalidateAd(currentlyShowingAd)
        invalidateAd(nextAd)
        currentlyShowingAd = null
        nextAd = null
        heliumBannerAdRef.get()?.removeAllViews()
        fullscreenAdShowingState?.unsubscribe(fullscreenAdShowingStateObserver)
    }

    /**
     * Gets the next ad.
     */
    private fun getNextAd(forceRefresh: Boolean = false) {
        if (!forceRefresh && (nextAd != null || fetchAdJob != null)) {
            LogController.i("Already loading an ad.")
            return
        }
        heliumBannerAdRef.get()?.let { heliumBannerAd ->
            val loadId = Environment.sessionId + System.currentTimeMillis()
            fetchAdJob =
                CoroutineScope(Main).launch(
                    CoroutineExceptionHandler { _, error ->
                        fetchAdJob = null
                        if (isPublisherTriggeredLoad) {
                            CoroutineScope(Main).launch {
                                heliumBannerAd.heliumBannerAdListener?.onAdCached(
                                    heliumBannerAd.placementName,
                                    loadId,
                                    mapOf(),
                                    if (error is ChartboostMediationAdException) {
                                        error
                                    } else {
                                        ChartboostMediationAdException(
                                            ChartboostMediationError.CM_UNKNOWN_ERROR,
                                        )
                                    },
                                    currentlyShowingAd?.partnerAd?.let {
                                        getCreativeSizeFromPartnerAdDetails(
                                            it,
                                            it.request.size ?: STANDARD.asSize()
                                        )
                                    } ?: STANDARD.asSize(),
                                )
                            }
                        }
                    },
                ) {
                    // This is a placeholder set for load metrics for banner that nothing else is using.
                    // Feel free to utilize this when the public banner API should also return load metrics.
                    val loadMetrics = mutableSetOf<Metrics>()
                    val loadResult =
                        HeliumSdk.chartboostMediationInternal.adController?.load(
                            heliumBannerAd.context,
                            AdLoadParams(
                                adIdentifier = AdIdentifier(heliumBannerAd.getAdType(), heliumBannerAd.placementName),
                                keywords = heliumBannerAd.keywords,
                                loadId = loadId,
                                bannerSize = heliumBannerAd.getSize(),
                                adInteractionListener =
                                    object : AdInteractionListener {
                                        override fun onImpressionTracked(partnerAd: PartnerAd) {
                                            // Helium ignores partner impression.
                                        }

                                        override fun onClicked(partnerAd: PartnerAd) {
                                            heliumBannerAdRef.get()?.heliumBannerAdListener?.onAdClicked(
                                                partnerAd.request.chartboostPlacement,
                                            )
                                        }

                                        override fun onRewarded(partnerAd: PartnerAd) {
                                            // This should not happen
                                            LogController.e("${partnerAd.request.chartboostPlacement} Banner received rewarded callback?")
                                        }

                                        override fun onDismissed(
                                            partnerAd: PartnerAd,
                                            error: ChartboostMediationAdException?,
                                        ) {
                                            // This should not happen
                                            LogController.e("${partnerAd.request.chartboostPlacement} Banner received dismissed callback?")
                                        }

                                        override fun onExpired(partnerAd: PartnerAd) {
                                            // No action for now
                                            LogController.e("${partnerAd.request.chartboostPlacement} Banner received expired callback?")
                                        }
                                    },
                            ),
                            loadMetrics,
                        )
                    fetchAdJob = null
                    if (!isActive) {
                        return@launch
                    }
                    loadResult?.fold({
                        if (it.partnerAd?.inlineView != null) {
                            if (forceRefresh) {
                                // if we're forcing a refresh, set the time shown to equal 1 greater than the refresh time threshold
                                shownDurationMillis = 1L + refreshTimeMillis
                            }
                            handleLoadSuccess(it, loadId)
                        } else {
                            handleLoadFailure(
                                loadId,
                                ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_NO_INLINE_VIEW),
                            )
                        }
                    }, {
                        handleLoadFailure(loadId, it)
                    }) ?: run {
                        LogController.e("Helium is not initialized.")
                        CoroutineScope(Main).launch {
                            heliumBannerAd.heliumBannerAdListener?.onAdCached(
                                heliumBannerAd.placementName,
                                loadId,
                                mapOf(),
                                ChartboostMediationAdException(
                                    ChartboostMediationError.CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED,
                                ),
                                currentlyShowingAd?.partnerAd?.let {
                                    getCreativeSizeFromPartnerAdDetails(
                                        it,
                                        it.request.size ?: STANDARD.asSize()
                                    )
                                } ?: STANDARD.asSize(),
                            )
                        }
                    }
                }
        } ?: LogController.e("The Helium SDK Banner reference is missing on getNextAd()")

        checkAndResumeRefresh()
    }

    private fun handleLoadSuccess(
        cachedAd: CachedAd,
        loadId: String,
    ) {
        cachedAd.loadId = loadId
        nextAd = cachedAd
        refreshesFailed = 0
        scheduleAdSwap()

        if (isPublisherTriggeredLoad) {
            CoroutineScope(Main).launch {
                heliumBannerAdRef.get()?.heliumBannerAdListener?.onAdCached(
                    getBannerAdPlacementName(),
                    loadId,
                    cachedAd.winningBidInfo,
                    null,
                    currentlyShowingAd?.partnerAd?.let {
                        getCreativeSizeFromPartnerAdDetails(
                            it,
                            it.request.size ?: STANDARD.asSize()
                        )
                    } ?: STANDARD.asSize(),
                )
            }
        }
        isPublisherTriggeredLoad = false
    }

    private fun handleLoadFailure(
        loadId: String,
        error: Throwable,
    ) {
        refreshesFailed++
        shownDurationMillis = 0
        scheduleNextRefresh()

        if (isPublisherTriggeredLoad) {
            CoroutineScope(Main).launch {
                heliumBannerAdRef.get()?.heliumBannerAdListener?.onAdCached(
                    getBannerAdPlacementName(),
                    loadId,
                    mapOf(),
                    if (error is ChartboostMediationAdException) {
                        error
                    } else {
                        ChartboostMediationAdException(
                            ChartboostMediationError.CM_LOAD_FAILURE_UNKNOWN,
                        )
                    },
                    currentlyShowingAd?.partnerAd?.let {
                        getCreativeSizeFromPartnerAdDetails(
                            it,
                            it.request.size ?: STANDARD.asSize()
                        )
                    } ?: STANDARD.asSize(),
                )
            }
        }
        isPublisherTriggeredLoad = false
    }

    /**
     * Schedule to swap in the new ad. This is immediately if an ad is not already showing. Otherwise,
     * we wait for [refreshTimeMillis] minus the amount of time we've already seen the previous ad.
     */
    private fun scheduleAdSwap() {
        if (!isHeliumBannerAdReadyToRefresh) {
            LogController.d("Waiting on ad swap since banner is offscreen.")
            return
        }
        val currentUptimeMillis = SystemClock.uptimeMillis()
        val bannerShownUptimeMillis = bannerShownUptimeMillis
        val totalTimeVisibleMillis =
            currentUptimeMillis - bannerShownUptimeMillis + shownDurationMillis
        // If there isn't an ad showing or the time threshold has been met, go ahead and show it
        if (!isShowingAd || totalTimeVisibleMillis > refreshTimeMillis || !shouldAutoRefresh) {
            swapAd()
            return
        }
        // otherwise, schedule it
        var timeToRefreshMillis = refreshTimeMillis - totalTimeVisibleMillis
        if (timeToRefreshMillis < 0) {
            timeToRefreshMillis = 0
        }
        LogController.d("Scheduling a banner ad swap in $timeToRefreshMillis millis.")
        swapAdJob?.cancel()
        swapAdJob =
            CoroutineScope(Main).launch {
                withContext(IO) {
                    delay(timeToRefreshMillis)
                }
                if (!isActive) return@launch
                swapAd()
            }
    }

    /**
     * Swaps the ad in.
     */
    private fun swapAd() {
        val heliumBannerAd =
            heliumBannerAdRef.get() ?: run {
                LogController.d("Failed to swap ad because reference to HeliumBannerAd lost")
                return
            }
        val nextAd =
            nextAd ?: run {
                LogController.d("Attempting to swap ad with no loaded ad.")
                return
            }
        val partnerAd =
            nextAd.partnerAd ?: run {
                LogController.d("Attempting to swap ad with no loaded partner ad. ${ChartboostMediationError.CM_LOAD_FAILURE_NO_FILL}")
                return
            }
        val nextBannerAdView =
            partnerAd.inlineView ?: run {
                LogController.d("Attempting to swap ad with no loaded ad view. ${ChartboostMediationError.CM_LOAD_FAILURE_NO_INLINE_VIEW}")
                return
            }

        LogController.d("Showing banner.")

        // Save this for later
        val previousAd = currentlyShowingAd
        currentlyShowingAd = nextAd

        // Clear out the existing ad
        this.nextAd = null

        // Just in case the view was already attached, detatch it in preparation to attach it here.
        (nextBannerAdView.parent as? ViewGroup)?.removeView(nextBannerAdView)

        // Store the previous children to remove them later
        val previousChildren = mutableListOf<View>()
        for (i in 0 until heliumBannerAd.childCount) {
            previousChildren.add(heliumBannerAd.getChildAt(i))
        }

        val bannerSize = heliumBannerAd.getSize()
        val density: Double = heliumBannerAd.context.resources.displayMetrics.density.toDouble()
        val layoutParams =
            when {
                bannerSize?.isAdaptive == true -> {
                    FrameLayout.LayoutParams(
                        (getCreativeSizeDips(bannerSize).width * density).toInt(),
                        (getCreativeSizeDips(bannerSize).height * density).toInt(),
                    )
                }
                bannerSize != null -> {
                    FrameLayout.LayoutParams(
                        (bannerSize.width * density).toInt(),
                        (bannerSize.height * density).toInt(),
                    )
                }
                else -> {
                    FrameLayout.LayoutParams(
                        (STANDARD.width * density).toInt(),
                        (STANDARD.height * density).toInt(),
                    )
                }
            }
        layoutParams.gravity = Gravity.CENTER
        heliumBannerAd.addView(nextBannerAdView, layoutParams)

        // Remove the previous child views. We do it after we have added the current ad to avoid
        // a blank view for a moment while the swap is happening.
        for (child in previousChildren) {
            heliumBannerAd.removeView(child)
        }

        isShowingAd = true
        shownDurationMillis = 0
        // This is done just in case. We actually want to wait for the visibility tracker.
        bannerShownUptimeMillis = SystemClock.uptimeMillis()

        visibilityTracker?.destroy()
        visibilityTracker =
            VisibilityTracker(
                heliumBannerAd.context,
                nextBannerAdView,
                VisibilityTracker.getTopmostView(heliumBannerAd.context, heliumBannerAd)
                    ?: heliumBannerAd,
                AppConfigStorage.bannerImpressionMinVisibleDips,
                AppConfigStorage.bannerImpressionMinVisibleDurationMs,
                AppConfigStorage.visibilityTrackerPollIntervalMs,
                AppConfigStorage.visibilityTrackerTraversalLimit,
            ).also {
                it.visibilityTrackerListener =
                    object : VisibilityTracker.VisibilityTrackerListener {
                        override fun onVisibilityThresholdMet() {
                            sendShowMetricsData(
                                startTime = System.currentTimeMillis(),
                                partnerName = partnerAd.request.partnerId,
                                auctionId = nextAd.auctionId,
                                loadId = nextAd.loadId,
                            )
                            val placementName = getBannerAdPlacementName()
                            HeliumSdk.chartboostMediationInternal.adController?.incrementBannerImpressionDepth()
                                ?: LogController.e("Failed to increment banner impression depth due to no ad controller.")
                            mainHandler.post {
                                heliumBannerAdRef.get()?.heliumBannerAdListener?.onAdImpressionRecorded(
                                    placementName,
                                )
                                    ?: LogController.e("The Helium SDK Banner listener is detached on onAdImpressionRecorded.")
                            }
                            CoroutineScope(IO).launch {
                                ChartboostMediationNetworking.trackChartboostImpression(nextAd.auctionId, nextAd.loadId)

                                delay(timeToVerifyAdSizeJobMillis)

                                if (!isActive) {
                                    return@launch
                                }

                                val creativeWidth =
                                    Dips.pixelsToIntDips(
                                        nextBannerAdView.width,
                                        nextBannerAdView.context,
                                    )
                                val creativeHeight =
                                    Dips.pixelsToIntDips(
                                        nextBannerAdView.height,
                                        nextBannerAdView.context,
                                    )

                                val containerWidth =
                                    Dips.pixelsToIntDips(
                                        heliumBannerAd.width,
                                        heliumBannerAd.context,
                                    )
                                val containerHeight =
                                    Dips.pixelsToIntDips(
                                        heliumBannerAd.height,
                                        heliumBannerAd.context,
                                    )

                                if (creativeWidth <= containerWidth && creativeHeight <= containerHeight) {
                                    return@launch
                                }

                                val requestedWidth = nextAd.partnerAd?.request?.size?.width ?: 0
                                val requestedHeight = nextAd.partnerAd?.request?.size?.height ?: 0

                                ChartboostMediationNetworking.trackAdaptiveBannerSize(
                                    loadId = nextAd.loadId,
                                    BannerSizeBody(
                                        auctionId = nextAd.auctionId,
                                        creativeSize =
                                            BannerAdDimensions(
                                                width = creativeWidth,
                                                height = creativeHeight,
                                            ),
                                        containerSize =
                                            BannerAdDimensions(
                                                width = containerWidth,
                                                height = containerHeight,
                                            ),
                                        requestSize =
                                            BannerAdDimensions(
                                                width = requestedWidth,
                                                height = requestedHeight,
                                            ),
                                    ),
                                )
                            }

                            nextAd.ilrdJson?.let { ilrdJson ->
                                ilrd?.onIlrdReceived(
                                    partnerAd.request.chartboostPlacement,
                                    ilrdJson.toJSONObject(),
                                )
                            }

                            bannerShownUptimeMillis = SystemClock.uptimeMillis()
                            scheduleNextRefresh()
                        }
                    }
                it.start()
            }

        // Now we invalidate the previous banner
        invalidateAd(previousAd)
    }

    /**
     * Schedule the next ad refresh.
     */
    private fun scheduleNextRefresh() {
        if (!shouldAutoRefresh) {
            return
        }
        if (!isHeliumBannerAdReadyToRefresh) {
            LogController.d("HeliumBannerAd is not on screen. Not refreshing.")
            return
        }

        var timeToRefreshMillis =
            if (nextAd == null && refreshesFailed < 1) {
                0
            } else if (refreshesFailed >= maxTriesUntilPenaltyTime) {
                maxRefreshTime - shownDurationMillis
            } else {
                refreshTimeMillis - shownDurationMillis
            }

        // Just in case we go negative
        if (timeToRefreshMillis < 0) {
            timeToRefreshMillis = 0
        }
        LogController.d("Scheduling next banner refresh in $timeToRefreshMillis millis.")
        nextAdJob?.cancel()
        nextAdJob =
            CoroutineScope(Main).launch {
                withContext(IO) {
                    delay(timeToRefreshMillis)
                }
                if (!isActive) return@launch
                getNextAd()
            }
    }

    private fun sendShowMetricsData(
        startTime: Long,
        partnerName: String,
        auctionId: String,
        loadId: String,
    ) {
        val metrics = Metrics(partnerName, Endpoints.Sdk.Event.SHOW)
        val showMetricsDataSet: MutableSet<Metrics> = HashSet()
        metrics.auctionId = auctionId
        metrics.start = startTime
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = true
        showMetricsDataSet.add(metrics)
        MetricsManager.postMetricsData(showMetricsDataSet, loadId)
    }
}
