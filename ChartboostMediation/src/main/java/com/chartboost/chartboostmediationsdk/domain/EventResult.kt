/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * @suppress
 */
sealed interface EventResult {
    fun getMetricsError(): MetricsError? = null

    sealed class SdkInitializationResult(
        val success: Boolean,
        val initResultCode: String,
    ) : EventResult {
        // No cached config
        // Response JSON parsed: init success, /event body JSON has fetched metrics array, no error
        object InitResult1A : SdkInitializationResult(
            success = true,
            initResultCode = "success_with_fetched_config",
        )

        // No cached config
        // Response JSON failed: init failure, /event body JSON does not have metrics array, has error
        class InitResult1B(
            val jsonParseError: MetricsError.JsonParseError,
        ) : SdkInitializationResult(
                success = false,
                initResultCode = "failure",
            ) {
            val error: Error = Error("No valid app config JSON available")

            override fun getMetricsError(): MetricsError? = jsonParseError
        }

        // Has cached config
        // Response JSON parsed: init success, /event body JSON has cached metrics array, no error
        object InitResult2A : SdkInitializationResult(
            success = true,
            initResultCode = "success_with_cached_config",
        )

        // Has cached config
        // Response JSON failed: init success,  /event body JSON has cached metrics array, has error
        class InitResult2B(
            val jsonParseError: MetricsError.JsonParseError,
        ) : SdkInitializationResult(
                success = true,
                initResultCode = "success_with_cached_config_and_error",
            ) {
            val error: Error = Error("Invalid app config JSON")

            override fun getMetricsError(): MetricsError? = jsonParseError
        }

        // No cached config
        // Response JSON parsed: init success but SDK disabled, /event body JSON has cached metrics array, no error
        object InitResult1C : SdkInitializationResult(
            success = true,
            initResultCode = "disabled_by_fetched_config ",
        )

        // Has cached config
        // Response JSON parsed: init success but SDK disabled, /event body JSON has cached metrics array, no error
        object InitResult2C : SdkInitializationResult(
            success = true,
            initResultCode = "disabled_by_cached_config ",
        )
    }

    sealed class AdLoadResult : EventResult {
        object AdLoadSuccess : AdLoadResult()

        class AdLoadJsonFailure(
            val jsonParseError: MetricsError.JsonParseError,
        ) : AdLoadResult() {
            override fun getMetricsError(): MetricsError? = jsonParseError
        }

        class AdLoadPartnerFailure(
            val metricsError: MetricsError.SimpleError,
        ) : AdLoadResult() {
            override fun getMetricsError(): MetricsError? = metricsError
        }

        class AdLoadUnspecifiedFailure(
            val metricsError: MetricsError.SimpleError,
        ) : AdLoadResult() {
            override fun getMetricsError(): MetricsError? = metricsError
        }
    }
}
