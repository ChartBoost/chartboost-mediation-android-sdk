/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import android.util.Log
import com.chartboost.chartboostmediationsdk.utils.LogController.STACK_TRACE_LEVEL

/**
 * @suppress
 *
 * Logger for partner adapters.
 */
class PartnerLogController {
    companion object {
        const val PRIVACY_TAG = "[Privacy]"

        /**
         * Log a partner adapter lifecycle event.
         *
         * @param event The [PartnerAdapterEvents] event to log.
         * @param message An optional message to log for added clarity.
         */
        fun log(
            event: PartnerAdapterEvents,
            message: String = "",
        ) {
            if (LogController.logLevel.value < LogController.LogLevel.DEBUG.value) {
                return
            }
            val messageFromTemplate =
                LogController.buildLogMsg(
                    LogController.getClassAndMethod(STACK_TRACE_LEVEL),
                    "${event.message}. $message",
                )

            Log.d(LogController.TAG, messageFromTemplate)
        }
    }

    /**
     * Collection of partner adapter lifecycle events for logging purposes.
     */
    enum class PartnerAdapterEvents(
        val message: String,
    ) {
        SETUP_STARTED("Partner setup started"),
        BIDDER_INFO_FETCH_STARTED("Partner bidder info fetch started"),
        LOAD_STARTED("Partner ad load started"),
        SHOW_STARTED("Partner ad show started"),
        INVALIDATE_STARTED("Partner ad invalidate started"),

        // Success events
        SETUP_SUCCEEDED("Partner setup succeeded"),
        BIDDER_INFO_FETCH_SUCCEEDED("Partner bidder info fetch succeeded"),
        LOAD_SUCCEEDED("Partner ad load succeeded"),
        SHOW_SUCCEEDED("Partner ad show succeeded"),
        INVALIDATE_SUCCEEDED("Partner ad invalidate succeeded"),

        // Failure events
        SETUP_FAILED("Partner setup failed"),
        BIDDER_INFO_FETCH_FAILED("Partner bidder info fetch failed"),
        LOAD_FAILED("Partner ad load failed"),
        SHOW_FAILED("Partner ad show failed"),
        INVALIDATE_FAILED("Partner ad invalidate failed"),

        // Partner ad lifecycle events
        DID_TRACK_IMPRESSION("Partner ad did track impression"),
        DID_CLICK("Partner ad did click"),
        DID_REWARD("Partner ad did reward"),
        DID_DISMISS("Partner ad did dismiss"),
        DID_EXPIRE("Partner ad did expire"),

        // Privacy events
        GDPR_UNKNOWN("$PRIVACY_TAG GDPR applicability is unknown"),
        GDPR_APPLICABLE("$PRIVACY_TAG GDPR is applicable"),
        GDPR_NOT_APPLICABLE("$PRIVACY_TAG GDPR is not applicable"),

        GDPR_CONSENT_GRANTED("$PRIVACY_TAG GDPR consent granted"),
        GDPR_CONSENT_DENIED("$PRIVACY_TAG GDPR consent denied"),
        GDPR_CONSENT_UNKNOWN("$PRIVACY_TAG GDPR consent unknown"),

        USP_CONSENT_GRANTED("$PRIVACY_TAG CCPA consent granted"),
        USP_CONSENT_DENIED("$PRIVACY_TAG CCPA consent denied"),

        USER_IS_UNDERAGE("$PRIVACY_TAG User is underage"),
        USER_IS_NOT_UNDERAGE("$PRIVACY_TAG User is not underage"),

        // Other events
        CUSTOM(""),
    }
}
