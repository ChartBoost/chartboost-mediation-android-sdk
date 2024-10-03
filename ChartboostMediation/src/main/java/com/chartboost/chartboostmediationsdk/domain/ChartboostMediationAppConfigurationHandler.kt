/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.content.Context
import com.chartboost.chartboostmediationsdk.ChartboostMediationInternal
import com.chartboost.chartboostmediationsdk.ChartboostMediationPreinitializationConfiguration
import com.chartboost.chartboostmediationsdk.controllers.PartnerController
import com.chartboost.chartboostmediationsdk.domain.EventResult.SdkInitializationResult.InitResult1B
import com.chartboost.chartboostmediationsdk.domain.MetricsManager.postMetricsData
import com.chartboost.chartboostmediationsdk.domain.MetricsManager.postMetricsDataForFailedEvent
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.core.ChartboostCore
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentValue
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @suppress
 *
 * This class handles mediation partner configuration changes during Chartboost Mediation initialization.
 *
 * @param context The current [Context] with which to handle app configuration changes.
 * @param chartboostMediationPreinitializationConfiguration Options with which to initialize Chartboost Mediation and partners.
 */
class ChartboostMediationAppConfigurationHandler(
    private val context: Context?,
    private val chartboostMediationPreinitializationConfiguration: ChartboostMediationPreinitializationConfiguration?,
) {
    companion object {
        // To prevent overloading the server on failed init, let's send the initialization event once.
        private var shouldSendInitializationMetrics = AtomicBoolean(false)
    }

    /**
     * Handle configuration changes during Chartboost Mediation initialization.
     *
     * @param partnerController The [PartnerController] instance.
     */
    suspend fun handleConfigurationChange(partnerController: PartnerController): ChartboostMediationError? {
        val parsingError = AppConfigStorage.parsingError
        val validCachedConfigExists = AppConfigStorage.validCachedConfigExists

        if (AppConfigStorage.shouldDisableSdk) {
            LogController.e("Failed to initialize the Chartboost Mediation SDK. SDK is disabled.")
            val mediationDisabled = ChartboostMediationError.InitializationError.Disabled
            if (!shouldSendInitializationMetrics.getAndSet(true)) {
                postMetricsData(
                    setOf(
                        Metrics(
                            null,
                            Endpoints.Event.INITIALIZATION,
                        ),
                    ),
                    eventResult =
                        if (validCachedConfigExists) {
                            EventResult.SdkInitializationResult.InitResult2C
                        } else {
                            EventResult.SdkInitializationResult.InitResult1C
                        },
                )
            }
            return mediationDisabled
        }

        if (parsingError == null || validCachedConfigExists) {
            val context =
                context ?: run {
                    LogController.e("Failed to initialize mediation partners. Context is null.")
                    return ChartboostMediationError.InitializationError.Aborted
                }
            updateServerLogLevelOverride(context)

            val skippedPartnerIds = chartboostMediationPreinitializationConfiguration?.skippedPartnerIds.orEmpty()
            val consents = ChartboostCore.consent.consents
            val isUserUnderage = ChartboostCore.analyticsEnvironment.isUserUnderage
            val partnerConfigMap = buildPartnerConfigMap(consents, isUserUnderage)

            addReferenceAdapterIfNeeded(partnerConfigMap, consents, isUserUnderage)
            return setUpPartnerAdapters(partnerController, context, partnerConfigMap, skippedPartnerIds)
        } else {
            val sdkInitializationResult = InitResult1B(parsingError)
            val mediationError = ChartboostMediationError.InitializationError.InvalidAppConfig
            postMetricsDataForFailedEvent(
                partner = null,
                event = Endpoints.Event.INITIALIZATION,
                auctionIdentifier = null,
                chartboostMediationError = mediationError,
                chartboostMediationErrorMessage = mediationError.message,
                loadId = null,
                eventResult = sdkInitializationResult,
            )
            return mediationError
        }
    }

    /**
     * Build a map of partner configurations from the partners list.
     *
     *       @param consents The map of partner consents.
     *       @param isUserUnderage Whether the user is underage.
     * @return A mutable map of partner configurations.
     */
    private fun buildPartnerConfigMap(
        consents: Map<ConsentKey, ConsentValue>,
        isUserUnderage: Boolean?,
    ): MutableMap<String, PartnerConfiguration> =
        AppConfigStorage.partners
            .associate { (partnerId, credentials) ->
                partnerId to PartnerConfiguration(credentials, consents, isUserUnderage)
            }.toMutableMap()

    /**
     * Add the Reference adapter to the partner configuration map if it's not present.
     *
     * @param partnerConfigMap The partner configuration map to be checked and updated.
     * @param consents The map of partner consents.
     * @param isUserUnderage Whether the user is underage.
     */
    private fun addReferenceAdapterIfNeeded(
        partnerConfigMap: MutableMap<String, PartnerConfiguration>,
        consents: Map<ConsentKey, ConsentValue>,
        isUserUnderage: Boolean?,
    ) {
        partnerConfigMap.getOrPut("reference") {
            PartnerConfiguration(
                consents = consents,
                isUserUnderage = isUserUnderage,
            )
        }
    }

    /**
     * Set up partner adapters and handles their initialization completion.
     *
     * @param partnerController The partner controller responsible for setting up adapters.
     * @param context The context used for setting up adapters.
     * @param partnerConfigMap The partner configuration map.
     * @param skippedPartnerIds The set of partner IDs to be skipped.
     */
    private suspend fun setUpPartnerAdapters(
        partnerController: PartnerController,
        context: Context,
        partnerConfigMap: MutableMap<String, PartnerConfiguration>,
        skippedPartnerIds: Set<String>,
    ): ChartboostMediationError? {
        return suspendCancellableCoroutine { continuation ->
            // Using manually tracked resumes since the coroutine seems to sometimes still be
            // active when something has gone wrong.
            val coroutineResumed = AtomicBoolean(false)
            partnerController.setUpAdapters(
                context,
                partnerConfigMap,
                AppConfigStorage.adapterClassPaths,
                skippedPartnerIds,
                onPartnerInitializationComplete = { error ->
                    if (coroutineResumed.compareAndSet(false, true)) {
                        continuation.resumeWith(Result.success(error))
                    }
                },
            )
        }
    }

    /**
     * If the server log level exists, set the log level. If not, remove the entry from shared preferences.
     * @param context The app Context to get shared preferences
     */
    private fun updateServerLogLevelOverride(context: Context) {
        val preferences =
            context
                .getSharedPreferences(
                    ChartboostMediationInternal.CHARTBOOST_MEDIATION_INTERNAL_SHARED_PREFS,
                    Context.MODE_PRIVATE,
                ).edit()
        AppConfigStorage.serverLogLevelOverride?.let {
            LogController.serverLogLevelOverride = it
            preferences
                .putString(ChartboostMediationInternal.SERVER_LOG_LEVEL_OVERRIDE, it.name)
                .apply()
        } ?: run {
            LogController.serverLogLevelOverride = null
            preferences.remove(ChartboostMediationInternal.SERVER_LOG_LEVEL_OVERRIDE).apply()
        }
    }
}
