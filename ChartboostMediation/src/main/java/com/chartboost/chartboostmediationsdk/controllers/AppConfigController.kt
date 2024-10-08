/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.controllers

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.domain.AppConfig
import com.chartboost.chartboostmediationsdk.domain.AppConfigStorage
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.domain.MetricsError
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking.INIT_HASH_HEADER_KEY
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/**
 * @suppress
 *
 * This class manages getting and setting the various configurations of the Chartboost Mediation SDK.
 *
 * @property appContext The Application Context.
 */
class AppConfigController(
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER = "HELIUM_CONFIG_IDENTIFIER"
        private const val INIT_HASH_KEY = "INIT_HASH"
    }

    /**
     * The hashed value of this configuration.
     */
    private var initHash: String =
        appContext
            .getSharedPreferences(CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER, Context.MODE_PRIVATE)
            .getString(INIT_HASH_KEY, "") ?: ""
        set(value) {
            field = value
            appContext
                .getSharedPreferences(CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER, Context.MODE_PRIVATE)
                .edit()
                .putString(
                    INIT_HASH_KEY,
                    value,
                ).apply()
        }

    /**
     * Get the app config to use. Start with the locally stored config, but also get the server one
     * to keep the SharedPreferences up-to-date.
     */
    suspend fun get() {
        val sharedPreferences =
            appContext.getSharedPreferences(
                CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER,
                Context.MODE_PRIVATE,
            )

        /**
         * Check validity of the local config if it exists and reset initHash if invalid
         * before fetching server config
         */
        val localConfig = getLocalConfig(sharedPreferences)
        if (!localConfig.hasMinimumAdapters() || !localConfig.hasMinimumCredentials()) {
            initHash = ""
        }

        if (!getServerConfig(sharedPreferences)) {
            processConfig(localConfig)
        }
    }

    /**
     * Get the locally stored config.
     *
     * @param sharedPreferences The SharedPreferences from which to get the config.
     *
     * @return The locally stored config String, or null if it doesn't exist.
     */
    private fun getLocalConfig(sharedPreferences: SharedPreferences): AppConfig {
        try {
            return sharedPreferences.getString(CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER, null)?.let {
                AppConfig.fromJsonString(it)
            } ?: run {
                AppConfigStorage.validCachedConfigExists = false
                AppConfig()
            }
        } catch (exception: SerializationException) {
            // Our cached config was unparseable. Reset the config hash for next launch
            LogController.e("Exception raised parsing local config: ${exception.message}")
            initHash = ""
            AppConfigStorage.validCachedConfigExists = false
        }

        return AppConfig()
    }

    /**
     * Get the config from the server and process it if we get a successful response.
     *
     * @param sharedPreferences The SharedPreferences from which to get the config.
     * @return True if the server config was successful. False otherwise.
     */
    private suspend fun getServerConfig(sharedPreferences: SharedPreferences): Boolean {
        return withContext(ioDispatcher) {
            val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

            when (
                val result =
                    ChartboostMediationNetworking.getAppConfig(ChartboostMediationSdk.getAppId() ?: "", initHash, appSetId)
            ) {
                is ChartboostMediationNetworkingResult.Success -> {
                    if (result.body != null) {
                        result.headers[INIT_HASH_HEADER_KEY]?.let {
                            initHash = it
                        }
                        processServerConfig(sharedPreferences, result.body)
                        return@withContext true
                    }
                }

                is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                    val cmError = ChartboostMediationError.InitializationError.InternalError
                    val exceptionMessage =
                        result.exception.message?.split('\n', limit = 2)?.let {
                            it[0]
                        } ?: ""

                    val malformedJson =
                        result.exception.message?.substring(exceptionMessage.length)?.let {
                            if (it.startsWith("\nJSON input: ")) {
                                it.substring("\nJSON input: ".length)
                            } else {
                                it
                            }
                        } ?: ""

                    AppConfigStorage.parsingError =
                        MetricsError.JsonParseError(
                            cmError,
                            result.exception,
                            exceptionMessage,
                            malformedJson,
                        )

                    failServerConfig(
                        ChartboostMediationError.InitializationError.InternalError,
                        result.exception,
                    )
                }

                is ChartboostMediationNetworkingResult.Failure -> {
                    failServerConfig(result.error)
                }
            }

            return@withContext false
        }
    }

    /**
     * Process the config from the server by storing it in the SharedPreferences for the next local
     * config fetch and then processing it.
     *
     * @param sharedPreferences The SharedPreferences to which to store the config.
     * @param appConfig The config from the server.
     */
    private fun processServerConfig(
        sharedPreferences: SharedPreferences,
        appConfig: AppConfig,
    ) {
        val editor = sharedPreferences.edit()
        editor.putString(CHARTBOOST_MEDIATION_CONFIG_IDENTIFIER, appConfig.toJsonString())
        editor.apply()
        processConfig(appConfig)
    }

    /**
     * Handle errors if we get a failure response from the server.
     *
     * @param error The error from the server.
     * @param exception The optional exception thrown during the failure.
     */
    private fun failServerConfig(
        error: ChartboostMediationError,
        exception: Exception? = null,
    ) {
        if (error == ChartboostMediationError.InitializationError.InternalError) {
            LogController.d(
                "Failed to parse retrieved config with error: $error due to " +
                    (exception?.message ?: "<no message provided>"),
            )
        } else {
            LogController.d(
                "Failed to retrieve config from server with error: $error. " +
                    "This is normal when no updates to the config are necessary.",
            )
        }
    }

    /**
     * Parse the config, update relevant fields, and notify Chartboost Mediation.
     *
     * @param appConfig The config to process.
     */
    private fun processConfig(appConfig: AppConfig) {
        AppConfigStorage.updateFields(appConfig)
        if (AppConfigStorage.parsingError != null) {
            initHash = ""
        }
    }
}
