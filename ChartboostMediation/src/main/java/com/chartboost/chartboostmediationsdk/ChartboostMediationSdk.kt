/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ProcessLifecycleOwner
import com.chartboost.chartboostmediationsdk.domain.AdapterInfo
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAdException
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.utils.Environment
import com.chartboost.chartboostmediationsdk.utils.LifecycleStatusObserver
import com.chartboost.chartboostmediationsdk.utils.LogController

/**
 * Core logic for the Chartboost Mediation SDK.
 */
class ChartboostMediationSdk private constructor() {
    /**
     * @suppress
     *
     * Collection of ChartboostMediation initialization statuses.
     */
    enum class ChartboostMediationInitializationStatus {
        IDLE,
        INITIALIZING,
        INITIALIZED,
    }

    companion object {
        /**
         * The Chartboost Core module ID for Chartboost Mediation.
         */
        const val CORE_MODULE_ID = "chartboost_mediation"

        internal var chartboostMediationInternal = ChartboostMediationInternal()
        private val lifecycleStatusObserver = LifecycleStatusObserver()

        /**
         * Chartboost Mediation preinitialization configuration. This only is used at initialization.
         */
        @JvmStatic
        var preinitializationConfiguration: ChartboostMediationPreinitializationConfiguration? =
            null
            private set

        /**
         * Sets the Chartboost Mediation preinitialization configuration. Setting this after
         * initialization does nothing and returns an exception.
         */
        @JvmStatic
        fun setPreinitializationConfiguration(
            configuration: ChartboostMediationPreinitializationConfiguration?,
        ): ChartboostMediationAdException? {
            if (chartboostMediationInternal.initializationStatus != ChartboostMediationInitializationStatus.IDLE) {
                LogController.w("Setting the preinitialization configuration after initialization does not do anything.")
                return ChartboostMediationAdException(ChartboostMediationError.OtherError.PreinitializationActionFailed)
            }
            preinitializationConfiguration = configuration
            return null
        }

        @JvmStatic
        fun getVersion(): String = BuildConfig.CHARTBOOST_MEDIATION_VERSION

        /**
         * Initializes the Chartboost Mediation SDK. Call this before using any other SDK components.
         * This method can only be run once
         *
         * @param context Recommended to be an activity context.
         * @param appId Chartboost Mediation appID from the Chartboost Mediation dashboard
         */
        @JvmStatic
        internal suspend fun initialize(
            context: Context,
            appId: String,
        ): Result<Unit> {
            Environment.appContext = context.applicationContext
            chartboostMediationInternal
                .initialize(context, appId, preinitializationConfiguration)
                .fold({
                    LogController.d("Chartboost Mediation ${getVersion()} initialized successfully")
                    ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleStatusObserver)
                    return Result.success(Unit)
                }, {
                    LogController.e("Chartboost Mediation failed to initialize. $it")
                    if (it is ChartboostMediationAdException &&
                        it.chartboostMediationError == ChartboostMediationError.InitializationError.InProgress
                    ) {
                        LogController.w("Start attempt already ongoing")
                    } else {
                        LogController.w("Failed to initialize Chartboost Mediation: $it")
                    }
                    return Result.failure(it)
                })
        }

        @JvmStatic
        fun getAppId() = chartboostMediationInternal.appId

        /**
         * The log level. Anything of that log level and lower will be emitted.
         * Set this to [LogController.LogLevel.DISABLED] for no logs
         */
        @JvmStatic
        var logLevel: LogController.LogLevel
            @JvmStatic
            get() = LogController.logLevel

            @JvmStatic
            set(value) {
                LogController.logLevel = value
            }

        @JvmStatic
        fun getTestMode() = if (chartboostMediationInternal.testMode) 1 else 0

        @JvmStatic
        fun setTestMode(
            context: Context,
            mode: Boolean,
        ) {
            when {
                !isDebuggable(context) -> LogController.w("Set the application to debug mode to use setTestMode().")

                else -> {
                    chartboostMediationInternal.testMode = mode
                    LogController.w("The Chartboost Mediation SDK is set to ONLY be requesting ${if (mode) "test" else "live"} ads.")
                }
            }
        }

        /**
         * Subscribe to impression level revenue data.
         *
         * @param observer To receive the ILRD data
         */
        @JvmStatic
        fun subscribeIlrd(observer: ChartboostMediationIlrdObserver) {
            chartboostMediationInternal.ilrd.subscribe(observer)
        }

        /**
         * Unsubscribe to impression level revenue data.
         *
         * @param observer To receive the ILRD data
         */
        @JvmStatic
        fun unsubscribeIlrd(observer: ChartboostMediationIlrdObserver) {
            chartboostMediationInternal.ilrd.unsubscribe(observer)
        }

        /**
         * Subscribe to partner adapter initialization results.
         *
         * @param observer To receive the partner adapter initialization results
         */
        @JvmStatic
        fun subscribePartnerAdapterInitializationResults(observer: PartnerAdapterInitializationResultsObserver) {
            chartboostMediationInternal.partnerAdapterInitializationResults.subscribe(observer)
        }

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
        fun isDiscardOversizedAdsEnabled(): Boolean = chartboostMediationInternal.shouldDiscardOversizedAds

        /**
         * Gets the adapter info from the PartnerController instance.
         *
         * @return a List of known AdapterInfo if Chartboost Mediation is initialized; empty otherwise.
         */
        @JvmStatic
        val adapterInfo: List<AdapterInfo>
            get() {
                return chartboostMediationInternal.partnerController.allAdapterInfo
            }

        /**
         * Check if the application is in debug mode.
         */
        private fun isDebuggable(context: Context): Boolean = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
