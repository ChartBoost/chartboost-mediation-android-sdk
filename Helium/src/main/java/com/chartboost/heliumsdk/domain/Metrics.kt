/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.util.Size
import com.chartboost.heliumsdk.network.Endpoints.Sdk
import com.chartboost.heliumsdk.utils.getMaxJsonPayload
import com.google.android.gms.common.util.VisibleForTesting
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

/**
 * @suppress
 *
 * Collection of measurable metrics per partner per event.
 *
 * @property partner The partner name.
 * @property event The ad lifecycle event.
 */
class Metrics(val partner: String?, val event: Sdk.Event) {

    /**
     * Collection of reportable network type
     */
    enum class NetworkType(val value: String) {
        MEDIATION("mediation"),
        BIDDING("bidding");

        companion object {
            fun getType(isMediation: Boolean): String {
                return if (isMediation) MEDIATION.value else BIDDING.value
            }
        }
    }

    /**
     * The partner SDK version, if any.
     */
    var partnerSdkVersion: String? = null

    /**
     * The partner adapter version, if any.
     */
    var partnerAdapterVersion: String? = null

    /**
     * The current auction ID, if any.
     */
    var auctionId: String? = null

    /**
     * The placement type, if any.
     */
    var placementType: String? = null

    /**
     * The placement size, if any.
     */
    var size: Size? = null

    /**
     * The network type (i.e. "bidding" or "mediation").
     */
    var networkType: String? = null

    /**
     * The line item ID, if any.
     */
    var lineItemId: String? = null

    /**
     * The partner placement, if any.
     */
    var partnerPlacement: String? = null

    /**
     * The timestamp in ms when the event starts.
     */
    var start: Long? = null

    /**
     * The timestamp in ms when the event ends. When a partner times out, the true end time is unknown
     * and therefore unavailable for consumption.
     */
    var end: Long? = null

    /**
     * The duration in ms the partner takes to complete the event, excluding suspension time.
     * When a partner times out, the true duration might not be available when Helium posts the metrics
     * data payload to the server.
     */
    var duration: Long? = null
        get() {
            return if (field == null) {
                field = end?.minus(start ?: 0L) ?: 0L
                field
            } else {
                field
            }
        }

    /**
     * Whether the partner successfully completes the event. Defaults to false.
     */
    var isSuccess = false

    /**
     * The [ChartboostMediationError] constant name, if any, returned by the Helium SDK or mediation adapter.
     *
     * Note: When a partner times out after we have stopped collecting metrics data, we won't be able
     * to set this value. In cases like that, retrofit the field based on the `is_success` flag.
     */
    var chartboostMediationError: ChartboostMediationError? = null
        get() {
            return if (!isSuccess && field == null) {
                field = getTimeoutForEvent(event)
                field
            } else {
                field
            }
        }

    /**
     * The error message if there has been an error.
     *
     * Note: When a partner times out after we have stopped collecting metrics data, we won't be able
     * to set this value. In cases like that, retrofit the field based on the `is_success` flag.
     */
    var chartboostMediationErrorMessage: String? = null
        get() {
            return if (!isSuccess && field == null) {
                field = (getTimeoutForEvent(event)).message
                field
            } else {
                field
            }
        }

    /**
     * Get the corresponding [ChartboostMediationError] timeout for the given [Sdk.Event].
     *
     * @param event The [Sdk.Event] to get the timeout for.
     */
    private fun getTimeoutForEvent(event: Sdk.Event): ChartboostMediationError {
        return when (event) {
            Sdk.Event.INITIALIZATION -> ChartboostMediationError.CM_INITIALIZATION_FAILURE_TIMEOUT
            Sdk.Event.PREBID -> ChartboostMediationError.CM_PREBID_FAILURE_TIMEOUT
            Sdk.Event.LOAD -> ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT
            Sdk.Event.SHOW -> ChartboostMediationError.CM_SHOW_FAILURE_TIMEOUT
            else -> ChartboostMediationError.CM_UNKNOWN_ERROR
        }
    }
}
