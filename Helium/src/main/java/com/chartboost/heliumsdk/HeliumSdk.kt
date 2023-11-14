/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.content.Context
import android.content.pm.ApplicationInfo
import com.chartboost.heliumsdk.ad.ChartboostMediationAdLoadRequest
import com.chartboost.heliumsdk.ad.ChartboostMediationFullscreenAd
import com.chartboost.heliumsdk.ad.ChartboostMediationFullscreenAdListener
import com.chartboost.heliumsdk.ad.ChartboostMediationFullscreenAdLoadListener
import com.chartboost.heliumsdk.ad.ChartboostMediationFullscreenAdLoadResult
import com.chartboost.heliumsdk.domain.AdapterInfo
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.utils.Environment.fetchUserAgent
import com.chartboost.heliumsdk.utils.LogController
import com.chartboost.heliumsdk.utils.LogController.w
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * Core logic for the Chartboost Mediation SDK.
 */
class HeliumSdk private constructor(
) {
    interface HeliumSdkListener {
        fun didInitialize(error: Error?)
    }

    /**
     * @suppress
     *
     * Collection of ChartboostMediation initialization statuses.
     */
    enum class ChartboostMediationInitializationStatus {
        IDLE,
        INITIALIZING,
        INITIALIZED
    }

    companion object {
        internal val chartboostMediationInternal = ChartboostMediationInternal()

        /**
         * Get the PartnerConsents object to be able to set consent on a per-partner basis.
         * Setting consent here will override the GDPR and CCPA consent for a particular partner.
         *
         * @param context The Android context.
         */
        @JvmStatic
        fun getPartnerConsents(context: Context): PartnerConsents{
            return chartboostMediationInternal.getPartnerConsents(context)
        }

        @JvmStatic
        fun getVersion(): String {
            return BuildConfig.CHARTBOOST_MEDIATION_VERSION
        }

        /**
         * Starts the Helium SDK. Call this before using any other SDK components.
         * This method can only be run once
         *
         * @param context Recommended to be an activity context.
         * @param appId Helium appID from helium dashboard
         * @param appSignature Chartboost Mediation appSignature from helium dashboard
         * @param options Chartboost Mediation initialization options, if any
         * @param heliumSdkListener Callback when the SDK finished starting
         */
        @JvmStatic
        fun start(
            context: Context,
            appId: String,
            appSignature: String,
            options: HeliumInitializationOptions? = null,
            heliumSdkListener: HeliumSdkListener?
        ) {
            CoroutineScope(Main).launch(CoroutineExceptionHandler { _, exception ->
                heliumSdkListener?.didInitialize(Error("Failed to initialize Chartboost Mediation: $exception}"))
            }) {
                chartboostMediationInternal.initialize(context, appId, appSignature, options).fold({
                    LogController.d("Chartboost Mediation ${getVersion()} initialized successfully")
                    heliumSdkListener?.didInitialize(null)
                }, {
                    LogController.e("Chartboost Mediation failed to initialize. $it")
                    if (it is ChartboostMediationAdException && it.chartboostMediationError == ChartboostMediationError.CM_INITIALIZATION_FAILURE_INITIALIZATION_IN_PROGRESS) {
                        heliumSdkListener?.didInitialize(Error("Start attempt already ongoing"))
                    } else {
                        heliumSdkListener?.didInitialize(Error("Failed to initialize Chartboost Mediation: $it}"))
                    }
                })
                // Wait for Helium to finish initializing (whether it succeeds or fails) before fetching
                // the user agent.
                fetchUserAgent()
            }
        }

        /**
         * Indicates that the user is subject to COPPA (Children's Online Privacy Protection Act).
         * For more information about COPPA:
         *
         * @param isSubject True if the user is subject to COPPA, false otherwise.
         * @see [Chartboost Helpsite on COPPA](https://answers.chartboost.com/en-us/articles/115001488494)
         */
        @JvmStatic
        fun setSubjectToCoppa(isSubject: Boolean) {
            chartboostMediationInternal.setSubjectToCoppa(isSubject)
        }

        /**
         * Indicates that the user is subject to GDPR (General Data Protection Regulation).
         * For more information about GDPR:
         *
         * @param isSubjectToGdpr True if the user is subject to GDPR, false otherwise.
         * @see [Chartboost Helpsite on GDPR](https://answers.chartboost.com/en-us/articles/115001489613)
         */
        @JvmStatic
        fun setSubjectToGDPR(isSubjectToGdpr: Boolean) {
            chartboostMediationInternal.setSubjectToGdpr(isSubjectToGdpr)
        }

        /**
         * Indicates that the GDPR-applicable user has granted consent to the collection of Personally Identifiable Information.
         * For more information about GDPR:
         *
         * @param hasGivenGdprConsent True if the user has granted consent, false otherwise.
         * @see [Chartboost Helpsite on GDPR](https://answers.chartboost.com/en-us/articles/115001489613)
         */
        @JvmStatic
        fun setUserHasGivenConsent(hasGivenGdprConsent: Boolean) {
            chartboostMediationInternal.setUserHasGivenConsent(hasGivenGdprConsent)
        }

        /**
         * Indicates that the CCPA-applicable user has granted consent to the collection of Personally Identifiable Information.
         * For more information about CCPA:
         *
         * @param hasGivenCcpaConsent True if the user has granted consent, false otherwise.
         * @see [Chartboost Helpsite on CCPA](https://answers.chartboost.com/en-us/articles/115001490031)
         */
        @JvmStatic
        fun setCCPAConsent(hasGivenCcpaConsent: Boolean) {
            chartboostMediationInternal.setCcpaConsent(hasGivenCcpaConsent)
        }

        /**
         * Returns the activity context if available.
         * Otherwise, returns the application context if initialized.
         */
        @JvmStatic
        val context: Context?
            get() {
                return chartboostMediationInternal.weakActivityContext.get() ?: chartboostMediationInternal.appContext
            }

        @JvmStatic
        fun getAppId() = chartboostMediationInternal.appId

        @JvmStatic
        fun getAppSignature() = chartboostMediationInternal.appSignature

        @JvmStatic
        fun setDebugMode(debugMode: Boolean) {
            LogController.debugMode = debugMode
        }

        @JvmStatic
        fun getTestMode() = if (chartboostMediationInternal.testMode) 1 else 0

        @JvmStatic
        fun setTestMode(mode: Boolean) {
            val appContext = chartboostMediationInternal.appContext

            when {
                appContext == null -> w("setTestMode() failed. Initialize the SDK first.")
                !isDebuggable(appContext) -> w("Set the application to debug mode to use setTestMode().")
                else -> {
                    chartboostMediationInternal.testMode = mode
                    w("The Chartboost Mediation SDK is set to ONLY be requesting ${if (mode) "test" else "live"} ads.")
                }
            }
        }

        /**
         * Subscribe to impression level revenue data.
         *
         * @param observer To receive the ILRD data
         */
        @JvmStatic
        fun subscribeIlrd(observer: HeliumIlrdObserver) {
            chartboostMediationInternal.ilrd.subscribe(observer)
        }

        /**
         * Unsubscribe to impression level revenue data.
         *
         * @param observer To receive the ILRD data
         */
        @JvmStatic
        fun unsubscribeIlrd(observer: HeliumIlrdObserver) {
            chartboostMediationInternal.ilrd.unsubscribe(observer)
        }

        /**
         * Subscribe to partner initialization results.
         *
         * @param observer To receive the initialization results
         */
        @JvmStatic
        fun subscribeInitializationResults(observer: PartnerInitializationResultsObserver) {
            chartboostMediationInternal.partnerInitializationResults.subscribe(observer)
        }

        /**
         * Set the user identifier used for rewarded callbacks.
         *
         * @param userIdentifier The user identifier.
         */
        @JvmStatic
        fun setUserIdentifier(userIdentifier: String?) {
            chartboostMediationInternal.userIdentifier = userIdentifier
        }

        /**
         * Gets the user identifier previously set.
         */
        @JvmStatic
        fun getUserIdentifier() = chartboostMediationInternal.userIdentifier

        /**
         * Specifies to the Helium SDK the Game Engine environment.
         *
         * @param name game engine's name
         * @param version game engine's version
         */
        @JvmStatic
        fun setGameEngine(name: String?, version: String?) {
            chartboostMediationInternal.gameEngineName = name
            chartboostMediationInternal.gameEngineVersion = version
        }

        /**
         * Gets the Game Engine name.
         */
        @JvmStatic
        fun getGameEngineName() = chartboostMediationInternal.gameEngineName

        /**
         * Gets the Game Engine version.
         */
        @JvmStatic
        fun getGameEngineVersion() = chartboostMediationInternal.gameEngineVersion

        /**
         * Specifies if oversized banner ads should be discarded.
         *
         * @param shouldDrop whether or not the oversized banners should be discarded
         */
        @JvmStatic
        fun setShouldDiscardOversizedAds(shouldDrop: Boolean) {
            chartboostMediationInternal.shouldDiscardOversizedAds = shouldDrop
        }

        /**
         * Indicates if oversized banner ads will be dropped.
         *
         * @return whether or not the oversized banners will be discarded
         */
        @JvmStatic
        fun isDiscardOversizedAdsEnabled(): Boolean {
            return chartboostMediationInternal.shouldDiscardOversizedAds
        }

        /**
         * Gets the adapter info from the PartnerController instance.
         *
         * @return a List of known AdapterInfo if Helium is initialized; empty otherwise.
         */
        @JvmStatic
        val adapterInfo: List<AdapterInfo>
            get() {
                return chartboostMediationInternal.partnerController.allAdapterInfo
            }

        /**
         * Load a fullscreen ad. This method is designed to be called from Java.
         *
         * @param context The [Context] to use for loading the ad.
         * @param request The publisher-supplied [ChartboostMediationAdLoadRequest] containing relevant details to load the ad.
         * @param listener The [ChartboostMediationFullscreenAdListener] to notify of the ad lifecycle events.
         * @param adLoadListener The [ChartboostMediationFullscreenAdLoadListener] to notify of the ad load event.
         */
        @JvmStatic
        fun loadFullscreenAdFromJava(
            context: Context,
            request: ChartboostMediationAdLoadRequest,
            listener: ChartboostMediationFullscreenAdListener,
            adLoadListener: ChartboostMediationFullscreenAdLoadListener
        ) {
            CoroutineScope(Main).launch {
                val result = loadFullscreenAd(context, request, listener)
                adLoadListener.onAdLoaded(result)
            }
        }

        /**
         * Load a fullscreen ad.
         *
         * @param context The [Context] to use for loading the ad.
         * @param request The publisher-supplied [ChartboostMediationAdLoadRequest] containing relevant details to load the ad.
         * @param listener The [ChartboostMediationFullscreenAdListener] to notify of ad events.
         *
         * @return The [ChartboostMediationFullscreenAdLoadResult] containing the result of the ad load.
         */
        @JvmStatic
        suspend fun loadFullscreenAd(
            context: Context,
            request: ChartboostMediationAdLoadRequest,
            listener: ChartboostMediationFullscreenAdListener
        ): ChartboostMediationFullscreenAdLoadResult {
            return ChartboostMediationFullscreenAd.loadFullscreenAd(context, request, chartboostMediationInternal.adController, listener)
        }

        /**
         * Check if the application is in debug mode.
         */
        private fun isDebuggable(context: Context): Boolean {
            return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }
    }
}
