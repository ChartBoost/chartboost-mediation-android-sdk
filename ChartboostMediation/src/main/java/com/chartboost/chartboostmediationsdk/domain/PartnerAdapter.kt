/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.app.Activity
import android.content.Context
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentValue

interface PartnerAdapter {
    /**
     * The partner adapter configuration.
     */
    val configuration: PartnerAdapterConfiguration

    /**
     * Initialize the partner SDK so that it's ready to request ads.
     *
     * @param context The current [Context].
     * @param partnerConfiguration A map of relevant data that can be used for initialization purposes.
     *
     * @return Result.success(Map<String, Any>) if the initialization was successful, otherwise Result.failure(Exception).
     */
    suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration,
    ): Result<Map<String, Any>>

    /**
     * Get a bid token if network bidding is supported.
     *
     * @param context The current [Context].
     * @param request The [PartnerAdPreBidRequest] instance containing relevant data for the current bid request.
     *
     * @return A Map of biddable token Strings.
     */
    suspend fun fetchBidderInformation(
        context: Context,
        request: PartnerAdPreBidRequest,
    ): Result<Map<String, String>>

    /**
     * Attempt to load a partner ad.
     *
     * @param context The current [Context].
     * @param request An [PartnerAdLoadRequest] instance containing relevant data for the current ad load call.
     * @param partnerAdListener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully loaded, Result.failure(Exception) otherwise.
     */
    suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener,
    ): Result<PartnerAd>

    /**
     * Attempt to show the currently loaded partner ad. This will not be called for banners.
     *
     * @param activity The current [Activity]
     * @param partnerAd The [PartnerAd] object containing the partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    suspend fun show(
        activity: Activity,
        partnerAd: PartnerAd,
    ): Result<PartnerAd>

    /**
     * Discard unnecessary partner ad objects and release resources. This is required to destroy
     * any Views from a specific [PartnerAd], especially for banners.
     *
     * @param partnerAd The [PartnerAd] object containing the partner ad to be discarded.
     *
     * @return Result.success(PartnerAd) if the ad was successfully discarded, Result.failure(Exception) otherwise.
     */
    suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd>

    /**
     * Notifies the partner of the current consents. Also notifies which ones changed. A key present
     * in the modifiedKeys but not in the consents map means the consent was removed.
     *
     * @param context The current [Context].
     * @param consents Map of [ConsentKey] to [ConsentValue] of all current consents.
     * @param modifiedKeys A set of [ConsentKey] that have changed in this consent update.
     */
    fun setConsents(
        context: Context,
        consents: Map<ConsentKey, ConsentValue>,
        modifiedKeys: Set<ConsentKey>,
    )

    /**
     * Notify the partner if the current user is underage (subject to COPPA or equivalent).
     *
     * @param context The current [Context].
     * @param isUserUnderage True if the user is underage, false otherwise.
     */
    fun setIsUserUnderage(
        context: Context,
        isUserUnderage: Boolean,
    )
}
