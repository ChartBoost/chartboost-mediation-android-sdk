/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.content.Context

interface PartnerAdapter {
    /**
     * The underlying partner's SDK version.
     */
    val partnerSdkVersion: String

    /**
     * The version of this adapter.
     *
     * Note that the version string will be in the format of `Chartboost Mediation.Partner.Partner.Partner[.Partner].Adapter`,
     * in which `Chartboost Mediation` is the major version of the Chartboost Mediation SDK, `Partner` is the major.minor.patch[.build]
     * version of the partner SDK, and `Adapter` is the version of the adapter. Partners may have 3 or 4 digits.
     */
    val adapterVersion: String

    /**
     * The Chartboost Mediation internal partner ID.
     */
    val partnerId: String

    /**
     * The pretty display name or brand name of the partner.
     */
    val partnerDisplayName: String

    /**
     * Initialize the partner SDK so that it's ready to request ads.
     *
     * @param context The current [Context].
     * @param partnerConfiguration A map of relevant data that can be used for initialization purposes.
     *
     * @return Result.success() if the initialization was successful, otherwise Result.failure(Exception).
     */
    suspend fun setUp(context: Context, partnerConfiguration: PartnerConfiguration): Result<Unit>

    /**
     * Get a bid token if network bidding is supported.
     *
     * @param context The current [Context].
     * @param request The [PreBidRequest] instance containing relevant data for the current bid request.
     *
     * @return A Map of biddable token Strings.
     */
    suspend fun fetchBidderInformation(
        context: Context,
        request: PreBidRequest
    ): Map<String, String>

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
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd>

    /**
     * Attempt to show the currently loaded partner ad. This will not be called for banners.
     *
     * @param context The current [Context]
     * @param partnerAd The [PartnerAd] object containing the partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd>

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
     * Notify the partner whether GDPR applies and if so, whether the user has consented.
     *
     * @param context The current [Context].
     * @param applies True if GDPR applies, false otherwise.
     * @param gdprConsentStatus The user's GDPR consent status.
     */
    fun setGdpr(context: Context, applies: Boolean?, gdprConsentStatus: GdprConsentStatus)

    /**
     * Notify the partner of CCPA consent. https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/US%20Privacy%20String.md
     * for more details regarding the CCPA privacy string.
     *
     * @param context The current [Context].
     * @param hasGrantedCcpaConsent True if the user has granted CCPA consent, false otherwise.
     * @param privacyString The CCPA privacy string.
     */
    fun setCcpaConsent(context: Context, hasGrantedCcpaConsent: Boolean, privacyString: String)

    /**
     * Notify the partner if the current user is subject to COPPA.
     *
     * @param context The current [Context].
     * @param isSubjectToCoppa True if the user is subject to COPPA, false otherwise.
     */
    fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean)
}
