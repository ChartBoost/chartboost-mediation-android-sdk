/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.app.Activity
import android.content.Context
import com.chartboost.heliumsdk.controllers.*
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.FullscreenAdShowingState
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Holds all the internal logic for Chartboost Mediation.
 */
internal class ChartboostMediationInternal {

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
    internal val partnerController = PartnerController()
    internal val partnerInitializationResults = PartnerInitializationResults()

    private var gdprApplies: Boolean? = null
    private var gdprConsentStatus = GdprConsentStatus.GDPR_CONSENT_UNKNOWN
    private var initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.IDLE

    /**
     * Store the pre-init value of COPPA until the SDK is initialized.
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
        options: HeliumInitializationOptions?
    ): Result<Unit> {
        return withContext(Main) {
            if (initializationStatus == HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZED) {
                this@ChartboostMediationInternal.weakActivityContext =
                    if (context is Activity) WeakReference(context) else weakActivityContext
                LogController.d("Chartboost Mediation already initialized. Potentially updating the activity")
                return@withContext Result.success(Unit)
            }
            if (initializationStatus == HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZING) {
                return@withContext Result.failure(
                    ChartboostMediationAdException(
                        ChartboostMediationError.CM_INITIALIZATION_FAILURE_INITIALIZATION_IN_PROGRESS
                    )
                )
            }
            initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZING
            LogController.d("Chartboost Mediation initialize called with SDK Key: $appId")

            this@ChartboostMediationInternal.appId = appId
            this@ChartboostMediationInternal.appSignature = appSignature
            this@ChartboostMediationInternal.weakActivityContext =
                if (context is Activity) WeakReference(context) else weakActivityContext
            this@ChartboostMediationInternal.appContext = context.applicationContext
            val localPrivacyController = PrivacyController(context)
            privacyController = localPrivacyController
            val bidController = BidController(partnerController)
            adController = AdController(
                bidController = bidController,
                partnerController = partnerController,
                privacyController = localPrivacyController,
                loadRateLimiter = LoadRateLimiter(),
                ilrd = ilrd
            )

            // Grab the app configuration from the server or from local if server is unavailable
            AppConfigController(context.applicationContext).get()

            // Handle processing the config and also initialize all partners
            val error = ChartboostMediationAppConfigurationHandler(
                context,
                options
            ).handleConfigurationChange(partnerController)

            if (error != null) {
                initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.IDLE
                return@withContext Result.failure(ChartboostMediationAdException(error))
            }

            Environment.startSession(context)
            runGdprConsentTask()
            runCcpaConsentTask()
            runSubjectToCoppaTask()
            initializationStatus = HeliumSdk.ChartboostMediationInitializationStatus.INITIALIZED
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
    fun setUserHasGivenConsent(hasGivenGdprConsent: Boolean) {
        gdprConsentStatus =
            if (hasGivenGdprConsent) GdprConsentStatus.GDPR_CONSENT_GRANTED else GdprConsentStatus.GDPR_CONSENT_DENIED
        checkAndRun(::runGdprConsentTask)
    }

    private fun runGdprConsentTask() {
        gdprApplies?.let { isSubjectToGdpr ->
            privacyController?.gdpr = if (isSubjectToGdpr) 1 else 0
            when (gdprConsentStatus) {
                GdprConsentStatus.GDPR_CONSENT_GRANTED -> privacyController?.userConsent = true
                GdprConsentStatus.GDPR_CONSENT_DENIED -> privacyController?.userConsent = false
                else -> {} // do nothing
            }
            appContext?.let { context ->
                partnerController.setGdpr(context, isSubjectToGdpr, gdprConsentStatus)
            }
        }
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

    private fun runCcpaConsentTask() {
        ccpaConsentGranted?.let { ccpaConsentGranted ->
            privacyController?.ccpaConsent = ccpaConsentGranted
            appContext?.let { context ->
                partnerController.setCcpaConsent(
                    context, ccpaConsentGranted,
                    if (ccpaConsentGranted) {
                        PrivacyController.PrivacyString.GRANTED.consentString
                    } else {
                        PrivacyController.PrivacyString.DENIED.consentString
                    }
                )
            }
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

    private fun runSubjectToCoppaTask() {
        isSubjectToCoppa?.let { isSubject ->
            privacyController?.coppa = isSubject
            appContext?.let { context ->
                partnerController.setUserSubjectToCoppa(context, isSubject)
            }
        }
    }

    /**
     * Checks context and privacy controller to see if they are null. If either is, do nothing.
     * Otherwise, run the function passed in.
     */
    private fun checkAndRun(function: () -> Unit) {
        val context = appContext
        val privacyController = privacyController
        if (context == null || privacyController == null) {
            LogController.d("Delaying $function until SDK initialized.")
            return
        }
        function()
    }
}
