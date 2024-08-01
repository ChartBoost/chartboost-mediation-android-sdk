/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import android.util.Base64
import com.chartboost.chartboostmediationsdk.domain.MetricsError
import com.chartboost.chartboostmediationsdk.domain.errorSerializersModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * By using Chartboost Mediation instead of Json where ever serialization and deserialization is
 * happening, we're able to guarantee that we're always using the same configuration
 */
@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal val ChartboostMediationJson =
    Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        serializersModule =
            SerializersModule {
                polymorphic(
                    MetricsError::class,
                    MetricsError.SimpleError::class,
                    MetricsError.SimpleError.serializer(),
                )
                polymorphic(
                    MetricsError::class,
                    MetricsError.JsonParseError::class,
                    MetricsError.JsonParseError.serializer(),
                )
                include(errorSerializersModule)
            }
    }

/**
 * @suppress
 */
fun getMaxJsonPayload(
    jsonString: String,
    maxSize: Int,
): String {
    val jsonBase64Encoded = Base64.encode(jsonString.toByteArray(), Base64.NO_WRAP)

    return if (jsonBase64Encoded.size > maxSize) {
        Base64
            .encode(
                "The malformed JSON is too large to include. Partial JSON: $jsonString".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            ).copyOfRange(0, maxSize - maxSize % 4)
            .toString(Charsets.UTF_8)
    } else {
        Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
    }
}
