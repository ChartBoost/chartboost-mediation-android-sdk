/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.content.Context
import com.chartboost.heliumsdk.HeliumInitializationOptions
import com.chartboost.heliumsdk.controllers.PartnerController
import com.chartboost.heliumsdk.domain.EventResult.SdkInitializationResult.InitResult1B
import com.chartboost.heliumsdk.network.Endpoints
import com.chartboost.heliumsdk.utils.LogController
import com.chartboost.heliumsdk.domain.MetricsManager.postMetricsDataForFailedEvent
import kotlin.coroutines.suspendCoroutine

/**
 * @suppress
 *
 * This class handles mediation partner configuration changes during Chartboost Mediation initialization.
 *
 * @param context The current [Context] with which to handle app configuration changes.
 * @param chartboostMediationInitializationOptions Options with which to initialize Chartboost Mediation and partners.
 */
class ChartboostMediationAppConfigurationHandler(
    private val context: Context?,
    private val chartboostMediationInitializationOptions: HeliumInitializationOptions?,
) {
    /**
     * Handle configuration changes during Chartboost Mediation initialization.
     *
     * @param partnerController The [PartnerController] instance.
     */
    suspend fun handleConfigurationChange(partnerController: PartnerController): ChartboostMediationError? {
        val parsingError = AppConfigStorage.parsingError
        val validCachedConfigExists = AppConfigStorage.validCachedConfigExists

        if (parsingError == null || validCachedConfigExists) {
            val context = context ?: run {
                LogController.e("Failed to initialize mediation partners. Context is null.")
                return ChartboostMediationError.CM_INITIALIZATION_FAILURE_ABORTED
            }

            val initializationOptions = chartboostMediationInitializationOptions
            val skippedPartnerIds = initializationOptions?.skippedPartnerIds.orEmpty()
            val partnerConfigMap = buildPartnerConfigMap()

            addReferenceAdapterIfNeeded(partnerConfigMap)
            return setUpPartnerAdapters(partnerController, context, partnerConfigMap, skippedPartnerIds)
        } else {
            val sdkInitializationResult = InitResult1B(parsingError)
            val mediationError = ChartboostMediationError.CM_INITIALIZATION_FAILURE_INVALID_APP_CONFIG
            postMetricsDataForFailedEvent(
                partner = null,
                event = Endpoints.Sdk.Event.INITIALIZATION,
                auctionIdentifier = null,
                chartboostMediationError = mediationError,
                chartboostMediationErrorMessage =  mediationError.message,
                loadId = null,
                eventResult = sdkInitializationResult
            )
            return mediationError
        }
    }

    /**
     * Build a map of partner configurations from the partners list.
     *
     * @return A mutable map of partner configurations.
     */
    private fun buildPartnerConfigMap(): MutableMap<String, PartnerConfiguration> {
        return AppConfigStorage.partners.associate { (partnerId, credentials) ->
            partnerId to PartnerConfiguration(credentials)
        }.toMutableMap()
    }

    /**
     * Add the Reference adapter to the partner configuration map if it's not present.
     *
     * @param partnerConfigMap The partner configuration map to be checked and updated.
     */
    private fun addReferenceAdapterIfNeeded(partnerConfigMap: MutableMap<String, PartnerConfiguration>) {
        partnerConfigMap.getOrPut("reference") { PartnerConfiguration() }
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
        skippedPartnerIds: Set<String>
    ): ChartboostMediationError? {
        return suspendCoroutine { continuation ->
            partnerController.setUpAdapters(
                context,
                partnerConfigMap,
                AppConfigStorage.adapterClassPaths,
                skippedPartnerIds,
                onPartnerInitializationComplete = { error ->
                    continuation.resumeWith(Result.success(error))
                }
            )
        }

    }
}
