/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.network.Endpoints
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sign

/**
 * @suppress
 */
@Serializable
class MetricsRequestBody constructor(
    @SerialName("auction_id")
    val auctionId: String? = null,
    @SerialName("queue_id")
    val queueId: String? = null,
    @SerialName("placement_type")
    val placementType: String? = null,
    @SerialName("size")
    val size: BannerAdDimensions? = null,
    @SerialName("start")
    val start: Long? = null,
    @SerialName("end")
    val end: Long? = null,
    @SerialName("duration")
    val duration: Long? = null,
    @SerialName("background_duration")
    val backgroundDurationMs: Long? = null,
    @Contextual
    @SerialName("result")
    val result: String? = null,
    @SerialName("metrics")
    val metrics: Set<MetricsData>,
    @Contextual
    @SerialName("error")
    val error: MetricsError? = null,
)

/**
 * @suppress
 */
@Serializable
class MetricsData private constructor(
    @SerialName("network_type")
    val networkType: String? = null,
    @SerialName("line_item_id")
    val lineItemId: String? = null,
    @SerialName("partner_placement")
    val partnerPlacement: String? = null,
    @SerialName("partner")
    val partner: String? = null,
    @SerialName("start")
    val start: Long? = null,
    @SerialName("end")
    val end: Long? = null,
    @SerialName("duration")
    val duration: Long? = null,
    @SerialName("is_success")
    val isSuccess: Boolean,
    @SerialName("helium_error")
    val chartboostMediationError: ChartboostMediationError? = null,
    @SerialName("helium_error_code")
    val chartboostMediationErrorCode: String? = null,
    @SerialName("helium_error_message")
    val chartboostMediationErrorMessage: String? = null,
    @SerialName("partner_sdk_version")
    val partnerSdkVersion: String? = null,
    @SerialName("partner_adapter_version")
    val partnerAdapterVersion: String? = null,
) {
    constructor(metrics: Metrics) : this(
        networkType = metrics.networkType,
        lineItemId = metrics.lineItemId,
        partnerPlacement = metrics.partnerPlacement,
        partner = metrics.partner,
        start = resolveStartTime(metrics),
        end = if (didPartnerTimeOut(metrics.chartboostMediationError)) null else metrics.end,
        duration = if (didPartnerTimeOut(metrics.chartboostMediationError) || metrics.duration?.sign != 1) null else metrics.duration,
        isSuccess = metrics.isSuccess,
        chartboostMediationError = metrics.chartboostMediationError,
        chartboostMediationErrorCode = metrics.chartboostMediationError?.code,
        chartboostMediationErrorMessage = metrics.chartboostMediationErrorMessage,
        partnerSdkVersion = metrics.partnerSdkVersion,
        partnerAdapterVersion = metrics.partnerAdapterVersion,
    )

    /**
     * @suppress
     */
    companion object {
        /**
         * The last known start time.
         */
        @Volatile
        private var startLastKnownGood: Long = System.currentTimeMillis()

        /**
         * Heuristic to determine the start time of a partner. This is not meant to be foolproof.
         * Once we've completely rewritten metrics, we can stop doing all this.
         *
         * @param metrics The metrics object.
         *
         * @return The start time of the event, guaranteed to be non-null, so it should always be available.
         */
        private fun resolveStartTime(metrics: Metrics): Long {
            val event = metrics.event
            val start = metrics.start
            val end = metrics.end

            // Only do this for initialization since it's known to be a problem.
            if (event != Endpoints.Sdk.Event.INITIALIZATION) {
                return start ?: System.currentTimeMillis()
            }

            return when {
                // This is a temporary fix for the issue where the start time is occasionally not set (i.e. null).
                // We store the last known start time and use it as a backup when the start time is null.
                start != null && (end == null || start <= end) -> {
                    startLastKnownGood = start
                    start
                }

                // If the start time is greater than the end time, then the start time is incorrect.
                // Use end time as the start time instead. This will skew the duration, but it's better than nothing.
                start != null && end != null && start > end -> end

                // If the start time is null, then use the last known start time as a backup.
                // For concurrent requests, this will result in the start time being slightly off, but it's better than nothing.
                else ->
                    startLastKnownGood.takeIf { it <= (end ?: Long.MAX_VALUE) }
                        ?: System.currentTimeMillis()
            }
        }

        private fun didPartnerTimeOut(error: ChartboostMediationError?) =
            error == ChartboostMediationError.CM_INITIALIZATION_FAILURE_TIMEOUT ||
                error == ChartboostMediationError.CM_PREBID_FAILURE_TIMEOUT ||
                error == ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT ||
                error == ChartboostMediationError.CM_SHOW_FAILURE_TIMEOUT ||
                error == ChartboostMediationError.CM_INVALIDATE_FAILURE_TIMEOUT
    }
}
