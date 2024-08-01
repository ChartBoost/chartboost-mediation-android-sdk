/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import com.chartboost.chartboostmediationsdk.domain.AppConfigStorage
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * @suppress
 *
 * Chartboost Mediation ad lifecycle initialization result notification mechanism.
 */
class PartnerAdapterInitializationResults {
    companion object {
        /**
         * Collection of keys for grouping initialization results by.
         */
        private const val SUCCESS_GROUP_KEY = "success"
        private const val IN_PROGRESS_GROUP_KEY = "in_progress"
        private const val FAILURE_GROUP_KEY = "failure"
        private const val SKIPPED_GROUP_KEY = "skipped"

        /**
         * Collection of keys for parsing initialization result data.
         */
        private const val DURATION_KEY = "duration"
        private const val END_KEY = "end"
        private const val CHARTBOOST_MEDIATION_ERROR_CODE_KEY = "helium_error_code"
        private const val CHARTBOOST_MEDIATION_ERROR_KEY = "helium_error"
        private const val CHARTBOOST_MEDIATION_ERROR_MESSAGE_KEY = "helium_error_message"
        private const val IS_SUCCESS_KEY = "is_success"
        private const val METRICS_NODE_KEY = "metrics"
        private const val PARTNER_ADAPTER_VERSION_KEY = "partner_adapter_version"
        private const val PARTNER_KEY = "partner"
        private const val PARTNER_SDK_VERSION_KEY = "partner_sdk_version"
        private const val SESSION_ID_KEY = "session_id"
        private const val START_KEY = "start"
        private const val TIMEOUT_SECONDS_KEY = "timeout_seconds"
    }

    private val observers: MutableSet<PartnerAdapterInitializationResultsObserver> = mutableSetOf()

    /**
     * Subscribe your observer for initialization result.
     */
    fun subscribe(observer: PartnerAdapterInitializationResultsObserver) {
        CoroutineScope(Main.immediate).launch {
            observers.add(observer)
        }
    }

    fun onResultsReceived(results: PartnerAdapterInitializationResultsData) {
        CoroutineScope(Main.immediate).launch {
            observers.forEach { observer ->
                observer.onPartnerAdapterInitializationResultsReady(
                    PartnerAdapterInitializationResultsData(
                        getPublicPayload(results.data),
                    ),
                )
            }

            // Since initialization is a one time event per app session, we can unsubscribe
            // all observers ourselves and avoid the need for the publisher to do it.
            observers.clear()
        }
    }

    /**
     * Transform the internal initialization result data to a publicly-consumable format.
     *
     * @param payload The metrics data payload sent to the server.
     *
     * @return The transformed initialization result data.
     */
    private fun getPublicPayload(payload: JSONObject): JSONObject =
        JSONObject().apply {
            put(SESSION_ID_KEY, ChartboostCore.analyticsEnvironment.appSessionIdentifier)

            val metrics = payload.getJSONArray(METRICS_NODE_KEY)
            val groupedMetrics =
                (0 until metrics.length())
                    .map { metrics.getJSONObject(it) }
                    .groupBy { partnerData ->
                        when {
                            partnerData.opt(IS_SUCCESS_KEY) as Boolean -> SUCCESS_GROUP_KEY

                            partnerData.opt(
                                CHARTBOOST_MEDIATION_ERROR_CODE_KEY,
                            ) == ChartboostMediationError.InitializationError.Timeout.code -> IN_PROGRESS_GROUP_KEY

                            partnerData.opt(
                                CHARTBOOST_MEDIATION_ERROR_CODE_KEY,
                            ) == ChartboostMediationError.InitializationError.Skipped.code -> SKIPPED_GROUP_KEY

                            else -> FAILURE_GROUP_KEY
                        }
                    }

            put(
                SUCCESS_GROUP_KEY,
                createJsonArray(groupedMetrics, SUCCESS_GROUP_KEY) {
                    createSuccessJson(it)
                },
            )
            put(
                IN_PROGRESS_GROUP_KEY,
                createJsonArray(groupedMetrics, IN_PROGRESS_GROUP_KEY) {
                    createInProgressJson(it)
                },
            )
            put(
                SKIPPED_GROUP_KEY,
                createJsonArray(groupedMetrics, SKIPPED_GROUP_KEY) {
                    it.optString(PARTNER_KEY)
                },
            )
            put(
                FAILURE_GROUP_KEY,
                createJsonArray(groupedMetrics, FAILURE_GROUP_KEY) {
                    createFailureJson(it, it.opt(CHARTBOOST_MEDIATION_ERROR_CODE_KEY) as String)
                },
            )
        }

    private fun <T> createJsonArray(
        groupedMetrics: Map<String, List<JSONObject>>,
        key: String,
        transform: (JSONObject) -> T,
    ): JSONArray =
        JSONArray().apply {
            putAll(groupedMetrics[key]?.map(transform) ?: emptyList())
        }

    private fun JSONArray.putAll(collection: Collection<Any?>) {
        collection.forEach { this.put(it) }
    }

    private fun createCommonJson(partnerData: JSONObject): JSONObject =
        JSONObject().apply {
            put(PARTNER_KEY, partnerData.optString(PARTNER_KEY))
            put(START_KEY, partnerData.optLong(START_KEY))
            put(PARTNER_SDK_VERSION_KEY, partnerData.optString(PARTNER_SDK_VERSION_KEY))
            put(PARTNER_ADAPTER_VERSION_KEY, partnerData.optString(PARTNER_ADAPTER_VERSION_KEY))
        }

    private fun createSuccessJson(partnerData: JSONObject): JSONObject =
        createCommonJson(partnerData).apply {
            put(END_KEY, partnerData.optLong(END_KEY))
            put(DURATION_KEY, partnerData.optInt(DURATION_KEY))
        }

    private fun createInProgressJson(partnerData: JSONObject): JSONObject =
        createCommonJson(partnerData).apply {
            put(TIMEOUT_SECONDS_KEY, AppConfigStorage.initializationMetricsPostTimeout)
        }

    private fun createFailureJson(
        partnerData: JSONObject,
        chartboostMediationErrorCode: String,
    ): JSONObject =
        createCommonJson(partnerData).apply {
            put(END_KEY, partnerData.optLong(END_KEY))
            put(DURATION_KEY, partnerData.optInt(DURATION_KEY))
            put(CHARTBOOST_MEDIATION_ERROR_KEY, partnerData.opt(CHARTBOOST_MEDIATION_ERROR_KEY))
            put(CHARTBOOST_MEDIATION_ERROR_CODE_KEY, chartboostMediationErrorCode)
            put(CHARTBOOST_MEDIATION_ERROR_MESSAGE_KEY, partnerData.optString(CHARTBOOST_MEDIATION_ERROR_MESSAGE_KEY))
        }
}

interface PartnerAdapterInitializationResultsObserver {
    /**
     * When initialization results are compiled and ready for ingestion. Note that this is not the
     * same as the [ChartboostMediationSdk.initialize] call, when _results_ might not
     * yet be processed.
     *
     * @param data The initialization results data.
     */
    fun onPartnerAdapterInitializationResultsReady(data: PartnerAdapterInitializationResultsData)
}

/**
 * The initialization results data.
 */
data class PartnerAdapterInitializationResultsData(
    val data: JSONObject,
)
