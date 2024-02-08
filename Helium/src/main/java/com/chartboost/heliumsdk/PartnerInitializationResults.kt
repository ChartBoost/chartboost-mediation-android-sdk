/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import com.chartboost.heliumsdk.domain.AppConfigStorage
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.utils.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * @suppress
 *
 * Helium ad lifecycle initialization result notification mechanism.
 */
class PartnerInitializationResults {
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
        private const val HELIUM_ERROR_CODE_KEY = "helium_error_code"
        private const val HELIUM_ERROR_KEY = "helium_error"
        private const val HELIUM_ERROR_MESSAGE_KEY = "helium_error_message"
        private const val IS_SUCCESS_KEY = "is_success"
        private const val METRICS_NODE_KEY = "metrics"
        private const val PARTNER_ADAPTER_VERSION_KEY = "partner_adapter_version"
        private const val PARTNER_KEY = "partner"
        private const val PARTNER_SDK_VERSION_KEY = "partner_sdk_version"
        private const val SESSION_ID_KEY = "session_id"
        private const val START_KEY = "start"
        private const val TIMEOUT_SECONDS_KEY = "timeout_seconds"
    }

    private val observers: MutableSet<PartnerInitializationResultsObserver> = mutableSetOf()

    /**
     * Subscribe your observer for initialization result.
     */
    fun subscribe(observer: PartnerInitializationResultsObserver) {
        CoroutineScope(Main.immediate).launch {
            observers.add(observer)
        }
    }

    /**
     * Unsubscribe your observer for initialization result.
     */
    private fun unsubscribe(observer: PartnerInitializationResultsObserver) {
        CoroutineScope(Main.immediate).launch {
            observers.remove(observer)
        }
    }

    fun onResultsReceived(results: PartnerInitializationResultsData) {
        CoroutineScope(Main.immediate).launch {
            observers.forEach { observer ->
                observer.onPartnerInitializationResultsReady(
                    PartnerInitializationResultsData(
                        getPublicPayload(results.data),
                    ),
                )

                // Since initialization is a one time event per app session, we can unsubscribe the
                // observer ourselves and avoid the need for the publisher to do it.
                unsubscribe(observer)
            }
        }
    }

    /**
     * Transform the internal initialization result data to a publicly-consumable format.
     *
     * @param payload The metrics data payload sent to the server.
     *
     * @return The transformed initialization result data.
     */
    private fun getPublicPayload(payload: JSONObject): JSONObject {
        return JSONObject().apply {
            put(SESSION_ID_KEY, Environment.sessionId)

            val metrics = payload.getJSONArray(METRICS_NODE_KEY)
            val groupedMetrics =
                (0 until metrics.length())
                    .map { metrics.getJSONObject(it) }
                    .groupBy { partnerData ->
                        when {
                            partnerData.opt(IS_SUCCESS_KEY) as Boolean -> SUCCESS_GROUP_KEY
                            partnerData.opt(
                                HELIUM_ERROR_CODE_KEY,
                            ) == ChartboostMediationError.CM_INITIALIZATION_FAILURE_TIMEOUT.code -> IN_PROGRESS_GROUP_KEY
                            partnerData.opt(
                                HELIUM_ERROR_CODE_KEY,
                            ) == ChartboostMediationError.CM_INITIALIZATION_SKIPPED.code -> SKIPPED_GROUP_KEY
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
                    createFailureJson(it, it.opt(HELIUM_ERROR_CODE_KEY) as String)
                },
            )
        }
    }

    private fun <T> createJsonArray(
        groupedMetrics: Map<String, List<JSONObject>>,
        key: String,
        transform: (JSONObject) -> T,
    ): JSONArray {
        return JSONArray().apply {
            putAll(groupedMetrics[key]?.map(transform) ?: emptyList())
        }
    }

    private fun JSONArray.putAll(collection: Collection<Any?>) {
        collection.forEach { this.put(it) }
    }

    private fun createCommonJson(partnerData: JSONObject): JSONObject {
        return JSONObject().apply {
            put(PARTNER_KEY, partnerData.optString(PARTNER_KEY))
            put(START_KEY, partnerData.optLong(START_KEY))
            put(PARTNER_SDK_VERSION_KEY, partnerData.optString(PARTNER_SDK_VERSION_KEY))
            put(PARTNER_ADAPTER_VERSION_KEY, partnerData.optString(PARTNER_ADAPTER_VERSION_KEY))
        }
    }

    private fun createSuccessJson(partnerData: JSONObject): JSONObject {
        return createCommonJson(partnerData).apply {
            put(END_KEY, partnerData.optLong(END_KEY))
            put(DURATION_KEY, partnerData.optInt(DURATION_KEY))
        }
    }

    private fun createInProgressJson(partnerData: JSONObject): JSONObject {
        return createCommonJson(partnerData).apply {
            put(TIMEOUT_SECONDS_KEY, AppConfigStorage.initializationMetricsPostTimeout)
        }
    }

    private fun createFailureJson(
        partnerData: JSONObject,
        heliumErrorCode: String,
    ): JSONObject {
        return createCommonJson(partnerData).apply {
            put(END_KEY, partnerData.optLong(END_KEY))
            put(DURATION_KEY, partnerData.optInt(DURATION_KEY))
            put(HELIUM_ERROR_KEY, partnerData.opt(HELIUM_ERROR_KEY))
            put(HELIUM_ERROR_CODE_KEY, heliumErrorCode)
            put(HELIUM_ERROR_MESSAGE_KEY, partnerData.optString(HELIUM_ERROR_MESSAGE_KEY))
        }
    }
}

interface PartnerInitializationResultsObserver {
    /**
     * When initialization results are compiled and ready for ingestion. Note that this is not the
     * same as the [HeliumSdk.HeliumSdkListener.didInitialize] callback, when _results_ might not
     * yet be processed.
     *
     * @param data The initialization results data.
     */
    fun onPartnerInitializationResultsReady(data: PartnerInitializationResultsData)
}

/**
 * The initialization results data.
 */
data class PartnerInitializationResultsData(val data: JSONObject)
