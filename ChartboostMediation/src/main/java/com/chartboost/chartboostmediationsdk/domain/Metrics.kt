/*
 * Copyright 2024-2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.util.Size

/**
 * @suppress
 *
 * Collection of measurable metrics per partner per event.
 *
 * @property partner The partner name.
 * @property event The ad lifecycle event.
 */
class Metrics(
    val partner: String?,
    val event: TrackingEvent,
) {
    /**
     * Collection of reportable network type
     */
    enum class NetworkType(
        val value: String,
    ) {
        MEDIATION("mediation"),
        BIDDING("bidding"),
        ;

        companion object {
            fun getType(isMediation: Boolean): String = if (isMediation) MEDIATION.value else BIDDING.value
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
     * The queue ID, if any.
     */
    var queueId: String? = null

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
     * When a partner times out, the true duration might not be available when Chartboost Mediation posts the metrics
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
     * The [ChartboostMediationError] constant name, if any, returned by the Chartboost Mediation SDK or mediation adapter.
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
     * Get the corresponding [ChartboostMediationError] timeout for the given [TrackingEvent].
     *
     * @param event The [TrackingEvent] to get the timeout for.
     */
    private fun getTimeoutForEvent(event: TrackingEvent): ChartboostMediationError =
        when (event) {
            TrackingEvent.INITIALIZATION -> ChartboostMediationError.InitializationError.Timeout
            TrackingEvent.PREBID -> ChartboostMediationError.PrebidError.Timeout
            TrackingEvent.LOAD -> ChartboostMediationError.LoadError.AdRequestTimeout
            TrackingEvent.SHOW -> ChartboostMediationError.ShowError.Timeout
            else -> ChartboostMediationError.OtherError.Unknown
        }
}
