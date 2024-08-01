/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.controllers.banners

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.Ilrd
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdLoadRequest
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdLoadResult
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.STANDARD
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.asSize
import com.chartboost.chartboostmediationsdk.controllers.AdController
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.domain.PartnerAdUtils.getCreativeSizeFromPartnerAdDetails
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.network.model.BannerAdDimensions
import com.chartboost.chartboostmediationsdk.network.model.BannerSizeBody
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.utils.*
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * @suppress
 *
 * Handles loading and refreshing banners.
 */
class BannerController(
    private val chartboostMediationBannerAdViewRef: WeakReference<ChartboostMediationBannerAdView>,
    private val fullscreenAdShowingState: FullscreenAdShowingState? = ChartboostMediationSdk.chartboostMediationInternal.fullscreenAdShowingState,
    private val ilrd: Ilrd? = ChartboostMediationSdk.chartboostMediationInternal.ilrd,
) {
    /**
     * Whether or not auto refresh is enabled for this placement
     */
    val shouldAutoRefresh: Boolean
        get() = PlacementStorage.shouldRefresh(getBannerAdPlacement())

    /**
     * How long to wait between each refresh.
     */
    private val refreshTimeMillis
        get() = PlacementStorage.getRefreshTime(getBannerAdPlacement()) * 1000

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
     * Whether or not the ChartboostMediationBannerAd is attached to the screen and is ready to refresh ads.
     */
    private var isChartboostMediationBannerAdReadyToRefresh = false

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
    private var fetchAdJob: Deferred<ChartboostMediationBannerAdLoadResult>? = null

    /**
     * We need this to only ever have one of these running. This is the [Job] to wait for the next refresh.
     */
    private var nextAdJob: Job? = null

    /**
     * The current auto refresh state.
     */
    private var isAutoRefreshResumed = true

    private var cachedRequest: ChartboostMediationBannerAdLoadRequest? = null

    /**
     * Convenience function to get the placement name.
     */
    private fun getBannerAdPlacement(): String {
        chartboostMediationBannerAdViewRef.get()?.let {
            return it.placement
        } ?: return ""
    }

    internal suspend fun renewCachedAd(): ChartboostMediationBannerAdLoadResult =
        withContext(Main) {
            cachedRequest?.let {
                // all jobs are also cancelled by pauseRefresh()
                pauseRefresh()
                isPublisherTriggeredLoad = true
                return@withContext getNextAd(it, forceRefresh = true)
            } ?: run {
                LogController.e("Failed to renew banner ad because `cachedRequest` is null.")
                return@withContext createAdLoadResult(
                    loadId = "",
                    createPayloadJson(),
                    ChartboostMediationError.LoadError.Unknown,
                    placement = "",
                    bannerSize = Size(0, 0),
                )
            }
        }

    /**
     * Starts a load. If a load is in progress, do nothing. Since this is considered a publisher
     * initiated action, set it so that callbacks do happen if there happens to be a load request
     * in flight. If there is already an ad loaded, immediately show the ad.
     *
     * @return The load request ID associated with this load.
     */
    suspend fun load(request: ChartboostMediationBannerAdLoadRequest): ChartboostMediationBannerAdLoadResult =
        withContext(Main) {
            if (request.placement == cachedRequest?.placement && request.size == cachedRequest?.size) {
                return@withContext renewCachedAd()
            }

            val loadId = generateLoadId()

            if (isPublisherTriggeredLoad) {
                LogController.w(ChartboostMediationError.LoadError.LoadInProgress.message)
                return@withContext createAdLoadResult(
                    loadId,
                    createPayloadJson(),
                    ChartboostMediationError.LoadError.LoadInProgress,
                    placement = request.placement,
                    bannerSize = request.size.asSize(),
                )
            }

            val chartboostMediationBannerAdView = chartboostMediationBannerAdViewRef.get()

            if (chartboostMediationBannerAdView == null) {
                LogController.e("Failed to load banner ad because `chartboostMediationBannerAdView` is null.")
                return@withContext createAdLoadResult(
                    loadId,
                    createPayloadJson(),
                    ChartboostMediationError.LoadError.Unknown,
                    placement = request.placement,
                    bannerSize = request.size.asSize(),
                )
            }

            isPublisherTriggeredLoad = true

            if (fetchAdJob != null) {
                LogController.w(
                    "${ChartboostMediationError.LoadError.LoadInProgress.message} Treating the next load as a publisher initiated load.",
                )
                return@withContext createAdLoadResult(
                    loadId,
                    createPayloadJson(),
                    ChartboostMediationError.LoadError.LoadInProgress,
                    placement = request.placement,
                    bannerSize = request.size.asSize(),
                )
            }

            val adLoadParams =
                createAdLoadParams(
                    request,
                    loadId,
                    chartboostMediationBannerAdView,
                )

            val (metricsSet, loadResult) =
                performAdLoad(
                    chartboostMediationBannerAdView.context,
                    adLoadParams,
                    ChartboostMediationSdk.chartboostMediationInternal.adController,
                )

            val error = getError(loadResult)

            cachedRequest = request

            if (error != null) {
                LogController.e("Failed to load banner ad with error: $error")
                return@withContext createAdLoadResult(
                    loadId,
                    createPayloadJson(),
                    error,
                    placement = request.placement,
                    bannerSize = request.size.asSize(),
                )
            }

            val cachedAd = getCachedAd(loadResult)

            val result =
                createAdLoadResult(
                    cachedAd.loadId,
                    createPayloadJson(metricsSet),
                    null,
                    cachedAd.winningBidInfo,
                    request.placement,
                    cachedAd.partnerAd?.let { getCreativeSizeFromPartnerAdDetails(it) }
                        ?: STANDARD.asSize(),
                )

            handleLoadSuccess(cachedAd, loadId)

            swapAd()

            fullscreenAdShowingState?.subscribe(fullscreenAdShowingStateObserver)

            return@withContext result
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
            ChartboostMediationSdk.chartboostMediationInternal.adController?.invalidate(cachedAd)
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
     * This is to notify the controller that the [ChartboostMediationBannerAdView] is back on the screen.
     */
    fun onChartboostMediationBannerAdResumeRefresh() {
        isChartboostMediationBannerAdReadyToRefresh = true
        checkAndResumeRefresh()
    }

    /**
     * Check to see if the [ChartboostMediationBannerAdView] is on the screen and that a fullscreen ad is not showing.
     * If those conditions are true, then schedule either an ad swap if an ad is loaded or an
     * ad refresh if there is no loaded ad.
     */
    private fun checkAndResumeRefresh() {
        if (!isChartboostMediationBannerAdReadyToRefresh || fullscreenAdShowingState?.isFullscreenAdShowing == true) {
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
     * This is to notify the controller that the [ChartboostMediationBannerAdView] is no longer on the screen. This
     * can be because the screen is off, the app is paused, or the banner has been removed from
     * the view hierarchy.
     */
    fun onChartboostMediationBannerAdPauseRefresh() {
        isChartboostMediationBannerAdReadyToRefresh = false
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
        chartboostMediationBannerAdViewRef.clear()
    }

    internal fun getCreativeSizeDips(bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?): Size {
        var adapterProvidedSize: Size? = null

        try {
            if (bannerSize?.isAdaptive == true) {
                adapterProvidedSize =
                    currentlyShowingAd?.partnerAd?.let {
                        getCreativeSizeFromPartnerAdDetails(it)
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
        chartboostMediationBannerAdViewRef.get()?.removeAllViews()
        fullscreenAdShowingState?.unsubscribe(fullscreenAdShowingStateObserver)
    }

    private suspend fun performAdLoad(
        context: Context,
        adLoadParams: AdLoadParams,
        adController: AdController?,
    ): Pair<MutableSet<Metrics>, Result<CachedAd>?> =
        run {
            val metricsSet = mutableSetOf<Metrics>()
            var loadResult: Result<CachedAd>? = null

            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, error ->
                    loadResult = Result.failure(error)
                },
            ) {
                loadResult = adController?.load(context, adLoadParams, metricsSet)
                    ?: Result.failure(
                        ChartboostMediationAdException(ChartboostMediationError.LoadError.ChartboostMediationNotInitialized),
                    )
            }.also { it.join() }

            return Pair(metricsSet, loadResult)
        }

    /**
     * Gets the next ad.
     */
    private suspend fun getNextAd(
        request: ChartboostMediationBannerAdLoadRequest,
        forceRefresh: Boolean = false,
    ): ChartboostMediationBannerAdLoadResult =
        withContext(Main) {
            val loadId = generateLoadId()

            fetchAdJob =
                CoroutineScope(Main).async(
                    CoroutineExceptionHandler { _, error ->
                        fetchAdJob = null
                        if (isPublisherTriggeredLoad) {
                            LogController.w(ChartboostMediationError.LoadError.LoadInProgress.message)
                        }
                    },
                ) {
                    // This is a placeholder set for load metrics for banner that nothing else is using.
                    // Feel free to utilize this when the public banner API should also return load metrics.

                    fetchAdJob = null

                    if (!isActive) {
                        return@async createAdLoadResult(
                            loadId,
                            createPayloadJson(),
                            ChartboostMediationError.LoadError.Unknown,
                            placement = request.placement,
                            bannerSize = request.size.asSize(),
                        )
                    }

                    val chartboostMediationBannerAdView = chartboostMediationBannerAdViewRef.get()

                    if (chartboostMediationBannerAdView == null) {
                        LogController.e("Failed to load banner ad because `chartboostMediationBannerAdView` is null.")
                        handleLoadFailure()
                        return@async createAdLoadResult(
                            loadId,
                            createPayloadJson(),
                            ChartboostMediationError.LoadError.Unknown,
                            placement = request.placement,
                            bannerSize = request.size.asSize(),
                        )
                    }

                    val adLoadParams =
                        createAdLoadParams(
                            request,
                            loadId,
                            chartboostMediationBannerAdView,
                        )

                    val (metricsSet, loadResult) =
                        performAdLoad(
                            chartboostMediationBannerAdView.context,
                            adLoadParams,
                            ChartboostMediationSdk.chartboostMediationInternal.adController,
                        )

                    loadResult?.fold({
                        if (it.partnerAd?.inlineView != null) {
                            if (forceRefresh) {
                                // if we're forcing a refresh, set the time shown to equal 1 greater than the refresh time threshold
                                shownDurationMillis = 1L + refreshTimeMillis
                            }
                            handleLoadSuccess(it, loadId)
                        } else {
                            handleLoadFailure()
                        }
                    }, {
                        handleLoadFailure()
                    }) ?: LogController.e("Chartboost Mediation is not initialized.")

                    checkAndResumeRefresh()

                    return@async createAdLoadResult(
                        loadId,
                        createPayloadJson(metricsSet),
                        ChartboostMediationError.LoadError.Unknown,
                        placement = request.placement,
                        bannerSize = request.size.asSize(),
                    )
                }

            return@withContext fetchAdJob?.await() ?: createAdLoadResult(
                loadId,
                createPayloadJson(),
                ChartboostMediationError.LoadError.Unknown,
                placement = request.placement,
                bannerSize = request.size.asSize(),
            )
        }

    private fun handleLoadSuccess(
        cachedAd: CachedAd,
        loadId: String,
    ) {
        cachedAd.loadId = loadId
        nextAd = cachedAd
        refreshesFailed = 0
        scheduleAdSwap()

        isPublisherTriggeredLoad = false
    }

    private fun handleLoadFailure() {
        refreshesFailed++
        shownDurationMillis = 0
        scheduleNextRefresh()

        isPublisherTriggeredLoad = false
    }

    /**
     * Schedule to swap in the new ad. This is immediately if an ad is not already showing. Otherwise,
     * we wait for [refreshTimeMillis] minus the amount of time we've already seen the previous ad.
     */
    private fun scheduleAdSwap() {
        if (!isChartboostMediationBannerAdReadyToRefresh) {
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
        val chartboostMediationBannerAd =
            chartboostMediationBannerAdViewRef.get() ?: run {
                LogController.d("Failed to swap ad because reference to ChartboostMediationBannerAd lost")
                return
            }
        val nextAd =
            nextAd ?: run {
                LogController.d("Attempting to swap ad with no loaded ad.")
                return
            }
        val partnerAd =
            nextAd.partnerAd ?: run {
                LogController.d("Attempting to swap ad with no loaded partner ad. ${ChartboostMediationError.LoadError.NoFill}")
                return
            }
        val nextBannerAdView =
            partnerAd.inlineView ?: run {
                LogController.d("Attempting to swap ad with no loaded ad view. ${ChartboostMediationError.LoadError.NoBannerView}")
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
        for (i in 0 until chartboostMediationBannerAd.childCount) {
            previousChildren.add(chartboostMediationBannerAd.getChildAt(i))
        }

        val bannerSize = chartboostMediationBannerAd.getSize()
        val density: Double =
            chartboostMediationBannerAd.context.resources.displayMetrics.density.toDouble()
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
        chartboostMediationBannerAd.addView(nextBannerAdView, layoutParams)

        // Remove the previous child views. We do it after we have added the current ad to avoid
        // a blank view for a moment while the swap is happening.
        for (child in previousChildren) {
            chartboostMediationBannerAd.removeView(child)
        }

        isShowingAd = true
        shownDurationMillis = 0
        // This is done just in case. We actually want to wait for the visibility tracker.
        bannerShownUptimeMillis = SystemClock.uptimeMillis()

        visibilityTracker?.destroy()
        visibilityTracker =
            VisibilityTracker(
                chartboostMediationBannerAd.context,
                nextBannerAdView,
                VisibilityTracker.getTopmostView(
                    chartboostMediationBannerAd.context,
                    chartboostMediationBannerAd,
                )
                    ?: chartboostMediationBannerAd,
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
                            val placement = getBannerAdPlacement()
                            ChartboostMediationSdk.chartboostMediationInternal.adController?.incrementBannerImpressionDepth()
                                ?: LogController.e("Failed to increment banner impression depth due to no ad controller.")
                            mainHandler.post {
                                chartboostMediationBannerAdViewRef.get()?.chartboostMediationBannerAdViewListener?.onAdImpressionRecorded(
                                    placement,
                                )
                                    ?: LogController.e(
                                        "The Chartboost Mediation SDK Banner listener is detached on onAdImpressionRecorded.",
                                    )
                            }
                            CoroutineScope(IO).launch {
                                ChartboostMediationNetworking.trackChartboostImpression(
                                    nextAd.auctionId,
                                    nextAd.loadId,
                                    nextAd.partnerAd?.partnerBannerSize?.type
                                        ?: if (bannerSize?.isAdaptive == true) AdFormat.ADAPTIVE_BANNER.key else AdFormat.BANNER.key,
                                )

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
                                        chartboostMediationBannerAd.width,
                                        chartboostMediationBannerAd.context,
                                    )
                                val containerHeight =
                                    Dips.pixelsToIntDips(
                                        chartboostMediationBannerAd.height,
                                        chartboostMediationBannerAd.context,
                                    )

                                if (creativeWidth <= containerWidth && creativeHeight <= containerHeight) {
                                    return@launch
                                }

                                val requestedWidth =
                                    nextAd.partnerAd
                                        ?.request
                                        ?.bannerSize
                                        ?.width ?: 0
                                val requestedHeight =
                                    nextAd.partnerAd
                                        ?.request
                                        ?.bannerSize
                                        ?.height ?: 0

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
                                    partnerAd.request.mediationPlacement,
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
        if (!isChartboostMediationBannerAdReadyToRefresh) {
            LogController.d("ChartboostMediationBannerAd is not on screen. Not refreshing.")
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

                cachedRequest?.let {
                    getNextAd(it)
                } ?: run {
                    LogController.e("Failed to refresh banner ad because `cachedRequest` is null.")
                }
            }
    }

    private fun sendShowMetricsData(
        startTime: Long,
        partnerName: String,
        auctionId: String,
        loadId: String,
    ) {
        val metrics = Metrics(partnerName, Endpoints.Event.SHOW)
        val showMetricsDataSet: MutableSet<Metrics> = HashSet()
        metrics.auctionId = auctionId
        metrics.start = startTime
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = true
        showMetricsDataSet.add(metrics)
        MetricsManager.postMetricsData(showMetricsDataSet, loadId)
    }

    /**
     * Constructs and returns an instance of [AdLoadParams] based on the provided parameters.
     * This function is intended to configure ad loading parameters specifically for Chartboost Mediation banner ads.
     *
     * @param request The [ChartboostMediationBannerAdLoadRequest] containing ad load request details such as placement and keywords.
     * @param loadId A unique identifier for the ad load operation.
     * @param chartboostMediationBannerAd A [ChartboostMediationBannerAdView] instance used to manage and display the ad.
     *
     * @return An [AdLoadParams] instance containing all necessary parameters to load a Chartboost Mediation banner ad.
     */
    private fun createAdLoadParams(
        request: ChartboostMediationBannerAdLoadRequest,
        loadId: String,
        chartboostMediationBannerAd: ChartboostMediationBannerAdView,
    ) = AdLoadParams(
        adIdentifier =
            AdIdentifier(
                chartboostMediationBannerAd.getAdType(),
                request.placement,
            ),
        keywords = request.keywords,
        loadId = loadId,
        bannerSize = request.size,
        adInteractionListener =
            object : AdInteractionListener {
                override fun onImpressionTracked(partnerAd: PartnerAd) {
                    // Chartboost Mediation ignores partner impression.
                }

                override fun onClicked(partnerAd: PartnerAd) {
                    chartboostMediationBannerAdViewRef.get()?.chartboostMediationBannerAdViewListener?.onAdClicked(
                        partnerAd.request.mediationPlacement,
                    )
                }

                override fun onRewarded(partnerAd: PartnerAd) {
                    // This should not happen
                    LogController.e("${partnerAd.request.mediationPlacement} Banner received rewarded callback?")
                }

                override fun onDismissed(
                    partnerAd: PartnerAd,
                    error: ChartboostMediationAdException?,
                ) {
                    // This should not happen
                    LogController.e("${partnerAd.request.mediationPlacement} Banner received dismissed callback?")
                }

                override fun onExpired(partnerAd: PartnerAd) {
                    // No action for now
                    LogController.e("${partnerAd.request.mediationPlacement} Banner received expired callback?")
                }
            },
        partnerSettings = chartboostMediationBannerAd.partnerSettings,
    )

    private fun createAdLoadResult(
        loadId: String,
        payloadJson: JSONObject,
        error: ChartboostMediationError?,
        winningBidInfo: Map<String, String> = mapOf(),
        placement: String,
        bannerSize: Size,
    ) = ChartboostMediationBannerAdLoadResult(
        loadId,
        payloadJson,
        winningBidInfo,
        error,
        placement,
        bannerSize,
    )

    private fun getError(loadResult: Result<CachedAd>?): ChartboostMediationError? {
        return when (loadResult) {
            null -> ChartboostMediationError.LoadError.ChartboostMediationNotInitialized

            else ->
                loadResult.fold({ null }, { throwable ->
                    if (throwable is ChartboostMediationAdException) {
                        throwable.chartboostMediationError
                    } else {
                        ChartboostMediationError.LoadError.Exception
                    }
                })
        }
    }

    private fun getCachedAd(loadResult: Result<CachedAd>?): CachedAd {
        return loadResult?.getOrNull() ?: throw ChartboostMediationAdException(
            ChartboostMediationError.LoadError.ChartboostMediationNotInitialized,
        )
    }

    private fun generateLoadId(): String {
        return "${ChartboostCore.analyticsEnvironment.appSessionIdentifier}${System.currentTimeMillis()}"
    }

    @OptIn(InternalSerializationApi::class)
    private fun createPayloadJson(metricsSet: MutableSet<Metrics> = mutableSetOf()): JSONObject {
        val metricsRequestBody = MetricsManager.buildMetricsDataRequestBody(metricsSet)
        return ChartboostMediationJson.writeJson(
            metricsRequestBody,
            MetricsRequestBody.serializer(),
        ).jsonObject.toJSONObject()
    }
}
