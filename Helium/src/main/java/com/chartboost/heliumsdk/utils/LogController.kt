/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.util.Log
import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.PartnerInitializationResultsData
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking
import com.chartboost.heliumsdk.network.Endpoints.Sdk
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.model.MetricsData
import com.chartboost.heliumsdk.network.model.MetricsRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.jsonObject

/**
 * @suppress
 *
 * Logging system for the Helium SDK that handles both client-side logging and metrics data reporting.
 */
object LogController {
    /**
     * A data class containing the class and method name of the caller on the call stack.
     */
    data class StackTraceElements(val className: String, val methodName: String)

    /**
     * Collection of all supported log levels.
     */
    enum class LogLevel(val value: Int) {
        ERROR(0),
        WARNING(1),
        INFO(2),
        DEBUG(3),
        VERBOSE(4);
    }

    /**
     * Specify whether debug mode is enabled.
     */
    var debugMode = false

    /**
     * Prefix for all Helium log messages.
     */
    internal const val TAG = "[Helium]"

    /**
     * The default magic number used to determine the offset on the call stack of the calling class
     * and method so the names can be used in log messages.
     */
    internal const val STACK_TRACE_LEVEL = 5

    /**
     * Log an error-level message.
     *
     * @param message The message to log.
     */
    fun e(message: String?) {
        message?.let { Log.e(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log a warning-level message.
     *
     * @param message The message to log.
     */
    fun w(message: String?) {
        message?.let { Log.w(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log an info-level message.
     *
     * @param message The message to log.
     */
    fun i(message: String?) {
        if (debugMode) {
            message?.let { Log.i(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
    }

    /**
     * Log a debug-level message.
     *
     * @param message The message to log.
     */
    fun d(message: String?) {
        if (debugMode) {
            message?.let { Log.d(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
    }

    /**
     * Log a verbose-level message.
     *
     * @param message The message to log.
     */
    fun v(message: String?) {
        if (debugMode) {
            message?.let { Log.v(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
    }

    /**
     * Post metrics data payload to the server on an ad lifecycle event basis.
     *
     * @param data The metrics data payload for a specific ad lifecycle event.
     */
    @OptIn(InternalSerializationApi::class)
    fun postMetricsData(
        data: Set<Metrics>,
        loadId: String? = null,
        eventResult: EventResult? = null
    ) {
        // No need to send empty/invalid/corrupted data since it's not going to be useful.
        // This is not the same as checking for partial data, which is actually valid and which could
        // manifest when an ad lifecycle event fails to complete.
        if (!metricsDataIsValid(data)) {
            return
        }

        // Build the payload early so we can also log it before sending it to the server.
        // This ensures what's logged is what's sent.
        val metricsRequestBody = buildMetricsDataRequestBody(data, eventResult)
        val event = data.first().event

        val payload = HeliumJson.writeJson(
            metricsRequestBody,
            MetricsRequestBody.serializer()
        ).jsonObject.toJSONObject()

        d("Metrics data for the $event lifecycle event: $payload")

        // Post the payload onto the callback for the public API.
        when (event) {
            Sdk.Event.INITIALIZATION -> CoroutineScope(Main).launch {
                HeliumSdk.chartboostMediationInternal.partnerInitializationResults.onResultsReceived(
                    PartnerInitializationResultsData(payload)
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
            val result = ChartboostMediationNetworking.trackEvent(
                event,
                loadId = loadId,
                metricsRequestBody
            )
            CoroutineScope(Main).launch {
                when (result) {
                    is ChartboostMediationNetworkingResult.Success ->
                        i("Successfully posted metrics data for the $event lifecycle event")

                    is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                        e("Failed to post metrics data for the $event lifecycle event: ${result.error}")
                    }

                    is ChartboostMediationNetworkingResult.Failure ->
                        e("Failed to post metrics data for the $event lifecycle event: ${result.error}")
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
     * @param chartboostMediationError The Helium error.
     * @param chartboostMediationErrorMessage The Helium error message.
     */
    fun postMetricsDataForFailedEvent(
        partner: String?,
        event: Sdk.Event,
        auctionIdentifier: String?,
        chartboostMediationError: ChartboostMediationError,
        chartboostMediationErrorMessage: String?,
        loadId: String? = null,
        eventResult: EventResult? = null
    ) {
        postMetricsData(
            setOf(
                Metrics(partner, event).apply {
                    start = System.currentTimeMillis()
                    end = System.currentTimeMillis()
                    duration = 0
                    auctionId = auctionIdentifier
                    isSuccess = false
                    this.chartboostMediationError = chartboostMediationError
                    this.chartboostMediationErrorMessage = chartboostMediationErrorMessage
                }
            ),
            loadId,
            eventResult
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
                d("$errorPrefix. Data set is empty.")
                false
            }

            !metricsDataBelongsToSameEvent(data) -> {
                d("$errorPrefix. Data set contains metrics data for multiple events.")
                false
            }

            else -> {
                true
            }
        }
    }

    /**
     * Get the class and method name of the caller on the call stack.
     *
     * @param stackTraceLevel A magic number used to determine the offset on the call stack of the
     * calling class and method so the names can be used in log messages. Defaults to [STACK_TRACE_LEVEL].
     *
     * @return A [StackTraceElements] object containing the class and method name of the caller.
     */
    internal fun getClassAndMethod(stackTraceLevel: Int = STACK_TRACE_LEVEL): StackTraceElements? {
        Thread.currentThread().stackTrace.run {
            return if (size > stackTraceLevel) {
                StackTraceElements(
                    this[stackTraceLevel].className.substringAfterLast('.'),
                    this[stackTraceLevel].methodName
                )
            } else {
                null
            }
        }
    }

    /**
     * Build a log message using a pre-defined template given the class and method name, and the
     * actual message.
     *
     * The template: `ClassName.method(): message`.
     *
     * @param stackTraceElements The class and method name of the caller.
     * @param message The actual message to log.
     *
     * @return The log message based on the template.
     */
    internal fun buildLogMsg(
        stackTraceElements: StackTraceElements?,
        message: String
    ): String {
        return "${
            if (stackTraceElements != null) "${stackTraceElements.className}.${stackTraceElements.methodName}():"
            else ""
        } $message"
    }

    /**
     * Build the metrics data payload for the current ad lifecycle event. Note that this is the payload
     * sent to the server. The payload for the public API may be transformed.
     *
     * @param data The metrics data set
     *
     * @return A JSONObject containing the finalized payload.
     */
    internal fun buildMetricsDataRequestBody(
        data: Set<Metrics>,
        eventResult: EventResult? = null
    ): MetricsRequestBody {
        return when (eventResult) {
            // SdkInitializationResults
            is EventResult.SdkInitializationResult.InitResult1A -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet()
                )
            }

            is EventResult.SdkInitializationResult.InitResult2A -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet()
                )
            }

            is EventResult.SdkInitializationResult.InitResult1B -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = emptySet(),
                    error = eventResult.jsonParseError
                )
            }

            is EventResult.SdkInitializationResult.InitResult2B -> {
                MetricsRequestBody(
                    result = eventResult.initResultCode,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    error = eventResult.jsonParseError
                )
            }

            // AdLoadResults
            is EventResult.AdLoadResult.AdLoadSuccess -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = data.map { MetricsData(it) }.toSet()
                )
            }

            is EventResult.AdLoadResult.AdLoadJsonFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = emptySet(),
                    error = eventResult.jsonParseError
                )
            }

            is EventResult.AdLoadResult.AdLoadPartnerFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    error = eventResult.metricsError
                )
            }

            is EventResult.AdLoadResult.AdLoadUnspecifiedFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = emptySet(),
                    error = eventResult.metricsError
                )
            }

            is EventResult.AdLoadResult.AdaptiveBannerTooLargeFailure -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = data.map { MetricsData(it) }.toSet(),
                    error = eventResult.metricsError
                )
            }

            else -> {
                MetricsRequestBody(
                    auctionId = data.firstOrNull()?.auctionId,
                    metrics = data.map { MetricsData(it) }.toSet()
                )
            }
        }
    }

    /**
     * Check if metrics data in the given data set belongs to the same [Metrics.Event].
     *
     * @param data The metrics data to check.
     *
     * @return True if all metrics data in the given data set belongs to the same [Metrics.Event], false otherwise.
     */
    private fun metricsDataBelongsToSameEvent(data: Set<Metrics>): Boolean {
        return data.all { it.event == data.firstOrNull()?.event }
    }

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
    private fun shouldPostMetricsData(event: Sdk.Event): Boolean {
        return AppConfigStorage.metricsEvents.contains(event)
    }
}
