/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.controllers

import android.content.Context
import com.chartboost.chartboostmediationsdk.utils.LogController

/**
 * @suppress
 *
 * This class manages privacy settings for the Chartboost Mediation SDK.
 */
class PrivacyController(
    context: Context,
) {
    /**
     * Internal keys used for reading privacy settings from SharedPreferences.
     */
    companion object {
        /**
         * The IAB GDPR applies key.
         */
        private const val GDPR_APPLIES_KEY = "IABTCF_gdprApplies"

        /**
         * A placeholder value for GDPR not set.
         */
        private const val GDPR_NOT_SET = -1

        /**
         * The Interactive Advertising Bureau Global Privacy Platform Sections String.
         * Note: This will later be moved to Chartboost Core.
         */
        private const val GPP_SID = "IABGPP_GppSID"
    }

    private val sharedPreferences =
        context.getSharedPreferences(
            "${context.packageName}_preferences",
            Context.MODE_PRIVATE,
        )

    /**
     * Is this user covered under GDPR?
     * 1 for covered. 0 for not.
     * If this is not set, then this returns null.
     */
    val gdpr: Int?
        get() =
            try {
                sharedPreferences.getInt(GDPR_APPLIES_KEY, GDPR_NOT_SET).takeIf { it != GDPR_NOT_SET }
            } catch (e: ClassCastException) {
                LogController.e("Unable to get GDPR: $e")
                null
            }

    /**
     * The GPP Sections String.
     * Note: This will later be moved to Chartboost Core.
     */
    val gppSid: String?
        get() =
            try {
                sharedPreferences.getString(GPP_SID, "")
            } catch (e: ClassCastException) {
                LogController.e("Unable to get GPP Section string: $e")
                null
            }
}
