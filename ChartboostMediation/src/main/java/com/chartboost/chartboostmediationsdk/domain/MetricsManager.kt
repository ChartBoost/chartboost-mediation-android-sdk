/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.util.Size
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.PartnerAdapterInitializationResultsData
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.network.model.BannerAdDimensions
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
     * @param data The metrics data payload for a specific ad lifecycle event.
     * @param loadId The load ID for the ad lifecycle event.
     * @param loadStart The start time, in milliseconds, of a load.
     * @param backgroundDurationMs The amount of time, in milliseconds, that a load was being performed while the app was in the background.
     * @param eventResult The result of the ad lifecycle event.
     */
    @OptIn(InternalSerializationApi::class)
    fun postMetricsData(
        data: Set<Metrics>,
        loadId: String? = null,
        queueId: String? = null,
        loadStart: Long? = null,
        backgroundDurationMs: Long? = null,
        eventResult: EventResult? = null,
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
            Endpoints.Event.INITIALIZATION ->
                CoroutineScope(Dispatchers.Main).launch {
                    ChartboostMediationSdk.chartboostMediationInternal.partnerAdapterInitializationResults.onResultsReceived(
                        PartnerAdapterInitializationResultsData(payload),
                    )
                }

            else -> {
                // NO-OP for now. Other lifecycle events will be added in the future.
            }
        }

        if (!shouldPostMetricsData(event)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result =
                ChartboostMediationNetworking.trackEvent(
                    event,
                    loadId = loadId,
                    queueId = queueId,
                    metricsRequestBody,
                )
            CoroutineScope(Dispatchers.Main).launch {
                when (result) {
                    is ChartboostMediationNetworkingResult.Success ->
                        LogController.i("Successfully posted metrics data for the $event lifecycle event")

                    is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                        LogController.e("Failed to post metrics data for the $event lifecycle event: ${result.error}")
                    }

                    is ChartboostMediationNetworkingResult.Failure ->
                        LogController.e("Failed to post metrics data for the $event lifecycle event: ${result.error}")
                }
            }
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
     */
    fun postMetricsDataForFailedEvent(
        partner: String?,
        event: Endpoints.Event,
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
        )
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
     * Check if metrics data in the given data set belongs to the same [Metrics.Event].
     *
     * @param data The metrics data to check.
     *
     * @return True if all metrics data in the given data set belongs to the same [Metrics.Event], false otherwise.
     */
    private fun metricsDataBelongsToSameEvent(data: Set<Metrics>): Boolean = data.all { it.event == data.firstOrNull()?.event }

    /**
     * Check whether we should post metrics data for this event to the server using the following logic:
     *
     * If [AppConfigStorage.metricsEvents] is null (i.e. omitted) --> Collect metrics data for all pre-defined ad lifecycle events
     * If [AppConfigStorage.metricsEvents] is non-null with a subset of pre-defined lifecycle events --> Collect metrics for the indicated events
     *
     * @param event The [Metrics] to check.
     *
     * @return true if we should post metrics data for this event to the server, false otherwise
     */
    private fun shouldPostMetricsData(event: Endpoints.Event): Boolean = AppConfigStorage.metricsEvents.contains(event)

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
}
