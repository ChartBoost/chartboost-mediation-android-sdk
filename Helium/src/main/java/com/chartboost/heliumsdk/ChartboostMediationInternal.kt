/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.app.Activity
import android.content.Context
import com.chartboost.heliumsdk.ad.ChartboostMediationFullscreenAdQueueManager
import com.chartboost.heliumsdk.controllers.*
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.utils.BackgroundTimeMonitor
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.FullscreenAdShowingState
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Holds all the internal logic for Chartboost Mediation.
 */
internal class ChartboostMediationInternal(internal val partnerController: PartnerController = PartnerController()) {
    internal var adController: AdController? = null
    internal var appContext: Context? = null
    internal var appId: String? = null
    internal var appSignature: String? = null
    internal var gameEngineName: String? = null
    internal var gameEngineVersion: String? = null
    internal var privacyController: PrivacyController? = null
    internal var testMode = false
    internal var shouldDiscardOversizedAds = false
    internal var userIdentifier: String? = null
    internal var weakActivityContext: WeakReference<Activity?> = WeakReference(null)

    internal val fullscreenAdShowingState = FullscreenAdShowingState()
    internal val ilrd = Ilrd()
    internal val partnerInitializationResults = PartnerInitializationResults()
    internal val partnerConsents = PartnerConsents()

    internal var initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.IDLE

    /**
     * Stores the pre-init value of GDPR applies until the SDK is initialized.
     */
    private var gdprApplies: Boolean? = null

    /**
     * Stores the pre-init value of GDPR consent status until the SDK is initialized.
     */
    private var gdprConsentStatus: GdprConsentStatus? = null

    /**
     * Stores the pre-init value of COPPA until the SDK is initialized.
     */
    private var isSubjectToCoppa: Boolean? = null

    /**
     * Stores the pre-init value of CCPA consent until the SDK is initialized.
     */
    private var ccpaConsentGranted: Boolean? = null

    internal suspend fun initialize(
        context: Context,
        appId: String,
        appSignature: String,
        options: HeliumInitializationOptions?,
    ): Result<Unit> {
        return withContext(Main) {
            try {
                val preferences =
                    context.getSharedPreferences(
                        CHARTBOOST_MEDIATION_INTERNAL_SHARED_PREFS,
                        Context.MODE_PRIVATE,
                    )
                LogController.LogLevel.valueOf(
                    preferences.getString(
                        SERVER_LOG_LEVEL_OVERRIDE,
                        null,
                    ) ?: "",
                )
            } catch (iae: IllegalArgumentException) {
                null
            }?.let {
                LogController.serverLogLevelOverride = it
            }

            if (initializationStatus == HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZED) {
                this@ChartboostMediationInternal.weakActivityContext =
                    if (context is Activity) WeakReference(context) else weakActivityContext
                LogController.d("Chartboost Mediation already initialized. Potentially updating the activity")
                return@withContext Result.success(Unit)
            }
            if (initializationStatus == HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZING) {
                return@withContext Result.failure(
                    ChartboostMediationAdException(
                        ChartboostMediationError.CM_INITIALIZATION_FAILURE_INITIALIZATION_IN_PROGRESS,
                    ),
                )
            }
            initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZING
            LogController.d("Chartboost Mediation initialize called with SDK Key: $appId")

            this@ChartboostMediationInternal.appId = appId
            this@ChartboostMediationInternal.appSignature = appSignature
            this@ChartboostMediationInternal.weakActivityContext =
                if (context is Activity) WeakReference(context) else weakActivityContext
            this@ChartboostMediationInternal.appContext = context.applicationContext
            val localPrivacyController = privacyController ?: PrivacyController(context, partnerConsents)
            privacyController = localPrivacyController
            val bidController = BidController(partnerController)
            adController =
                AdController(
                    bidController = bidController,
                    partnerController = partnerController,
                    privacyController = localPrivacyController,
                    loadRateLimiter = LoadRateLimiter(),
                    backgroundTimeMonitor = BackgroundTimeMonitor(),
                    ilrd = ilrd,
                )

            partnerConsents.addPartnerConsentsObserver(
                object :
                    PartnerConsents.PartnerConsentsObserver {
                    override fun onPartnerConsentsUpdated() {
                        runGdprConsentTask(context.applicationContext, localPrivacyController)
                        runCcpaConsentTask(context.applicationContext, localPrivacyController)
                    }
                },
            )

            // Grab the app configuration from the server or from local if server is unavailable
            AppConfigController(context.applicationContext).get()

            // Handle processing the config and also initialize all partners
            val error =
                ChartboostMediationAppConfigurationHandler(
                    context,
                    options,
                ).handleConfigurationChange(partnerController)

            if (error != null) {
                initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.IDLE
                ChartboostMediationFullscreenAdQueueManager.autoStartQueues(false)
                return@withContext Result.failure(ChartboostMediationAdException(error))
            }

            Environment.startSession(context)
            localPrivacyController.updatePartnerConsentsFromDisk()
            runGdprConsentTask(context, localPrivacyController)
            runCcpaConsentTask(context, localPrivacyController)
            runSubjectToCoppaTask(context, localPrivacyController)
            initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZED
            ChartboostMediationFullscreenAdQueueManager.autoStartQueues(true)
            Result.success(Unit)
        }
    }

    /**
     * Locally sets gdprApplies but may wait if not initialized to apply the changes.
     *
     * @param isSubjectToGdpr True if the user is subject to GDPR, false otherwise.
     */
    internal fun setSubjectToGdpr(isSubjectToGdpr: Boolean) {
        gdprApplies = isSubjectToGdpr
        checkAndRun(::runGdprConsentTask)
    }

    /**
     * Locally sets gdprConsentStatus but may wait if not initialized to apply the changes.
     *
     * @param hasGivenGdprConsent True if the user has granted consent, false otherwise.
     */
    internal fun setUserHasGivenConsent(hasGivenGdprConsent: Boolean) {
        gdprConsentStatus =
            if (hasGivenGdprConsent) GdprConsentStatus.GDPR_CONSENT_GRANTED else GdprConsentStatus.GDPR_CONSENT_DENIED
        checkAndRun(::runGdprConsentTask)
    }

    private fun runGdprConsentTask(
        context: Context,
        privacyController: PrivacyController,
    ) {
        val privacyControllerGdprApplies = privacyController.gdpr
        val localGdprApplies =
            gdprApplies ?: when (privacyControllerGdprApplies) {
                PrivacyController.PrivacySetting.TRUE.value -> true

                PrivacyController.PrivacySetting.FALSE.value -> false

                // If unset or null, don't continue setting GDPR consent
                else -> return
            }
        val localGdprConsentStatus =
            gdprConsentStatus ?: when (privacyController.userConsent) {
                true -> GdprConsentStatus.GDPR_CONSENT_GRANTED
                false -> GdprConsentStatus.GDPR_CONSENT_DENIED
                else -> GdprConsentStatus.GDPR_CONSENT_UNKNOWN
            }

        privacyController.gdpr =
            if (localGdprApplies) PrivacyController.PrivacySetting.TRUE.value else PrivacyController.PrivacySetting.FALSE.value
        when (localGdprConsentStatus) {
            GdprConsentStatus.GDPR_CONSENT_GRANTED -> privacyController.userConsent = true
            GdprConsentStatus.GDPR_CONSENT_DENIED -> privacyController.userConsent = false
            else -> {} // do nothing
        }

        partnerController.setGdpr(context, localGdprApplies, localGdprConsentStatus, partnerConsents)
    }

    /**
     * Locally sets ccpaConsentGranted but may wait if not initialized to apply the changes.
     *
     * @param hasGivenCcpaConsent True if the user has granted consent, false otherwise.
     */
    internal fun setCcpaConsent(hasGivenCcpaConsent: Boolean) {
        ccpaConsentGranted = hasGivenCcpaConsent
        checkAndRun(::runCcpaConsentTask)
    }

    private fun runCcpaConsentTask(
        context: Context,
        privacyController: PrivacyController,
    ) {
        (ccpaConsentGranted ?: privacyController.ccpaConsent).let { ccpaConsentGranted ->
            privacyController.ccpaConsent = ccpaConsentGranted
            partnerController.setCcpaConsent(
                context,
                ccpaConsentGranted,
                if (ccpaConsentGranted == true) {
                    PrivacyController.PrivacyString.GRANTED.consentString
                } else {
                    PrivacyController.PrivacyString.DENIED.consentString
                },
                partnerConsents,
            )
        }
    }

    /**
     * Locally sets isSubjectToCoppa but may wait if not initialized to apply the changes.
     *
     * @param isSubject True if the user is subject to COPPA, false otherwise.
     */
    internal fun setSubjectToCoppa(isSubject: Boolean) {
        isSubjectToCoppa = isSubject
        checkAndRun(::runSubjectToCoppaTask)
    }

    private fun runSubjectToCoppaTask(
        context: Context,
        privacyController: PrivacyController,
    ) {
        (isSubjectToCoppa ?: privacyController.coppa)?.let { isSubject ->
            privacyController.coppa = isSubject
            partnerController.setUserSubjectToCoppa(context, isSubject)
        }
    }

    /**
     * Get the PartnerConsents object. Also initializes the privacyController if necessary and
     * updates partner consents from disk.
     */
    internal fun getPartnerConsents(context: Context): PartnerConsents {
        val localPrivacyController = privacyController ?: PrivacyController(context, partnerConsents)
        localPrivacyController.updatePartnerConsentsFromDisk()
        return partnerConsents
    }

    /**
     * Checks context and privacy controller to see if they are null. If either is, do nothing.
     * Otherwise, run the function passed in.
     */
    private fun checkAndRun(function: (context: Context, privacyController: PrivacyController) -> Unit) {
        val context = appContext
        val privacyController = privacyController
        if (context == null || privacyController == null) {
            LogController.d("Delaying $function until SDK initialized.")
            return
        }
        function(context, privacyController)
    }

    companion object {
        internal const val CHARTBOOST_MEDIATION_INTERNAL_SHARED_PREFS = "CBM_INTERNAL"
        internal const val SERVER_LOG_LEVEL_OVERRIDE = "cbm_internal_server_log_level_override"
    }
}
