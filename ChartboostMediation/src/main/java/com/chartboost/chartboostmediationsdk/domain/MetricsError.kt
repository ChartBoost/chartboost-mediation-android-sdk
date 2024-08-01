/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import androidx.annotation.VisibleForTesting
import com.chartboost.chartboostmediationsdk.utils.getMaxJsonPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * @suppress
 */
@Serializable
sealed class MetricsError {
    @Serializable
    class SimpleError private constructor(
        @SerialName("cm_code")
        val cmCode: String,
        @SerialName("details")
        val details: JsonObject,
    ) : MetricsError() {
        constructor(
            chartboostMediationError: ChartboostMediationError,
        ) : this(
            cmCode = chartboostMediationError.code,
            details =
                buildJsonObject {
                    put("type", chartboostMediationError.name)
                    put("description", chartboostMediationError.message)
                },
        )
    }

    @Serializable
    class JsonParseError private constructor(
        @SerialName("cm_code")
        val cmCode: String,
        @SerialName("details")
        val details: JsonObject,
    ) : MetricsError() {
        constructor(
            chartboostMediationError: ChartboostMediationError,
            exception: Exception,
            exceptionMessage: String,
            malformedJson: String,
        ) : this(
            cmCode = chartboostMediationError.code,
            details =
                buildJsonObject {
                    put("type", exception::class.toString())
                    put("description", exceptionMessage)
                    put("data_as_string", getMaxJsonPayload(malformedJson, MAX_JSON_SIZE))
                },
        )

        companion object {
            // 3.5 MB
            @VisibleForTesting
            const val MAX_JSON_SIZE = (3.5 * 1024 * 1024).toInt()
        }
    }
}
