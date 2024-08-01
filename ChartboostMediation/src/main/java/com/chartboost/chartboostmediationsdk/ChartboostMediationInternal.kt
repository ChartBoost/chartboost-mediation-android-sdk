/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationFullscreenAdQueueManager
import com.chartboost.chartboostmediationsdk.controllers.*
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAdException
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAppConfigurationHandler
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.domain.LoadRateLimiter
import com.chartboost.chartboostmediationsdk.utils.BackgroundTimeMonitor
import com.chartboost.chartboostmediationsdk.utils.FullscreenAdShowingState
import com.chartboost.chartboostmediationsdk.utils.LogController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext

/**
 * Holds all the internal logic for Chartboost Mediation.
 */
internal class ChartboostMediationInternal(
    internal val partnerController: PartnerController = PartnerController(),
    internal val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    internal var adController: AdController? = null
    internal var appId: String? = null
    internal var privacyController: PrivacyController? = null
    internal var testMode = false
    internal var shouldDiscardOversizedAds = false

    internal val fullscreenAdShowingState = FullscreenAdShowingState()
    internal val ilrd = Ilrd()
    internal val partnerAdapterInitializationResults = PartnerAdapterInitializationResults()

    internal var initializationStatus = ChartboostMediationSdk.ChartboostMediationInitializationStatus.IDLE

    internal suspend fun initialize(
        context: Context,
        appId: String,
        options: ChartboostMediationPreinitializationConfiguration?,
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

            if (initializationStatus == ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED) {
                LogController.d("Chartboost Mediation already initialized.")
                return@withContext Result.success(Unit)
            }
            if (initializationStatus == ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZING) {
                return@withContext Result.failure(
                    ChartboostMediationAdException(
                        ChartboostMediationError.InitializationError.InProgress,
                    ),
                )
            }
            initializationStatus = ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZING
            LogController.d("Chartboost Mediation initialize called with SDK Key: $appId")

            this@ChartboostMediationInternal.appId = appId
            val localPrivacyController = privacyController ?: PrivacyController(context)
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

            // Grab the app configuration from the server or from local if server is unavailable
            AppConfigController(context.applicationContext, ioDispatcher).get()

            // Handle processing the config and also initialize all partners
            val error =
                ChartboostMediationAppConfigurationHandler(
                    context,
                    options,
                ).handleConfigurationChange(partnerController)

            if (error != null) {
                initializationStatus = ChartboostMediationSdk.ChartboostMediationInitializationStatus.IDLE
                ChartboostMediationFullscreenAdQueueManager.autoStartQueues(false)
                return@withContext Result.failure(ChartboostMediationAdException(error))
            }

            initializationStatus = ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED
            ChartboostMediationFullscreenAdQueueManager.autoStartQueues(true)
            Result.success(Unit)
        }
    }

    companion object {
        internal const val CHARTBOOST_MEDIATION_INTERNAL_SHARED_PREFS = "CBM_INTERNAL"
        internal const val SERVER_LOG_LEVEL_OVERRIDE = "cbm_internal_server_log_level_override"
    }
}
