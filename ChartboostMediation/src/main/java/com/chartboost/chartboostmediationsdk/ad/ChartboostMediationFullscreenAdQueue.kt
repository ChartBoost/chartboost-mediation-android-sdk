/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.content.Context
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.max

/**
 * The fullscreen ad queue class. This class is responsible for loading multiple
 * [ChartboostMediationFullscreenAd] ads and storing them to a queue for subsequent showing.
 * Ads that have been stored for a period of [queuedAdTtlMs] are expired and removed from the queue.
 *
 * The queue will begin to load ads upon `start()` and continue to fill until it reaches the [queueCapacity].
 * When an ad is removed from the queue, a new load will be attempted.
 *
 * The queue will stop to load ads upon `stop()`. However, ads already stored will persist until they
 * are either retrieved from the queue or have been expired automatically.
 *
 * @param context [Context] The current context.
 * @param placement [String] The corresponding Chartboost Mediation placement.
 */
class ChartboostMediationFullscreenAdQueue(
    val context: Context,
    val placement: String,
) {
    internal companion object {
        /**
         * The minimum allowed queue size.
         * Not currently server-side configurable as of this moment.
         */
        private const val MINIMUM_QUEUE_SIZE = 1

        /**
         * A log prefix used when logging fullscreen ad queue messages.
         */
        private const val LOG_PREFIX = "The fullscreen ad queue for:"
    }

    /**
     * A listener to notify this queue whether it should
     * automatically start queuing if the SDK is initialized or not.
     */
    internal val notifyQueueToAutoStart: ChartboostMediationFullscreenAdQueueNotifyOnAutoQueue =
        object :
            ChartboostMediationFullscreenAdQueueNotifyOnAutoQueue {
            override fun onSdkInitAutoQueue(isSdkInitialized: Boolean) {
                if (isSdkInitialized) {
                    // This resets the originally set capacity before init, which is at 1
                    // when a placement is not found during init.
                    // Since we are auto-starting, we need to overwrite this to the proper one.
                    queueCapacity =
                        PlacementStorage.getQueueSize(placement)

                    start()
                } else {
                    // An issue was found during the initialization. Stop the queue.
                    LogController.w(
                        "Cannot auto start queuing due to the Chartboost" +
                            " Mediation SDK failing to initialize. Stopping the queue.",
                    )
                    stop()
                }
            }
        }

    /**
     * A [ChartboostMediationFullscreenAdQueueAdExpiredListener] to keep track of ad expiration and remove
     * an ad from the queue.
     */
    private val fullscreenAdQueueAdExpiredListener =
        object : ChartboostMediationFullscreenAdQueueAdExpiredListener {
            /**
             * Expires a [ChartboostMediationFullscreenAd] ad, removes it from the queue,
             * and notifies the [ChartboostMediationFullscreenAdQueueListener].
             */
            override fun onAdExpired(ad: ChartboostMediationFullscreenAd) {
                // Post metrics data regarding expiration.
                MetricsManager.postMetricsData(
                    setOf(
                        Metrics(
                            partner =
                                ad.cachedAd
                                    ?.partnerAd
                                    ?.request
                                    ?.partnerId,
                            event = Endpoints.Event.EXPIRATION,
                        ).apply {
                            auctionId = ad.cachedAd?.bids?.auctionId
                            // As expiration events are not technically errors.
                            isSuccess = true
                        },
                    ),
                )

                // Log that an ad has expired.
                LogController.d(
                    "$LOG_PREFIX ${ad.request.placement} has an expired ad. Will be" +
                        " removed from the queue.",
                )

                // Invalidate the ad
                ad.invalidate()

                // Remove this ad from the queue
                fullscreenAdsQueued.remove(ad)

                // Notify an ad has been removed.
                CoroutineScope(Main).launch {
                    adQueueListener?.onFullscreenAdQueueExpiredAdRemoved(
                        this@ChartboostMediationFullscreenAdQueue,
                        numberOfAdsReady,
                    ) ?: run {
                        LogController.w(
                            "Unable to fire onFullscreenAdQueueExpiredAdRemoved() " +
                                "because $LOG_PREFIX $placement has a null listener.",
                        )
                    }
                }

                // Start a new ad fetch job.
                // The job already checks if there's already an existing fetch.
                startAdFetchJob()
            }
        }

    /**
     * A list of successfully loaded [ChartboostMediationFullscreenAd] ads.
     */
    private val fullscreenAdsQueued = mutableListOf<ChartboostMediationFullscreenAd>()

    /**
     * The time to live (TTL) value of a queued ad in milliseconds.
     * This value is configurable via the [AppConfigStorage].
     */
    private var queuedAdTtlMs: Long = AppConfigStorage.queueAdTtlSeconds * 1000L

    /**
     * The maximum queue size.
     * A server-side configurable value from the [PlacementStorage].
     */
    private var maxQueueSize: Int = AppConfigStorage.maxQueueSize

    /**
     * A [Job] representing when an ad is being loaded and added to the queue.
     */
    private var fetchAdJob: Job? = null

    /**
     * A unique queue identifier that is updated on every queue start.
     */
    private var queueId = ""

    /**
     * The delay rate of an ad fetch.
     * Based on the maximum between the fullscreen load timeout & the load rate limit.
     * May one day have its own dedicated time.
     */
    private val delayFetchRateMs: Long
        get() =
            1000L *
                max(
                    AppConfigStorage.fullscreenLoadTimeoutSeconds,
                    LoadRateLimiter().getLoadRateLimitSeconds(placement),
                )

    /**
     * Determine whether there already exists a current ad fetch job in progress.
     */
    private val isQueueFetchInProgress
        get() = fetchAdJob != null

    /**
     * The [ChartboostMediationFullscreenAdQueueListener] to notify of fullscreen ad queue events.
     */
    var adQueueListener: ChartboostMediationFullscreenAdQueueListener? = null
        get() {
            if (field == null) {
                LogController.w("Ad queue listener is null on FullscreenAdQueueListener")
            }
            return field
        }

    /**
     * A property that gets and sets the ad queue capacity.
     */
    var queueCapacity: Int = PlacementStorage.getQueueSize(placement)
        set(value) {
            // For a rare edge case in which a max size is outside of the expected range.
            val safeMaxQueueSize = maxQueueSize.coerceAtLeast(MINIMUM_QUEUE_SIZE)
            field =
                value.coerceIn(MINIMUM_QUEUE_SIZE, safeMaxQueueSize).also {
                    if (value != it) {
                        LogController.e("Queue capacity adjusted to be within valid range: $it")
                    }
                    PlacementStorage.addQueueSize(placement, it, safeMaxQueueSize)
                }
        }

    /**
     * Property to get and set [Keywords] from the ad queue.
     */
    var keywords: Keywords = Keywords()

    /**
     * Publisher-specified map of String to Any that is sent to all the partners.
     */
    val partnerSettings: MutableMap<String, Any> = mutableMapOf()

    /**
     * Whether or not the ad queue is currently running (active) and automatically queueing ads.
     */
    var isRunning: Boolean = false
        private set

    /**
     * Whether or not the ad queue is currently paused until the Chartboost Mediation SDK initializes.
     * It is internal so that we can use it for tests.
     */
    internal var isPaused: Boolean = false

    /**
     * A property that gets the number of ready ads currently in the ad queue.
     */
    val numberOfAdsReady: Int
        get() = fullscreenAdsQueued.size

    /**
     * Gets the next ad from the queue.
     *
     * @return A [ChartboostMediationFullscreenAd] ad.
     */
    fun getNextAd(): ChartboostMediationFullscreenAd? {
        val ad = fullscreenAdsQueued.removeFirstOrNull()

        // Start another ad fetch request.
        startAdFetchJob()

        // Remove the attached listener as the ad will not be automatically expired from this point.
        ad?.listener = null

        // Return ad.
        return ad
    }

    /**
     * Checks if there's an ad available.
     *
     * @return Whether or not there exists and ad in the queue.
     */
    fun hasNextAd(): Boolean = fullscreenAdsQueued.isNotEmpty()

    /**
     * Starts loading ads and append them to the queue automatically until capacity has been reached.
     */
    fun start() {
        if (ChartboostMediationSdk.chartboostMediationInternal.initializationStatus !=
            ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED
        ) {
            LogController.d(
                "$LOG_PREFIX $placement the Chartboost Mediation SDK has not initialized." +
                    " Cannot start the queue. Waiting until the Chartboost Mediation SDK has started before queuing.",
            )
            // The queue is technically running, but it is paused until after the SDK has started.
            isRunning = true
            isPaused = true
            return
        }

        if (isRunning && !isPaused) {
            LogController.d("$LOG_PREFIX $placement is already running")
            return
        }

        // Start queueing and log metrics.
        isRunning = true

        // The queue at this point no longer needs to be paused.
        isPaused = false

        // New queue identifiers are created on every queue start.
        queueId = "${ChartboostCore.analyticsEnvironment.appSessionIdentifier}${System.currentTimeMillis()}"

        CoroutineScope(IO).launch {
            ChartboostMediationNetworking.makeQueueRequest(
                isRunning = isRunning,
                placement = placement,
                queueCapacity = queueCapacity,
                actualMaxQueueSize = maxQueueSize,
                queueDepth = numberOfAdsReady,
                queueId = queueId,
                adType =
                    fullscreenAdsQueued
                        .firstOrNull()
                        ?.cachedAd
                        ?.partnerAd
                        ?.request
                        ?.format ?: "",
            )
        }

        // Start a fetch ad job.
        startAdFetchJob()
    }

    /**
     * Stops loading ads.
     *
     * When the user stops a queue during filling, we give it a chance to queue the next ad and no more afterwards.
     * This better reflects the number of auction requests made versus the number of ads ready in
     * the queue depth, for queue metrics.
     */
    fun stop() {
        val adType =
            fullscreenAdsQueued
                .firstOrNull()
                ?.cachedAd
                ?.partnerAd
                ?.request
                ?.format ?: ""

        /*
         * Apply an invokeOnCompletion handler to the current job. This will send an end_queue request at the end of
         * this fetch ad job and will correctly tell us the queue depth.
         */
        fetchAdJob?.apply {
            invokeOnCompletion {
                if (!isRunning) {
                    sendEndQueueRequest(adType)
                }
            }
        } ?: run {
            /*
             * Send an end_queue request even if there's no current job.
             * Handles the case where a queue is already full and no fetch job is created.
             * Only send the request when a queue is running.
             */
            if (isRunning) {
                sendEndQueueRequest(adType)
            }
        }
        isRunning = false
        isPaused = false
    }

    /**
     * Starts a queuing Job.
     *
     * @param isDelayed [Boolean] Whether or not the next fetch job is delayed (optional & false as default).
     */
    private fun startAdFetchJob(isDelayed: Boolean = false) {
        if (!isRunning) {
            LogController.d("$LOG_PREFIX $placement is no longer running. Waiting for the queue to start running.")
            return
        }

        if (fullscreenAdsQueued.size >= queueCapacity) {
            LogController.d("$LOG_PREFIX $placement is already full. Waiting until next queue fetch job.")
            return
        }

        if (isQueueFetchInProgress) {
            LogController.d("$LOG_PREFIX $placement already has a queue fetch job in progress.")
            return
        }

        fetchAdJob =
            CoroutineScope(IO).launch(
                CoroutineExceptionHandler { _, error ->
                    handleAdFetchLoadResultException(error)
                },
            ) {
                // Delay this fetch in case a previous fetch failed to queue an ad.
                if (isDelayed) delay(delayFetchRateMs)

                if (fullscreenAdsQueued.size >= queueCapacity) {
                    LogController.d("$LOG_PREFIX $placement already reached capacity. Waiting until the next queue job.")
                    fetchAdJob = null
                    return@launch
                }

                val loadResult = loadFullscreenAd(context)

                fetchAdJob = null
                if (!isActive) {
                    // Invalidate the ad
                    loadResult.ad?.invalidate()
                    LogController.w(
                        "$LOG_PREFIX $placement already has an active queue fetch job. Discarding previously loaded ad.",
                    )
                    return@launch
                }

                loadResult.ad?.let { ad ->
                    // Add the loaded ad to our queue.
                    fullscreenAdsQueued.add(ad)
                    // A successful ad was loaded. Next load should not be delayed.
                    CoroutineScope(Main).launch {
                        delay(queuedAdTtlMs)
                        ad.listener?.onAdExpired(ad) ?: run {
                            LogController.w("$LOG_PREFIX $placement has ad listener is null. Cannot expire ad.")
                        }
                    }
                } ?: run {
                    handleAdFetchLoadResultNoAd(loadResult)
                }

                // Notify the listener that the queue has been updated.
                CoroutineScope(Main.immediate).launch {
                    adQueueListener?.onFullScreenAdQueueUpdated(
                        this@ChartboostMediationFullscreenAdQueue,
                        loadResult,
                        numberOfAdsReady,
                    ) ?: run {
                        LogController.w("$LOG_PREFIX $placement has its FullscreenAdQueueAdExpiredListener null.")
                    }
                }

                // Check if the next load fetch should be delayed based on the resent load result.
                val isNextLoadFetchDelayed = loadResult.ad == null

                // Start another ad fetch job.
                startAdFetchJob(isDelayed = isNextLoadFetchDelayed)
            }
    }

    /**
     * Load a fullscreen ad.
     *
     * @param context [Context] The current context.
     *
     * @return A [ChartboostMediationFullscreenAdLoadResult].
     */
    private suspend fun loadFullscreenAd(context: Context) =
        ChartboostMediationFullscreenAd.loadFullscreenAd(
            context = context,
            request = ChartboostMediationFullscreenAdLoadRequest(placement, keywords),
            queueId = queueId,
            adController = ChartboostMediationSdk.chartboostMediationInternal.adController,
            listener = fullscreenAdQueueAdExpiredListener,
        )

    /**
     * Handles an ad fetch load result when there is no ad to queue.
     *
     * @param loadResult [ChartboostMediationFullscreenAdLoadResult].
     */
    private fun handleAdFetchLoadResultNoAd(loadResult: ChartboostMediationFullscreenAdLoadResult) {
        LogController.w(
            "$LOG_PREFIX $placement failed to load ad. " +
                "The next fetch request has been delayed for ${delayFetchRateMs / 1000} " +
                "seconds to prevent unnecessary requests from being made.",
        )
        LogController.i(
            "$LOG_PREFIX $placement failed to queue ad with loadId: ${loadResult.loadId} and " +
                "error: ${loadResult.error?.toString() ?: ""}",
        )
    }

    /**
     * Handles an ad fetch load result exception.
     *
     * @param error [Throwable] The throwable error that was captured.
     */
    private fun handleAdFetchLoadResultException(error: Throwable) {
        LogController.e(
            if (error is ChartboostMediationAdException) {
                error.message
            } else {
                ChartboostMediationError.OtherError.InternalError.message
            },
        )
        fetchAdJob = null
        LogController.w(
            "$LOG_PREFIX $placement encountered an error during ad fetch. " +
                "The next fetch request has been delayed for ${delayFetchRateMs / 1000} " +
                "seconds to prevent unnecessary requests from being made.",
        )
        // Start another ad fetch job.
        startAdFetchJob(isDelayed = true)
    }

    /**
     * Send an end_queue request.
     */
    private fun sendEndQueueRequest(adType: String?) =
        CoroutineScope(IO).launch {
            ChartboostMediationNetworking.makeQueueRequest(
                false,
                placement = placement,
                queueCapacity = queueCapacity,
                queueDepth = numberOfAdsReady,
                queueId = queueId,
                adType = adType ?: "",
            )
        }
}
