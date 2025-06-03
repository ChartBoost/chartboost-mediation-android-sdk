/*
 * Copyright 2024-2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.util.Size
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.PartnerAdapterInitializationResultsData
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.model.BannerAdDimensions
import com.chartboost.chartboostmediationsdk.network.model.BannerSizeBody
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.model.MetricsData
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.chartboostmediationsdk.utils.toJSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles metrics collection and reporting for the Mediation SDK.
 */
object MetricsManager {
    /**
     * Tracks all events in progress.
     */
    private val eventsInProgress = ConcurrentHashMap<String, MetricsEvent>()

    /**
     * Start tracking metrics for a new ad lifecycle event.
     *
     * @param eventType The type of event.
     * @param partner The partner name.
     *
     * @return The ID of the event in progress.
     */
    fun start(
        eventType: MetricsEvent.EventType,
        partner: String,
    ): String {
        val id = UUID.randomUUID().toString()
        eventsInProgress[id] = MetricsEvent(eventType, partner)

        return id
    }

    /**
     * End tracking metrics for an ad lifecycle event.
     *
     * @param id The ID of the event in progress.
     * @param isSuccess Whether the event completed successfully.
     * @param error The Chartboost Mediation error, if any.
     */
    fun end(
        id: String,
        isSuccess: Boolean,
        error: ChartboostMediationError? = null,
    ) {
        eventsInProgress[id]?.apply {
            endTimestamp = System.currentTimeMillis()
            this.isSuccess = isSuccess
            this.error = error

            // TODO: Post metrics data. Leaving it unimplemented for now since we're still using the old API below.

            eventsInProgress.remove(id)
        }
    }

    /**
     * Get the event in progress for the given ID.
     *
     * @param id The ID of the event in progress.
     *
     * @return The event in progress for the given ID.
     */
    fun getEventInProgress(id: String) = eventsInProgress[id]

    /**
     * Post metrics data payload to the server on an ad lifecycle event basis.
     *
     * This function sends tracking data related to ad lifecycle events to the server.
     * It handles both global and ad-specific event trackers. Global trackers are always
     * processed, even if `adEventTrackers` is empty. This ensures that essential tracking data is sent,
     * regardless of whether ad-specific trackers are present.
     * Ad-specific trackers, if provided, are processed in addition to the global ones.
     *
     * @param data The metrics data payload for a specific ad lifecycle event.
     * @param loadId The load ID for the ad lifecycle event.
     * @param loadStart The start time, in milliseconds, of a load.
     * @param backgroundDurationMs The amount of time, in milliseconds, that a load was being performed while the app was in the background.
     * @param eventResult The result of the ad lifecycle event.
     * @param adEventTrackers A map of event trackers specific to this ad. This map may be empty, in which case only global event trackers will be processed.
     */
    @OptIn(InternalSerializationApi::class)
    fun postMetricsData(
        data: Set<Metrics>,
        loadId: String? = null,
        queueId: String? = null,
        loadStart: Long? = null,
        backgroundDurationMs: Long? = null,
        eventResult: EventResult? = null,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>> = emptyMap(),
    ) {
        // No need to send empty/invalid/corrupted data since it's not going to be useful.
        // This is not the same as checking for partial data, which is actually valid and which could
        // manifest when an ad lifecycle event fails to complete.
        if (!metricsDataIsValid(data)) {
            return
        }

        // `loadEnd` and `loadDuration` only applicable if `loadStart` is supplied.
        var loadEnd: Long? = null
        var loadDuration: Long? = null
        loadStart?.let {
            val endMs = System.currentTimeMillis()
            loadEnd = endMs
            loadDuration = endMs - it
        }

        // Build the payload early so we can also log it before sending it to the server.
        // This ensures what's logged is what's sent.
        val metricsRequestBody =
            buildMetricsDataRequestBody(data, queueId, loadStart, loadEnd, loadDuration, backgroundDurationMs, eventResult)
        val event = data.first().event

        val payload =
            ChartboostMediationJson
                .writeJson(
                    metricsRequestBody,
                    MetricsRequestBody.serializer(),
                ).jsonObject
                .toJSONObject()

        LogController.d("Metrics data for the $event lifecycle event: $payload")

        // Post the payload onto the callback for the public API.
        when (event) {
            TrackingEvent.INITIALIZATION ->
                CoroutineScope(Dispatchers.Main).launch {
                    ChartboostMediationSdk.chartboostMediationInternal.partnerAdapterInitializationResults.onResultsReceived(
                        PartnerAdapterInitializationResultsData(payload),
                    )
                }

            else -> {
                // NO-OP for now. Other lifecycle events will be added in the future.
            }
        }

        dispatch(
            event = event,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackEvent(
                url,
                loadId = loadId,
                queueId = queueId,
                metricsRequestBody,
            )
        }
    }

    /**
     * Prepare and post a default metrics payload for event failures to the server.
     * Note: This is not the same as sending metrics data for a failed partner, but rather a failure
     * preventing the event itself from completing (e.g. adapter not found, or no network connection).
     *
     * Typically, these failures would be caused by client-side internal issues. The partner might even
     * succeed if the event does complete.
     *
     * In cases like this, the payload might not be conformant to pre-defined schemas, as long as it
     * indicates there's an issue.
     *
     * It handles both global and ad-specific event trackers. Global trackers are always
     * processed, even if `adEventTrackers` is empty. This ensures that essential tracking data is sent,
     * regardless of whether ad-specific trackers are present.
     * Ad-specific trackers, if provided, are processed in addition to the global ones.
     *
     * @param partner The partner for whom the event failed.
     * @param event The ad lifecycle event that failed.
     * @param auctionIdentifier The auction ID.
     * @param chartboostMediationError The Chartboost Mediation error.
     * @param chartboostMediationErrorMessage The Chartboost Mediation error message.
     * @param placementType The placement type.
     * @param size The ad size.
     * @param loadStart The start time, in milliseconds, of a load.
     * @param backgroundDuration The amount of time, in milliseconds, that a load was being performed while the app was in the background.
     * @param loadId The load ID.
     * @param eventResult The result of the ad lifecycle event.
     * @param networkType The network type (i.e. "bidding" or "mediation").
     * @param lineItemId The line item ID.
     * @param partnerPlacement The partner placement.
     * @param adEventTrackers A map of event trackers specific to this ad. This map may be empty, in which case only global event trackers will be processed.
     */
    fun postMetricsDataForFailedEvent(
        partner: String?,
        event: TrackingEvent,
        auctionIdentifier: String?,
        chartboostMediationError: ChartboostMediationError,
        chartboostMediationErrorMessage: String?,
        placementType: String? = null,
        size: Size? = null,
        loadStart: Long? = null,
        backgroundDuration: Long? = null,
        loadId: String? = null,
        eventResult: EventResult? = null,
        networkType: String? = null,
        lineItemId: String? = null,
        partnerPlacement: String? = null,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>> = emptyMap(),
    ) {
        postMetricsData(
            data =
                setOf(
                    Metrics(partner, event).apply {
                        start = System.currentTimeMillis()
                        end = System.currentTimeMillis()
                        duration = 0
                        auctionId = auctionIdentifier
                        this.placementType = placementType
                        this.networkType = networkType
                        this.lineItemId = lineItemId
                        this.partnerPlacement = partnerPlacement
                        this.size = size
                        isSuccess = false
                        this.chartboostMediationError = chartboostMediationError
                        this.chartboostMediationErrorMessage = chartboostMediationErrorMessage
                    },
                ),
            loadId = loadId,
            loadStart = loadStart,
            backgroundDurationMs = backgroundDuration,
            eventResult = eventResult,
            adEventTrackers = adEventTrackers,
        )
    }

    /**
     * Tracks a Chartboost impression event.
     *
     * This function dispatches a Chartboost impression event to the appropriate trackers, sending the impression data to the server.
     * This includes both the ad-specific trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param bids The bid information associated with the impression.
     * @param loadId The load ID for the impression event.
     * @param adType The type of ad that was displayed.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackChartboostImpression(
        bids: Bids,
        loadId: String,
        adType: String,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.HELIUM_IMPRESSION,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackChartboostImpression(
                url = url,
                bids = bids,
                loadId = loadId,
                adType = adType,
            )
        }
    }

    /**
     * Tracks a partner impression event.
     *
     * This function dispatches a partner impression event to the appropriate trackers, sending the impression data to the server.
     * This includes both the ad-specific trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param appSetId The app set ID associated with the impression.
     * @param auctionID The auction ID associated with the impression, if available.
     * @param loadId The load ID for the impression event.
     * @param adType The type of ad that was displayed.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackPartnerImpression(
        appSetId: String,
        auctionID: String?,
        loadId: String,
        adType: String,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.PARTNER_IMPRESSION,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackPartnerImpression(
                url = url,
                appSetId = appSetId,
                auctionID = auctionID,
                loadId = loadId,
                adType = adType,
            )
        }
    }

    /**
     * Tracks a click event.
     *
     * This function dispatches a click event to the appropriate trackers,
     * sending the data to the server. This includes both the ad-specific
     * trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param auctionId The auction ID associated with the impression, if available.
     * @param loadId The load ID for the impression event.
     * @param adType The type of ad that was displayed.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackClick(
        auctionId: String,
        loadId: String,
        adType: String,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.CLICK,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackClick(
                url = url,
                auctionId = auctionId,
                loadId = loadId,
                adType = adType,
            )
        }
    }

    /**
     * Tracks a reward event.
     *
     * This function dispatches a reward event to the appropriate trackers, sending the data to the server.
     * This includes both the ad-specific trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param auctionId The auction ID associated with the impression, if available.
     * @param loadId The load ID for the impression event.
     * @param adType The type of ad that was displayed.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackReward(
        auctionId: String,
        loadId: String,
        adType: String,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.REWARD,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackReward(
                url = url,
                auctionId = auctionId,
                loadId = loadId,
                adType = adType,
            )
        }
    }

    /**
     * Tracks an adaptive banner size event.
     *
     * This function dispatches an adaptive banner size event to the appropriate trackers, sending the banner size data to the server.
     * This includes both the ad-specific trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param loadId The load ID for the banner size event, if available.
     * @param bannerSizeBody The [BannerSizeBody] containing the width and height of the banner.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackAdaptiveBannerSize(
        loadId: String?,
        bannerSizeBody: BannerSizeBody,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.BANNER_SIZE,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackAdaptiveBannerSize(
                url = url,
                loadId = loadId,
                bannerSizeBody = bannerSizeBody,
            )
        }
    }

    /**
     * Tracks an auction winner event.
     *
     * This function dispatches an auction winner event to the appropriate trackers, sending the winning bid data to the server.
     * This includes both the ad-specific trackers provided in `adEventTrackers` and any globally configured trackers.
     *
     * @param bids The bid information associated with the winning auction.
     * @param loadId The load ID for the auction winner event.
     * @param adType The type of ad that won the auction.
     * @param adEventTrackers A map of event trackers specific to this ad. Global trackers
     *                        are always processed, even if this map is empty.
     */
    fun trackAuctionWinner(
        bids: Bids,
        loadId: String,
        adType: String,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ) {
        dispatch(
            event = TrackingEvent.WINNER,
            adEventTrackers = adEventTrackers,
        ) { url ->
            ChartboostMediationNetworking.trackAuctionWinner(
                url = url,
                bids = bids,
                loadId = loadId,
                adType = adType,
            )
        }
    }

    /**
     * Tracks a queue event.
     *
     * This function dispatches a queue event to the appropriate trackers, sending the queue data to the server.
     * Global trackers are always processed for queue events.
     *
     * @param event The [TrackingEvent] representing the specific queue event.
     * @param placement The placement associated with the queue event.
     * @param queueCapacity The capacity of the queue.
     * @param actualMaxQueueSize The actual maximum size of the queue.
     * @param queueDepth The current depth of the queue.
     * @param queueId The ID of the queue.
     * @param adType The type of ad associated with the queue event.
     */
    fun trackQueueEvent(
        event: TrackingEvent,
        placement: String,
        queueCapacity: Int,
        actualMaxQueueSize: Int? = null,
        queueDepth: Int,
        queueId: String,
        adType: String,
    ) {
        dispatch(
            event = event,
            adEventTrackers = emptyMap(),
        ) { url ->
            ChartboostMediationNetworking.trackQueueEvent(
                url = url,
                placement = placement,
                queueCapacity = queueCapacity,
                actualMaxQueueSize = actualMaxQueueSize,
                queueDepth = queueDepth,
                queueId = queueId,
                adType = adType,
            )
        }
    }

    /**
     * Validate the raw metrics data set before sending it to the server. Note that this does not
     * validate partial data, which is considered valid and which could manifest when an ad lifecycle
     * event fails to complete.
     *
     * @param data The metrics data set to validate.
     *
     * @return True if the entire data set is valid, false otherwise.
     */
    private fun metricsDataIsValid(data: Set<Metrics>): Boolean {
        val errorPrefix = "Failed to post metrics data to the server"

        return when {
            data.isEmpty() -> {
                LogController.d("$errorPrefix. Data set is empty.")
                false
            }

            !metricsDataBelongsToSameEvent(data) -> {
                LogController.d("$errorPrefix. Data set contains metrics data for multiple events.")
                false
            }

            else -> {
                true
            }
        }
    }

    /**
     * Build the metrics data payload for the current ad lifecycle event. Note that this is the payload
     * sent to the server. The payload for the public API may be transformed.
     *
     * @param data The metrics data set
     * @param backgroundDuration The amount of time, in milliseconds, that a load was being performed while the app was in the background.
     * @param eventResult The result of the ad lifecycle event.
     *
     * @return A JSONObject containing the finalized payload.
     */
    internal fun buildMetricsDataRequestBody(
        data: Set<Metrics>,
        queueId: String? = "",
        start: Long? = null,
        end: Long? = null,
        duration: Long? = null,
        backgroundDuration: Long? = null,
        eventResult: EventResult? = null,
    ): MetricsRequestBody =
        when (eventResult) {
            // SdkInitializationResults
            is EventResult.SdkInitializationResult.InitResult1A -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet(),
                )
            }

            is EventResult.SdkInitializationResult.InitResult2A -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet(),
                )
            }

            is EventResult.SdkInitializationResult.InitResult1B -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = emptySet(),
                    error = eventResult.jsonParseError,
                )
            }

            is EventResult.SdkInitializationResult.InitResult2B -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    error = eventResult.jsonParseError,
                )
            }

            is EventResult.SdkInitializationResult.InitResult1C -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = emptySet(),
                )
            }

            is EventResult.SdkInitializationResult.InitResult2C -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = emptySet(),
                )
            }

            // AdLoadResults
            is EventResult.AdLoadResult.AdLoadSuccess -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    queueId = queueId,
                    placementType = data.firstOrNull()?.placementType,
                    size = getBannerAdDimensions(data),
                    start = start,
                    end = end,
                    duration = duration,
                    backgroundDurationMs = backgroundDuration,
                    metrics = data.map { MetricsData(it) }.toSet(),
                )
            }

            is EventResult.AdLoadResult.AdLoadJsonFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    placementType = data.firstOrNull()?.placementType,
                    size = getBannerAdDimensions(data),
                    start = start,
                    end = end,
                    duration = duration,
                    backgroundDurationMs = backgroundDuration,
                    metrics = emptySet(),
                    error = eventResult.jsonParseError,
                )
            }

            is EventResult.AdLoadResult.AdLoadPartnerFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    placementType = data.firstOrNull()?.placementType,
                    size = getBannerAdDimensions(data),
                    start = start,
                    end = end,
                    duration = duration,
                    backgroundDurationMs = backgroundDuration,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    error = eventResult.metricsError,
                )
            }

            is EventResult.AdLoadResult.AdLoadUnspecifiedFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    placementType = data.firstOrNull()?.placementType,
                    size = getBannerAdDimensions(data),
                    start = start,
                    end = end,
                    duration = duration,
                    backgroundDurationMs = backgroundDuration,
                    metrics = emptySet(),
                    error = eventResult.metricsError,
                )
            }

            else -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    placementType = data.firstOrNull()?.placementType,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    size = getBannerAdDimensions(data),
                )
            }
        }

    /**
     * Check if metrics data in the given data set belongs to the same [TrackingEvent].
     *
     * @param data The metrics data to check.
     *
     * @return True if all metrics data in the given data set belongs to the same [TrackingEvent], false otherwise.
     */
    private fun metricsDataBelongsToSameEvent(data: Set<Metrics>): Boolean = data.all { it.event == data.firstOrNull()?.event }

    /**
     * Checks if metrics data should be posted for the given tracking event.
     *
     * @param event The [TrackingEvent] to check.
     * @param adEventTrackers The ad-specific event trackers.
     * @return `true` if metrics data should be posted, `false` otherwise.
     */
    private fun shouldPostMetricsData(
        event: TrackingEvent,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ): Boolean {
        val isGlobalEvent = AppConfigStorage.globalEventTrackers.containsKey(event)
        val isAdEvent = adEventTrackers.containsKey(event)
        return isGlobalEvent || isAdEvent
    }

    /**
     * Get the banner ad dimensions from the given metrics data set.
     *
     * @param data The metrics data set.
     *
     * @return The [BannerAdDimensions] from the given metrics data set.
     */
    private fun getBannerAdDimensions(data: Set<Metrics>) =
        data.firstOrNull()?.size?.let {
            BannerAdDimensions(it)
        }

    /**
     * Get all the trackers for specific event - global and ad-specific
     *
     * @param event Specific event to send
     * @param adEventTrackers Ad-specific trackers
     *
     * @return List<ServerEventTracker> all trackers for specific event
     */
    private fun getAllTrackersForEvent(
        event: TrackingEvent,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ): List<ServerEventTracker> {
        val globalTrackers = AppConfigStorage.globalEventTrackers[event] ?: emptyList()
        val adTrackers = adEventTrackers[event] ?: emptyList()

        return globalTrackers.plus(adTrackers).filter { it.url.isNotBlank() }
    }

    /**
     * Dispatches tracking events to their respective trackers, sending data to the server if necessary.
     *
     * This function processes a given[TrackingEvent] and its associated trackers. It first checks if
     * the event should be tracked based on the provided `adEventTrackers` and global tracking
     * configurations. If tracking is enabled, it prepares the list of trackers to be executed.
     *
     * For each tracker, it asynchronously sends the tracking data to the server using the provided
     * `sendAction` function. The result of each send operation is then logged, indicating success or
     * failure.
     *
     * @param event The [TrackingEvent] to be dispatched. This represents the specific event that
     *              occurred (e.g., impression, click, reward).
     * @param adEventTrackers A map of [TrackingEvent]s to lists of [ServerEventTracker]s. This map
     *                        contains trackers specific to the current ad. Global trackers are also
     *                        considered and processed in addition to these. If this map is empty,
     *                        only global trackers will be used.
     * @param sendAction A suspend function that performs the actual sending of tracking data to a
     *                   given URL. This function should handle the network request and return a
     *                   [ChartboostMediationNetworkingResult] indicating success or failure.
     */
    private fun dispatch(
        event: TrackingEvent,
        adEventTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
        sendAction: suspend (url: String) -> ChartboostMediationNetworkingResult<Unit?>,
    ) {
        if (!shouldPostMetricsData(event, adEventTrackers)) {
            return
        }

        getAllTrackersForEvent(event, adEventTrackers).forEach { eventTracker ->
            CoroutineScope(Dispatchers.IO).launch {
                when (val result = sendAction(eventTracker.url)) {
                    is ChartboostMediationNetworkingResult.Success ->
                        LogController.i("Successfully posted data for the $event lifecycle event to ${eventTracker.url}")

                    is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                        LogController.e("Failed to post data for the $event lifecycle event to ${eventTracker.url}: ${result.error}")
                    }

                    is ChartboostMediationNetworkingResult.Failure ->
                        LogController.e("Failed to post data for the $event lifecycle event to ${eventTracker.url}: ${result.error}")
                }
            }
        }
    }
}
