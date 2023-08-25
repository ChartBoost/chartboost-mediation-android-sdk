/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.domain.*
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

    @Contextual
    @SerialName("result")
    val result: String? = null,

    @SerialName("metrics")
    val metrics: Set<MetricsData>,

    @Contextual
    @SerialName("error")
    val error: MetricsError? = null
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
        start = metrics.start,
        end = if (didPartnerTimeOut(metrics.chartboostMediationError)) null else metrics.end,
        duration = if (didPartnerTimeOut(metrics.chartboostMediationError) || metrics.duration?.sign != 1) null else metrics.duration,
        isSuccess = metrics.isSuccess,
        chartboostMediationError = metrics.chartboostMediationError,
        chartboostMediationErrorCode = metrics.chartboostMediationError?.code,
        chartboostMediationErrorMessage = metrics.chartboostMediationErrorMessage,
        partnerSdkVersion = metrics.partnerSdkVersion,
        partnerAdapterVersion = metrics.partnerAdapterVersion
    )

    /**
     * @suppress
     */
    companion object {
        private fun didPartnerTimeOut(error: ChartboostMediationError?) =
            error == ChartboostMediationError.CM_INITIALIZATION_FAILURE_TIMEOUT ||
                    error == ChartboostMediationError.CM_PREBID_FAILURE_TIMEOUT ||
                    error == ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT ||
                    error == ChartboostMediationError.CM_SHOW_FAILURE_TIMEOUT ||
                    error == ChartboostMediationError.CM_INVALIDATE_FAILURE_TIMEOUT
    }
}
